package com.sanjivanai.app.core

// Alert + escalation generation, ported from the deterministic logic in
// prototype/server.js. DANGER alerts (vitals or camera fall) trigger the
// automatic emergency-call chain: family -> backup -> 108/112.
object AlertEngine {
    data class Alert(
        val id: String, val patientId: String, val patientName: String,
        val metric: String, val status: String, val message: String,
        val timestamp: Long, val source: String
    )

    data class CallStep(
        val label: String, val number: String, val status: String // pending|dialing|answered|unanswered
    )

    data class EmergencyCall(
        val id: String, val alertId: String, val patientId: String,
        val steps: List<CallStep>, val startedAt: Long
    )

    fun deriveAlerts(nowMs: Long = System.currentTimeMillis(), span: Int = 8): List<Alert> {
        // Danger episodes produce alerts. We scan recent 200s slots per patient.
        val out = mutableListOf<Alert>()
        for (p in DemoData.PATIENTS) {
            for (k in span downTo 0) {
                val slot = ((nowMs / VitalSimulator.SLOT_MS).toInt()) - k
                val rh = Hash.hash01("${p.id}:ep:$slot")
                if (rh < 0.5) {
                    val types = (VitalSimulator.EPISODES[p.condition] ?: VitalSimulator.EPISODES["healthy"]!!).keys.toList()
                    val idx = minOf(types.size - 1, ((rh / 0.5) * types.size).toInt())
                    val type = types[idx]
                    val t = slot.toLong() * VitalSimulator.SLOT_MS
                    val label = when (type) {
                        "hypertensive crisis" -> "Blood Pressure"
                        "arrhythmia episode", "rapid HR", "tachycardia spike", "bradycardia" -> "HR"
                        "fever spike" -> "Temp"
                        "hyperglycaemia", "hypoglycaemia" -> "Glucose"
                        else -> "SpO2"
                    }
                    out.add(Alert(
                        "ALT-${p.id}-$slot", p.id, p.name, label, "danger",
                        "${p.name}: $type detected (DANGER level).", t, "vitals"
                    ))
                }
            }
        }
        // Camera fall events become danger alerts too
        for (e in CameraZoneEngine.deriveCameraEvents(nowMs, 8)) {
            if (e.severity == "danger") {
                val pn = DemoData.PATIENTS.firstOrNull { it.id == e.patientId }?.name ?: ""
                out.add(Alert("ALT-${e.patientId}-${e.at}", e.patientId, pn, "Camera", "danger",
                    e.message, e.at, "camera"))
            }
        }
        return out.sortedByDescending { it.timestamp }
    }

    fun buildCallChain(alert: Alert, nowMs: Long): EmergencyCall {
        val c = DemoData.CONTACTS[alert.patientId] ?: Contact("Family", "Backup")
        val callStart = alert.timestamp

        // Range within which a step is DIALING (waiting) then resolves to answered/unanswered.
        fun stepState(dialFrom: Long, resolveAt: Long, answerOdds: Double): String {
            return when {
                nowMs - callStart < dialFrom -> "pending"
                nowMs - callStart < resolveAt -> "dialing"
                Hash.hash01("${alert.id}:ans:$callStart") < answerOdds -> "answered"
                else -> "unanswered"
            }
        }

        val family = stepState(4200, 5000, 0.55)
        val familyAnswered = family == "answered"
        val backup = if (familyAnswered) "skipped" else stepState(5500, 9800, 0.45)
        val backupAnswered = backup == "answered"
        val emergency = if (familyAnswered || backupAnswered) "skipped" else stepState(11000, 13200, 0.90)

        return EmergencyCall(
            "CAL-${alert.id.replace("ALT-", "")}", alert.id, alert.patientId,
            listOf(
                CallStep("Family caregiver", c.family, family),
                CallStep("Backup contact", c.backup, backup),
                CallStep("Emergency services (108/112)", "+91 108", emergency)
            ),
            callStart
        )
    }
}
