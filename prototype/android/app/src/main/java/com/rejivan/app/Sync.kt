package com.rejivan.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * REST client for the ReJivan server (rejivan.vercel.app).
 * Used ONLY when online — every method returns null-ish on network failure so
 * the app falls back to the on-device engine (complete offline independence).
 */
class Sync(private val base: String = "https://rejivan.vercel.app") {

    data class LoginResult(val token: String, val userName: String, val userRole: String)

    /** Server reachable ? */
    suspend fun ping(): Boolean =
        request("/api/health", "GET", null, null) != null

    suspend fun login(email: String, password: String): LoginResult? {
        val body = JSONObject().put("email", email).put("password", password)
        val raw = request("/api/auth/login", "POST", body.toString(), null) ?: return null
        return try {
            val j = JSONObject(raw)
            val user = j.getJSONObject("user")
            LoginResult(j.optString("token"), user.optString("name"), user.optString("role", "family"))
        } catch (_: Exception) { null }
    }

    suspend fun fetchMedications(token: String): List<Med> {
        val raw = request("/api/medications", "GET", null, token) ?: return emptyList()
        return try {
            val arr = JSONObject(raw).getJSONArray("list")
            (0 until arr.length()).mapNotNull { i -> medFromJson(arr.getJSONObject(i)) }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun pushTake(token: String, med: Med): Boolean {
        val code = requestCode("/api/medications/${med.id}/take", "POST", null, token)
        return code == 200
    }

    suspend fun pushCreate(token: String, med: Med): String? {
        val body = JSONObject()
            .put("patientId", med.patientId).put("name", med.name).put("dose", med.dose)
            .put("frequency", med.frequency)
            .put("times", JSONArray().apply { for (t in med.times) put(t) })
            .put("notes", med.notes)
        val raw = request("/api/medications", "POST", body.toString(), token) ?: return null
        return try { JSONObject(raw).optString("id", null) } catch (_: Exception) { null }
    }

    /** Full dashboard payloads (same endpoints the website uses). */
    suspend fun fetchVitals(token: String): JSONObject? =
        jsonOrNull("/api/vitals", token)

    suspend fun fetchAlerts(token: String): JSONObject? =
        jsonOrNull("/api/alerts", token)

    suspend fun fetchCalls(token: String): JSONObject? =
        jsonOrNull("/api/calls", token)

    suspend fun fetchCamera(token: String): JSONObject? =
        jsonOrNull("/api/camera-zones", token)

    suspend fun fetchLiveFrame(token: String, zoneId: String): JSONObject? =
        jsonOrNull("/api/camera-zones/$zoneId/live", token)

    // ---------- internals ----------
    private suspend fun jsonOrNull(path: String, token: String): JSONObject? {
        val raw = request(path, "GET", null, token) ?: return null
        return try { JSONObject(raw) } catch (_: Exception) { null }
    }

    private suspend fun request(path: String, method: String, body: String?, token: String?): String? =
        syncCall(path, method, body, token).first

    private suspend fun requestCode(path: String, method: String, body: String?, token: String?): Int =
        syncCall(path, method, body, token).second

    private suspend fun syncCall(path: String, method: String, body: String?, token: String?): Pair<String?, Int> =
        withContextSafe {
            try {
                val conn = URL(base + path).openConnection() as HttpURLConnection
                conn.requestMethod = method
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/json")
                if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { (it as OutputStream).write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val str = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else ""
                conn.disconnect()
                (str to code)
            } catch (_: Exception) {
                (null to -1)
            }
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

private suspend fun withContextSafe(block: () -> Pair<String?, Int>): Pair<String?, Int> =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { block() }