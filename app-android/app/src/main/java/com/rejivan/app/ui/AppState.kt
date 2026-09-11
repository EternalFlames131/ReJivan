package com.rejivan.app.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rejivan.app.core.*
import com.rejivan.app.data.MedStore
import com.rejivan.app.data.Repository
import com.rejivan.app.network.Sync

class AppState(private val ctx: Context) {
    var currentUser by mutableStateOf<User?>(null)
        private set
    var tick by mutableStateOf(0L)
    var dataSource by mutableStateOf(Repository.Source.LOCAL)
        private set
    var serverPatients by mutableStateOf<List<Patient>>(emptyList())
        private set
    var serverVitals by mutableStateOf<Map<String, Sync.ServerPatient>>(emptyMap())
        private set
    var serverAlerts by mutableStateOf<List<Sync.ServerAlert>>(emptyList())
        private set
    var serverEscalations by mutableStateOf<List<Sync.ServerEscalation>>(emptyList())
        private set
    var serverCalls by mutableStateOf<List<Sync.ServerCall>>(emptyList())
        private set
    var serverMeds by mutableStateOf<List<Medication>>(emptyList())
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var serverUser by mutableStateOf<Sync.LoginResult?>(null)
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            tick = System.currentTimeMillis()
            handler.postDelayed(this, 2000)
            // Poll server every 5 seconds for fresh data
            if (currentUser != null && dataSource == Repository.Source.SERVER) {
                refreshFromServer()
            }
        }
    }

    fun startClock() { handler.postDelayed(refreshRunnable, 2000) }
    fun stopClock() { handler.removeCallbacks(refreshRunnable) }

    val patients: List<Patient>
        get() = if (serverPatients.isNotEmpty()) serverPatients
                else DemoData.patientsForUser(currentUser?.id ?: "")

    val zones: List<CameraZone>
        get() = if (dataSource == Repository.Source.SERVER && serverPatients.isNotEmpty()) {
            DemoData.zonesForUser(currentUser?.id ?: "")
        } else DemoData.zonesForUser(currentUser?.id ?: "")

    fun login(email: String, password: String): String? {
        val err = Repository.login(email, password.trim())
        if (err != null) return err
        // Determine user — use server account if this is a website-registered
        // account, otherwise fall back to the local demo user.
        val u = Repository.cachedUser?.let { cu ->
            User(cu.userId, cu.name, cu.email, cu.role)
        } ?: DemoData.findUserByEmail(email)
        if (u == null) return "Unknown account"
        currentUser = u
        // Immediately fetch from server in background
        refreshFromServer()
        return null
    }

    fun logout() {
        currentUser = null
        Repository.setToken(null)
        dataSource = Repository.Source.LOCAL
        serverPatients = emptyList()
        serverVitals = emptyMap()
        serverAlerts = emptyList()
        serverEscalations = emptyList()
        serverCalls = emptyList()
        serverMeds = emptyList()
        serverUser = null
    }

    private fun refreshFromServer() {
        val uid = currentUser?.id ?: return
        if (isRefreshing) return
        isRefreshing = true
        Repository.fetchAll(uid, ctx) { state ->
            dataSource = state.source
            serverPatients = state.patients
            serverVitals = state.vitals
            serverAlerts = state.alerts
            serverEscalations = state.escalations
            serverCalls = state.calls
            serverMeds = state.meds
            serverUser = state.serverUser
            isRefreshing = false
        }
    }

    fun vitalsOf(p: Patient): VitalsResult = VitalSimulator.generateVitals(p.id, p.condition)

    fun reportOf(p: Patient): Map<String, Any> {
        // If server data available, use server report
        val sp = serverVitals[p.id]
        if (sp != null && sp.report != null) {
            return parseServerReport(sp)
        }
        // Local fallback
        return RulesEngine.report(p.id, p.name, vitalsOf(p).vitals)
    }

    private fun parseServerReport(sp: Sync.ServerPatient): Map<String, Any> {
        val report = sp.report ?: return emptyMap()
        val out = mutableMapOf<String, Any>()
        out["patientId"] = sp.id
        out["patientName"] = sp.name
        for (key in listOf("hr", "spo2", "sbp", "dbp", "temp", "glucose", "bp")) {
            val v = report.opt(key)
            if (v != null) out[key] = v
        }
        return out
    }

    fun alerts(): List<AlertEngine.Alert> {
        // If server alerts available, map them
        if (serverAlerts.isNotEmpty()) {
            return serverAlerts.map { sa ->
                AlertEngine.Alert(
                    sa.id, sa.patientId, sa.patientName,
                    sa.type, sa.severity, sa.message,
                    sa.createdAt, sa.type
                )
            }
        }
        // Local fallback
        return AlertEngine.deriveAlerts(System.currentTimeMillis()).filter {
            DemoData.patientsForUser(currentUser?.id ?: "").any { p -> p.id == it.patientId }
        }
    }

    fun callFor(alert: AlertEngine.Alert): AlertEngine.EmergencyCall {
        // If server calls available, find matching call
        val sc = serverCalls.firstOrNull { it.alertId == alert.id }
        if (sc != null) {
            return AlertEngine.EmergencyCall(
                sc.id, sc.alertId, sc.patientId,
                sc.steps.map { step ->
                    AlertEngine.CallStep(step.label, step.to, step.state)
                },
                sc.startedAt
            )
        }
        // Local fallback
        return AlertEngine.buildCallChain(alert, System.currentTimeMillis())
    }

    fun meds(): List<Medication> {
        // When online, show the live list straight from the website.
        if (dataSource == Repository.Source.SERVER && serverMeds.isNotEmpty()) {
            return serverMeds.filter { it.patientId in patients.map { p -> p.id } }
        }
        return MedStore.load(ctx).filter {
            it.patientId in patients.map { p -> p.id }
        }
    }

    fun addMed(med: Medication) = Repository.addMed(ctx, med)
    fun removeMed(id: String) = Repository.removeMed(ctx, id)
}
