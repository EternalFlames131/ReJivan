package com.rejivan.app.data

import android.content.Context
import com.rejivan.app.core.*
import com.rejivan.app.network.Sync
import kotlin.concurrent.thread

/**
 * Data layer: server-first, local-engine fallback.
 *
 * When online, fetches live data from https://rejivan.vercel.app and
 * merges it with the local state. When offline (or server unreachable),
 * falls back to the fully offline local engine (VitalSimulator + RulesEngine).
 *
 * Two-way sync: medication "take" actions push to the server.
 */
object Repository {
    enum class Source { SERVER, LOCAL }
    data class ServerState(
        val source: Source,
        val patients: List<Patient>,
        val vitals: Map<String, Sync.ServerPatient>,
        val alerts: List<Sync.ServerAlert>,
        val escalations: List<Sync.ServerEscalation>,
        val calls: List<Sync.ServerCall>,
        val zones: List<CameraZone>,
        val meds: List<Medication>,
        val serverUser: Sync.LoginResult?
    )

    private var token: String? = null
    private var cachedUser: Sync.LoginResult? = null

    fun setToken(t: String?) { token = t }
    fun getToken(): String? = token

    /**
     * Login: try server first, return null + set token if successful.
     * Returns error string on failure.
     */
    fun login(email: String, password: String): String? {
        // Try server
        val serverResult = try { Sync.login(email, password) } catch (e: Exception) { null }
        if (serverResult != null) {
            token = serverResult.token
            cachedUser = serverResult
            return null
        }
        // Fallback: local demo login
        val u = DemoData.findUserByEmail(email) ?: return "Unknown account"
        if (password != "demo123") return "Incorrect password (demo: demo123)"
        token = null
        cachedUser = null
        return null
    }

    /**
     * Fetch all data — runs on background thread, calls back on main thread.
     * Returns ServerState with source = SERVER or LOCAL.
     */
    fun fetchAll(userId: String, ctx: Context, callback: (ServerState) -> Unit) {
        thread {
            val result = fetchFromServer(userId, ctx)
            android.os.Handler(android.os.Looper.getMainLooper()).post { callback(result) }
        }
    }

    private fun fetchFromServer(userId: String, ctx: Context): ServerState {
        val t = token
        if (t != null) {
            // Server path: fetch all endpoints
            val vitals = try { Sync.fetchVitals(t!!) } catch (e: Exception) { null }
            val alertsPair = try { Sync.fetchAlerts(t!!) } catch (e: Exception) { null }
            val calls = try { Sync.fetchCalls(t!!) } catch (e: Exception) { null }
            val zones = try { Sync.fetchCameraZones(t!!) } catch (e: Exception) { null }
            val meds = try { Sync.fetchMedications(t!!) } catch (e: Exception) { null }

            if (vitals != null && alertsPair != null) {
                // Map server patients to local Patient model
                val patients = vitals.map { sp ->
                    Patient(sp.id, sp.name, sp.age, sp.sex, sp.condition,
                        sp.location, sp.addr ?: "", sp.ward, userId)
                }
                // Map server meds to local Medication model
                val medList = meds?.map { sm ->
                    Medication(sm.id, sm.patientId, sm.name, sm.dose, sm.frequency,
                        sm.times, sm.notes, sm.active)
                } ?: emptyList()
                // Map server zones to local CameraZone model
                val zoneList = zones?.map { sz ->
                    CameraZone(sz.id, sz.patientId, sz.name, "", sz.ward)
                } ?: emptyList()
                // Map server vitals to map
                val vitalsMap = vitals.associateBy { it.id }
                return ServerState(
                    Source.SERVER, patients, vitalsMap,
                    alertsPair.first, alertsPair.second,
                    calls ?: emptyList(), zoneList, medList, cachedUser
                )
            }
        }

        // Local fallback: everything computed on-device
        val patients = DemoData.patientsForUser(userId)
        val zones = DemoData.zonesForUser(userId)
        val meds = MedStore.load(ctx).filter { it.patientId in patients.map { p -> p.id } }
        return ServerState(Source.LOCAL, patients, emptyMap(),
            emptyList(), emptyList(), emptyList(), zones, meds, null)
    }

    /**
     * Mark a medication as taken — push to server if online, always update local.
     */
    fun markTaken(ctx: Context, medId: String) {
        MedStore.update(ctx, MedStore.load(ctx).map {
            if (it.id == medId) it.copy(active = false) else it
        })
        val t = token
        if (t != null) {
            thread { try { Sync.postMedTake(t!!, medId) } catch (_: Exception) {} }
        }
    }
}
