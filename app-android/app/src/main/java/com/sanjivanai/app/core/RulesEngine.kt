package com.sanjivanai.app.core

// Ported 1:1 from prototype/rules.js — REAL clinical-style thresholds (REAL logic).
object RulesEngine {
    private data class Rule(val lo: Double, val hi: Double, val dangerLo: Double? = null,
                            val dangerHi: Double? = null, val cautionLo: Double? = null,
                            val cautionHi: Double? = null, val spo2Danger: Double? = null)

    private val RULES = mapOf(
        "hr" to Rule(60.0, 100.0, cautionLo = 50.0, cautionHi = 120.0),
        "spo2" to Rule(95.0, 200.0, spo2Danger = 90.0),
        "sbp" to Rule(90.0, 140.0, cautionHi = 160.0),
        "dbp" to Rule(60.0, 90.0, cautionHi = 100.0),
        "temp" to Rule(36.0, 37.5, cautionHi = 38.5),
        "glucose" to Rule(70.0, 140.0, cautionLo = 70.0, cautionHi = 180.0)
    )

    fun statusFor(metric: String, value: Double): String {
        val r = RULES[metric] ?: return "normal"
        if (metric == "spo2") {
            return when {
                value < r.spo2Danger!! -> "danger"
                value < r.lo -> "caution"
                else -> "normal"
            }
        }
        val low = value < r.lo
        val high = value > r.hi
        if (low) {
            return if (r.cautionLo != null && value >= r.cautionLo) "caution" else "danger"
        }
        if (high) {
            return if (r.cautionHi != null && value <= r.cautionHi) "caution" else "danger"
        }
        return "normal"
    }

    fun bloodPressureStatus(sbp: Double, dbp: Double): String {
        val s = statusFor("sbp", sbp)
        val d = statusFor("dbp", dbp)
        return when {
            s == "danger" || d == "danger" -> "danger"
            s == "caution" || d == "caution" -> "caution"
            else -> "normal"
        }
    }

    fun report(patientId: String, patientName: String, v: VitalSet): Map<String, Any> = mapOf(
        "patientId" to patientId,
        "patientName" to patientName,
        "hr" to mapOf("value" to v.hr, "status" to statusFor("hr", v.hr.toDouble())),
        "spo2" to mapOf("value" to v.spo2, "status" to statusFor("spo2", v.spo2.toDouble())),
        "sbp" to mapOf("value" to v.sbp, "status" to statusFor("sbp", v.sbp.toDouble())),
        "dbp" to mapOf("value" to v.dbp, "status" to statusFor("dbp", v.dbp.toDouble())),
        "bp" to bloodPressureStatus(v.sbp.toDouble(), v.dbp.toDouble()),
        "temp" to mapOf("value" to v.temp, "status" to statusFor("temp", v.temp)),
        "glucose" to mapOf("value" to v.glucose, "status" to statusFor("glucose", v.glucose.toDouble()))
    )

    fun dangerLabels(report: Map<String, Any>): List<String> {
        val labels = mutableListOf<String>()
        for (m in listOf("hr", "spo2", "bp", "temp", "glucose")) {
            val s = report[m]
            val status = if (s is Map<*, *>) {
                val v = s["status"]
                if (v is String) v else "normal"
            } else s
            if (status == "danger") {
                labels.add(if (m == "bp") "Blood Pressure" else m.uppercase())
            }
        }
        return labels
    }
}
