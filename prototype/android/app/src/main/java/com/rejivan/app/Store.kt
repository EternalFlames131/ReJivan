package com.rejivan.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device persistence (SharedPreferences — plain local storage, no server
 * needed). Holds: login session, medications (+ taken log), and the pending
 * two-way-sync queue that flushes to the server when online.
 */
class Store(private val ctx: Context) {

    private val prefs: SharedPreferences = ctx.getSharedPreferences("rejivan_store", Context.MODE_PRIVATE)

    // ---------- session ----------
    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(v) = prefs.edit().putString("token", v).apply()
    var email: String
        get() = prefs.getString("email", "") ?: ""
        set(v) = prefs.edit().putString("email", v).apply()
    var userName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(v) = prefs.edit().putString("user_name", v).apply()
    var userRole: String
        get() = prefs.getString("user_role", "family") ?: "family"
        set(v) = prefs.edit().putString("user_role", v).apply()

    fun clearSession() {
        prefs.edit().remove("token").remove("email").remove("user_name").remove("user_role").apply()
    }

    fun isLoggedIn(): Boolean = token.isNotEmpty() && email.isNotEmpty()

    // ---------- medications ----------
    fun medications(): List<Med> {
        val raw = prefs.getString("meds", "") ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i -> medFromJson(arr.getJSONObject(i)) }
        } catch (_: Exception) { emptyList() }
    }

    fun replaceMedications(list: List<Med>) {
        val arr = JSONArray()
        for (m in list) arr.put(medToJson(m))
        prefs.edit().putString("meds", arr.toString()).apply()
    }

    fun storeMedication(m: Med) {
        val list = medications().toMutableList()
        val i = list.indexOfFirst { it.id == m.id }
        if (i >= 0) list[i] = m else list.add(m)
        replaceMedications(list)
    }

    fun removeMedication(id: String) {
        replaceMedications(medications().filter { it.id != id })
    }

    fun seedDefaultMeds() {
        if (prefs.contains("meds_seeded")) return
        val list = listOf(
            Med("MED001", "P1", "Amlodipine", "5 mg", "daily", listOf("08:00", "20:00"), "After food"),
            Med("MED002", "P2", "Metformin", "500 mg", "twice daily", listOf("09:00", "21:00"), "With meals"),
            Med("MED003", "P3", "Paracetamol", "650 mg", "8 hourly", listOf("08:00", "16:00", "00:00"), "For fever"),
        )
        replaceMedications(list)
        prefs.edit().putBoolean("meds_seeded", true).apply()
    }

    // ---------- taken log ----------
    fun lastTaken(medId: String): String? {
        val raw = prefs.getString("taken", "") ?: return null
        return try {
            val arr = JSONArray(raw)
            var last: String? = null
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optString("medId") == medId) {
                    val t = o.optString("time", "")
                    if (last == null || t > last) last = t
                }
            }
            last
        } catch (_: Exception) { null }
    }

    fun markTaken(medId: String) {
        val raw = prefs.getString("taken", "") ?: ""
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        arr.put(JSONObject().put("medId", medId).put("time", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(java.util.Date())))
        prefs.edit().putString("taken", arr.toString()).apply()
        enqueueTake(medId)
    }

    // ---------- pending two-way-sync queue ----------
    fun pendingTakes(): List<String> = prefs.getString("pending_takes", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    fun enqueueTake(medId: String) {
        val set = pendingTakes().toMutableSet()
        set.add(medId)
        prefs.edit().putString("pending_takes", set.joinToString(",")).apply()
    }

    fun clearPendingTake(medId: String) {
        val set = pendingTakes().toMutableSet()
        set.remove(medId)
        prefs.edit().putString("pending_takes", set.joinToString(",")).apply()
    }

    fun clearAllPending() {
        prefs.edit().remove("pending_takes").apply()
    }

    // ---------- json helpers ----------
    private fun medToJson(m: Med): JSONObject {
        val times = JSONArray()
        for (t in m.times) times.put(t)
        return JSONObject()
            .put("id", m.id).put("patientId", m.patientId).put("name", m.name)
            .put("dose", m.dose).put("frequency", m.frequency).put("times", times)
            .put("notes", m.notes).put("active", m.active)
    }

    private fun medFromJson(o: JSONObject): Med {
        val timesArr = o.optJSONArray("times") ?: JSONArray()
        val times = (0 until timesArr.length()).map { timesArr.getString(it) }
        return Med(
            id = o.optString("id"), patientId = o.optString("patientId"), name = o.optString("name"),
            dose = o.optString("dose"), frequency = o.optString("frequency", "daily"),
            times = times, notes = o.optString("notes"), active = o.optBoolean("active", true),
        )
    }
}