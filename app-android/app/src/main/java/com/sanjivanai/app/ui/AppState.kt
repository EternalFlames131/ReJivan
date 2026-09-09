package com.sanjivanai.app.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sanjivanai.app.core.*
import com.sanjivanai.app.data.MedStore

// Holds app state, computes vitals/alerts/camera deterministically from the
// wall clock, and refreshes on a UI tick. No network dependency.
class AppState(private val ctx: Context) {
    var currentUser by mutableStateOf<User?>(null)
        private set
    var tick by mutableStateOf(0L)

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            tick = System.currentTimeMillis()
            handler.postDelayed(this, 2000)
        }
    }

    fun startClock() { handler.postDelayed(refreshRunnable, 2000) }
    fun stopClock() { handler.removeCallbacks(refreshRunnable) }

    val patients: List<Patient> get() = DemoData.patientsForUser(currentUser?.id ?: "")
    val zones: List<CameraZone> get() = DemoData.zonesForUser(currentUser?.id ?: "")

    fun login(email: String, password: String): String? {
        val u = DemoData.findUserByEmail(email) ?: return "Unknown account"
        if (password != "demo123") return "Incorrect password (demo password: demo123)"
        currentUser = u
        return null
    }

    fun logout() { currentUser = null }

    fun vitalsOf(p: Patient): VitalsResult = VitalSimulator.generateVitals(p.id, p.condition)
    fun reportOf(p: Patient): Map<String, Any> =
        RulesEngine.report(p.id, p.name, vitalsOf(p).vitals)

    fun alerts(): List<AlertEngine.Alert> =
        AlertEngine.deriveAlerts(System.currentTimeMillis()).filter {
            DemoData.patientsForUser(currentUser?.id ?: "").any { p -> p.id == it.patientId }
        }

    fun callFor(alert: AlertEngine.Alert): AlertEngine.EmergencyCall =
        AlertEngine.buildCallChain(alert, System.currentTimeMillis())

    fun meds(): List<Medication> = MedStore.load(ctx).filter {
        it.patientId in patients.map { p -> p.id }
    }

    fun addMed(med: Medication) = MedStore.add(ctx, med)
    fun removeMed(id: String) = MedStore.remove(ctx, id)
}
