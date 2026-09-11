package com.rejivan.app.core

// Mirrors prototype/data model. Everything below is embedded in the app so it
// works fully OFFLINE (no server, no network). Simulation honesty is preserved:
// vitals/camera events are SIMULATED; rules/alerts/escalation are REAL logic.

data class Patient(
    val id: String,
    val name: String,
    val age: Int,
    val sex: String,
    val condition: String,
    val location: String,
    val address: String,
    val ward: String? = null,
    val userId: String
)

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String
)

data class Medication(
    val id: String,
    val patientId: String,
    val name: String,
    val dose: String,
    val frequency: String,
    val times: List<String>,
    val notes: String,
    val active: Boolean = true
)

data class CameraZone(
    val id: String,
    val patientId: String,
    val name: String,
    val room: String,
    val ward: Boolean = false
)

data class Contact(
    val family: String,
    val backup: String
)

object DemoData {
    val USERS = listOf(
        User("Ua7ac85b2", "Sharma Family", "asharma@demo.in", "family"),
        User("U0b480e2b", "Prakash Family", "rprakash@demo.in", "family"),
        User("U81699d70", "Ward Nurse Station", "wardnurse@demo.in", "ward")
    )

    val PATIENTS = listOf(
        Patient("P1", "Anita Sharma", 67, "F", "hypertension",
            "Home - Living Room", "Junglighat, Port Blair", userId = "Ua7ac85b2"),
        Patient("P2", "Ram Prakash", 74, "M", "diabetes",
            "Home - Bedroom", "Hut Bay, Little Andaman (served via PHC)", userId = "U0b480e2b"),
        Patient("P3", "Meera Nair", 58, "F", "post-surgery",
            "Virtual Ward", "GB Pant Hospital, Port Blair", ward = "Ward A - Bed 1", userId = "U81699d70"),
        Patient("P4", "Kavitha Rao", 61, "F", "heart-arrhythmia",
            "Virtual Ward", "GB Pant Hospital, Port Blair", ward = "Ward A - Bed 2", userId = "U81699d70")
    )

    val CAMERA_ZONES = listOf(
        CameraZone("CAM1", "P1", "Home - Living Room", "Home — Junglighat, Port Blair"),
        CameraZone("CAM2", "P2", "Home - Bedroom", "Home — Hut Bay, Little Andaman"),
        CameraZone("BED1", "P3", "GB Pant Virtual Ward - Bed 1", "GB Pant Hospital, Port Blair", ward = true),
        CameraZone("BED2", "P4", "GB Pant Virtual Ward - Bed 2", "GB Pant Hospital, Port Blair", ward = true)
    )

    val BASE_MEDICATIONS = listOf(
        Medication("MED001", "P1", "Amlodipine", "5 mg", "daily", listOf("08:00", "20:00"), "After food"),
        Medication("MED002", "P2", "Metformin", "500 mg", "twice daily", listOf("09:00", "21:00"), "With meals"),
        Medication("MED003", "P3", "Paracetamol", "650 mg", "8 hourly", listOf("08:00", "16:00", "00:00"), "For fever")
    )

    val CONTACTS = mapOf(
        "P1" to Contact("+91 98300 11001", "+91 98300 11002"),
        "P2" to Contact("+91 98300 12001", "+91 98300 12002"),
        "P3" to Contact("Ward A nurse — +91 98300 13001", "Duty doctor — +91 98300 13002"),
        "P4" to Contact("Ward A nurse — +91 98300 14001", "Duty doctor — +91 98300 14002")
    )

    val REGION = "Andaman & Nicobar Islands (UT)"

    fun findUserByEmail(email: String): User? = USERS.firstOrNull { it.email.equals(email, true) }

    fun patientsForUser(userId: String): List<Patient> = PATIENTS.filter { it.userId == userId }

    fun zonesForUser(userId: String): List<CameraZone> {
        val patientIds = patientsForUser(userId).map { it.id }
        return CAMERA_ZONES.filter { it.patientId in patientIds }
    }

    // Local device catalogue (for offline fallback — mirrors server medical-devices.js)
    data class LocalDevice(
        val id: String, val name: String, val manufacturer: String,
        val approval: String, val price: String, val madeInIndia: Boolean,
        val measures: List<String>, val description: String
    )

    val LOCAL_DEVICE_CATALOGUE = listOf(
        LocalDevice("SANKETLIFE_12", "SanketLife 12-Lead ECG", "Agatsa (Pune, India)", "CDSCO Class B, CE certified", "5000", true, listOf("hr"), "World's smallest 12-lead ECG device. Pocket-sized, Bluetooth-connected."),
        LocalDevice("HEXOSKIN_MEDICAL", "Hexoskin Medical System", "Hexoskin (Canada)", "FDA 510(k) cleared", "45000", false, listOf("hr"), "Smart vest: 3-lead ECG, heart rate, respiration rate."),
        LocalDevice("CHOICEMMED_MD300", "ChoiceMMed MD300C228", "Beijing Choice", "FDA 510(k) cleared", "4000", false, listOf("spo2"), "Fingertip medical pulse oximeter with Bluetooth."),
        LocalDevice("LEPU_AP10", "Lepu AP-10 Wrist Oximeter", "Lepu Medical", "FDA cleared, CE certified", "10000", false, listOf("spo2"), "Wrist-worn continuous oximeter. 160-hour data storage."),
        LocalDevice("OMRON_BP", "Omron HEM-7156T", "Omron (Japan)", "FDA, CE, CDSCO", "4500", false, listOf("sbp", "dbp"), "Medical-grade automatic blood pressure monitor with Bluetooth."),
        LocalDevice("BIOBEAT_CHEST", "Biobeat BB-613 Chest Patch", "Biobeat (Israel)", "FDA 510(k) cleared, CE", "25000", false, listOf("hr", "sbp", "dbp", "spo2", "temp"), "Gold-standard chest patch: 13 vitals from one wearable."),
        LocalDevice("TEMPTRAQ_PATCH", "TempTraq Continuous Temp Patch", "Blue Spark Technologies (USA)", "FDA Class II cleared", "2000", false, listOf("temp"), "Disposable axillary temperature patch. 72-hour continuous monitoring."),
        LocalDevice("AION_TEMPSHIELD", "AION TempShield", "AION Biosystems (USA)", "FDA 510(k) cleared", "15000", false, listOf("temp"), "90-day continuous skin temperature sensor."),
        LocalDevice("FREESTYLE_LIBRE3", "FreeStyle Libre 3", "Abbott", "FDA cleared, CDSCO approved", "4670", false, listOf("glucose"), "Real-time continuous glucose monitor. 14-day wear."),
        LocalDevice("GLUCORX_VIXXA2", "GlucoRx Vixxa 2", "MicroTech Medical / GlucoRx India", "CE, CDSCO Class B certified", "3200", false, listOf("glucose"), "Cheapest CDSCO-certified CGM in India. 15-day wear."),
        LocalDevice("H360_HEALTH360", "H360 Health360", "Medilogy Inc (India)", "CDSCO (Made in India)", "7000", true, listOf("hr", "spo2"), "World's smallest multi-parameter device. IIT-designed.")
    )

    private val LOCAL_DEVICE_IDS = mapOf(
        "P1" to listOf("SANKETLIFE_12", "OMRON_BP", "TEMPTRAQ_PATCH"),
        "P2" to listOf("FREESTYLE_LIBRE3", "CHOICEMMED_MD300", "OMRON_BP"),
        "P3" to listOf("BIOBEAT_CHEST", "FREESTYLE_LIBRE3"),
        "P4" to listOf("BIOBEAT_CHEST", "SANKETLIFE_12", "TEMPTRAQ_PATCH")
    )

    fun devicesForPatient(patientId: String): List<LocalDevice> =
        LOCAL_DEVICE_IDS[patientId].orEmpty().mapNotNull { id -> LOCAL_DEVICE_CATALOGUE.find { it.id == id } }
}
