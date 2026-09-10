package com.rejivan.app

/** Immutable demo/spec models shared by the offline engine and the server view. */
data class Patient(
    val id: String,
    val name: String,
    val age: Int,
    val sex: String,
    val condition: String,
    val location: String,
    val address: String? = null,
    val ward: String? = null,
)

data class Vitals(
    val hr: Int? = null,
    val spo2: Double? = null,
    val sbp: Int? = null,
    val dbp: Int? = null,
    val temp: Double? = null,
    val glucose: Int? = null,
)

data class Report(
    val vitals: Vitals,
    val metricStatus: Map<String, String>,   // hr/spo2/sbp/dbp/temp/glucose -> normal|caution|danger
    val bpStatus: String,
)

fun String.cap(): String = this.replaceFirstChar { it.uppercaseChar() }

data class Med(
    val id: String,
    val patientId: String,
    val name: String,
    val dose: String,
    val frequency: String = "daily",
    val times: List<String> = listOf("08:00", "20:00"),
    val notes: String = "",
    val active: Boolean = true,
)

data class Alert(
    val id: String,
    val patientId: String,
    val patientName: String,
    val age: Int? = null,
    val ward: String? = null,
    val location: String,
    val type: String,            // vitals | camera
    val severity: String,        // danger
    val message: String,
    val confirmed: Boolean = false,
    val suspect: List<String> = emptyList(),
    val createdAt: Long,
)

data class Escalation(
    val id: String,
    val alertId: String,
    val patientId: String,
    val patientName: String,
    val severity: String,
    val channels: List<String>,
    val to: String,
    val dispatchedAt: Long,
    val delivered: Boolean,
    val simulationNotice: String,
)

data class CallLadder(
    val label: String,   // family | backup | emergency
    val to: String,
    val state: String,   // pending | dialing | answered | unanswered
    val emergency: Boolean,
)

data class CallChain(
    val id: String,
    val alertId: String,
    val patientId: String,
    val patientName: String,
    val status: String,          // dialing | answered | escalating | dispatched | complete
    val ladder: List<CallLadder>,
    val log: List<Pair<Long, String>>,
)

data class CameraZone(
    val id: String,
    val patientId: String,
    val name: String,
    val room: String,
    val ward: Boolean = false,
)

data class CameraEvent(
    val id: String,
    val zoneId: String,
    val patientId: String,
    val zoneName: String,
    val kind: String,
    val severity: String,
    val message: String,
    val at: Long,
)

data class Frame(
    val ts: Long,
    val motion: Double,
    val person: Boolean,
    val lighting: String,
)

data class Device(
    val id: String? = null,
    val name: String,
    val manufacturer: String? = null,
    val category: String? = null,
    val approval: String? = null,
    val connectivity: String? = null,
    val accuracy: String? = null,
    val measures: List<String>,
    val priceINR: String? = null,
    val madeInIndia: Boolean = false,
    val productionTier: String = "medical",
    val connected: Boolean = false,
    val battery: Int? = null,
    val signalStrength: Int? = null,
)

data class DemoUser(
    val email: String,
    val name: String,
    val role: String,
    val patientIds: List<String>,
)

data class PatientSnapshot(
    val patient: Patient,
    val vitals: Vitals,
    val rawVitals: Vitals,
    val episode: Episode?,
    val reliability: ReliabilityInfo,
    val deviceTier: String,
    val devices: List<Device>,
    val report: Report,
)

data class Episode(val type: String, val from: Long, val until: Long)

data class ReliabilityInfo(
    val confidence: Int,
    val perMetricConfidence: Map<String, Int>,
    val validationPassed: Boolean,
    val rejectedMetrics: List<String>,
    val degradationMissing: List<String>,
    val degradationSufficient: Boolean,
)

data class AppSnapshot(
    val source: Source,
    val patients: List<PatientSnapshot>,
    val alerts: List<Alert>,
    val escalations: List<Escalation>,
    val calls: List<CallChain>,
    val zones: List<CameraZone>,
    val cameraEvents: List<CameraEvent>,
    val meds: List<Med>,
)

enum class Source { ONLINE, OFFLINE }