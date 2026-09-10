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
}
