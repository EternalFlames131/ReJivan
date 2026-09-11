package com.rejivan.app.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * REST client for the ReJivan server (https://rejivan.vercel.app).
 * All methods block — call from a background thread / coroutine.
 * Returns null on any failure (network down, server error, parse error).
 */
object Sync {
    private const val BASE = "https://rejivan.vercel.app"

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // ── Auth ──────────────────────────────────────────────────────────────

    data class LoginResult(val token: String, val userId: String, val name: String, val email: String, val role: String)

    fun login(email: String, password: String): LoginResult? {
        val body = JSONObject().put("email", email).put("password", password).toString()
        val resp = post("/api/auth/login", body) ?: return null
        return try {
            LoginResult(
                token = resp.getString("token"),
                userId = resp.getJSONObject("user").getString("id"),
                name = resp.getJSONObject("user").getString("name"),
                email = resp.getJSONObject("user").getString("email"),
                role = resp.getJSONObject("user").getString("role")
            )
        } catch (e: Exception) { null }
    }

    // ── Vitals ────────────────────────────────────────────────────────────

    data class ServerPatient(
        val id: String, val name: String, val age: Int, val sex: String,
        val condition: String, val location: String, val addr: String?,
        val ward: String?, val region: String,
        val vitals: JSONObject, val report: JSONObject?,
        val reliability: JSONObject?, val deviceTier: String?,
        val episode: JSONObject?
    )

    fun fetchVitals(token: String): List<ServerPatient>? {
        val arr = getArray("/api/vitals", token) ?: return null
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            try {
                ServerPatient(
                    id = o.getString("id"), name = o.getString("name"),
                    age = o.optInt("age", 0), sex = o.optString("sex", ""),
                    condition = o.optString("condition", ""),
                    location = o.optString("location", ""),
                    addr = o.optString("addr", null),
                    ward = o.optString("ward", null).let { if (it == "null" || it.isEmpty()) null else it },
                    region = o.optString("region", ""),
                    vitals = o.optJSONObject("vitals") ?: JSONObject(),
                    report = o.optJSONObject("report"),
                    reliability = o.optJSONObject("reliability"),
                    deviceTier = o.optString("deviceTier", null),
                    episode = o.optJSONObject("episode")
                )
            } catch (e: Exception) { null }
        }
    }

    // ── Alerts ────────────────────────────────────────────────────────────

    data class ServerAlert(
        val id: String, val patientId: String, val patientName: String,
        val severity: String, val message: String, val type: String,
        val createdAt: Long
    )

    data class ServerEscalation(
        val id: String, val alertId: String, val patientId: String,
        val patientName: String, val severity: String, val channels: List<String>,
        val to: String, val dispatchedAt: Long, val delivered: Boolean
    )

    fun fetchAlerts(token: String): Pair<List<ServerAlert>, List<ServerEscalation>>? {
        val obj = get("/api/alerts", token) ?: return null
        val alertsArr = obj.optJSONArray("alerts") ?: JSONArray()
        val escArr = obj.optJSONArray("escalations") ?: JSONArray()
        val alerts = (0 until alertsArr.length()).mapNotNull { i ->
            val o = alertsArr.getJSONObject(i)
            try {
                ServerAlert(o.getString("id"), o.getString("patientId"), o.getString("patientName"),
                    o.optString("severity", "caution"), o.optString("message", ""),
                    o.optString("type", "vitals"), o.optLong("createdAt", 0))
            } catch (e: Exception) { null }
        }
        val escs = (0 until escArr.length()).mapNotNull { i ->
            val o = escArr.getJSONObject(i)
            try {
                val chArr = o.optJSONArray("channels") ?: JSONArray()
                val channels = (0 until chArr.length()).map { chArr.getString(it) }
                ServerEscalation(o.getString("id"), o.getString("alertId"), o.getString("patientId"),
                    o.getString("patientName"), o.optString("severity", "danger"),
                    channels, o.optString("to", ""), o.optLong("dispatchedAt", 0),
                    o.optBoolean("delivered", false))
            } catch (e: Exception) { null }
        }
        return Pair(alerts, escs)
    }

    // ── Calls ─────────────────────────────────────────────────────────────

    data class ServerCallStep(val label: String, val to: String, val mode: String, val emergency: Boolean, val state: String)
    data class ServerCall(
        val id: String, val alertId: String, val patientId: String, val patientName: String,
        val startedAt: Long, val status: String, val steps: List<ServerCallStep>,
        val log: List<Pair<Long, String>>
    )

    fun fetchCalls(token: String): List<ServerCall>? {
        val obj = get("/api/calls", token) ?: return null
        val arr = obj.optJSONArray("calls") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            try {
                val stepsArr = o.getJSONArray("ladder")
                val steps = (0 until stepsArr.length()).map { j ->
                    val s = stepsArr.getJSONObject(j)
                    ServerCallStep(s.getString("label"), s.getString("to"),
                        s.optString("mode", "voice"), s.optBoolean("emergency", false),
                        s.optString("state", "pending"))
                }
                val logArr = o.optJSONArray("log") ?: JSONArray()
                val log = (0 until logArr.length()).map { j ->
                    val l = logArr.getJSONObject(j)
                    Pair(l.optLong("t", 0), l.optString("msg", ""))
                }
                ServerCall(o.getString("id"), o.getString("alertId"), o.getString("patientId"),
                    o.getString("patientName"), o.optLong("startedAt", 0),
                    o.optString("status", "pending"), steps, log)
            } catch (e: Exception) { null }
        }
    }

    // ── Camera zones ──────────────────────────────────────────────────────

    data class ServerZone(val id: String, val name: String, val room: String, val patientId: String, val ward: Boolean)

    fun fetchCameraZones(token: String): List<ServerZone>? {
        val obj = get("/api/camera-zones", token) ?: return null
        val arr = obj.optJSONArray("zones") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            try {
                ServerZone(o.getString("id"), o.getString("name"),
                    o.optString("room", ""), o.getString("patientId"), o.optBoolean("ward", false))
            } catch (e: Exception) { null }
        }
    }

    // ── Medications ───────────────────────────────────────────────────────

    data class ServerMed(val id: String, val patientId: String, val name: String,
        val dose: String, val frequency: String, val times: List<String>,
        val notes: String, val active: Boolean)

    fun fetchMedications(token: String): List<ServerMed>? {
        val obj = get("/api/medications", token) ?: return null
        val arr = obj.optJSONArray("list") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            try {
                val timesArr = o.optJSONArray("times") ?: JSONArray()
                val times = (0 until timesArr.length()).map { timesArr.getString(it) }
                ServerMed(o.getString("id"), o.getString("patientId"), o.getString("name"),
                    o.optString("dose", ""), o.optString("frequency", ""),
                    times, o.optString("notes", ""), o.optBoolean("active", true))
            } catch (e: Exception) { null }
        }
    }

    fun postMedTake(token: String, medId: String): Boolean {
        return post("/api/medications/$medId/take", "{}", token) != null
    }

    fun postMedCreate(token: String, patientId: String, name: String, dose: String,
                      frequency: String, times: List<String>, notes: String): String? {
        val timesArr = JSONArray()
        times.forEach { timesArr.put(it) }
        val body = JSONObject()
            .put("patientId", patientId)
            .put("name", name)
            .put("dose", dose)
            .put("frequency", frequency)
            .put("times", timesArr)
            .put("notes", notes)
        val resp = post("/api/medications", body.toString(), token) ?: return null
        return try { resp.getString("id") } catch (e: Exception) { null }
    }

    fun deleteMed(token: String, medId: String): Boolean {
        val req = Request.Builder()
            .url("$BASE/api/medications/$medId")
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return try {
            val resp = client.newCall(req).execute()
            resp.isSuccessful
        } catch (e: Exception) { false }
    }

    // ── Medical devices (per-patient registry + catalogue) ─────────────────

    data class ServerDevice(
        val deviceId: String, val name: String, val manufacturer: String,
        val category: String, val approval: String, val connectivity: String,
        val accuracy: String, val measures: List<String>, val priceINR: String,
        val madeInIndia: Boolean, val description: String, val productionTier: String,
        val connected: Boolean, val battery: Int, val signalStrength: Int,
        val secondsSinceLastSeen: Int
    )

    data class PatientDevices(
        val patientId: String, val patientName: String, val devices: List<ServerDevice>
    )

    fun fetchDevices(token: String): List<PatientDevices>? {
        val obj = get("/api/devices", token) ?: return null
        val arr = obj.optJSONArray("patients") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            try {
                val devArr = o.optJSONArray("devices") ?: JSONArray()
                val devs = (0 until devArr.length()).mapNotNull { j ->
                    parseDevice(devArr.getJSONObject(j))
                }
                PatientDevices(o.getString("patientId"), o.getString("patientName"), devs)
            } catch (e: Exception) { null }
        }
    }

    fun fetchCatalogue(token: String): List<ServerDevice>? {
        val obj = get("/api/devices/catalogue", token) ?: return null
        val arr = obj.optJSONArray("catalogue") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i -> parseDevice(arr.getJSONObject(i)) }
    }

    private fun parseDevice(d: JSONObject): ServerDevice? {
        return try {
            val mea = d.optJSONArray("measures") ?: JSONArray()
            val measures = (0 until mea.length()).map { mea.getString(it) }
            ServerDevice(
                d.getString("deviceId"), d.getString("name"), d.optString("manufacturer", ""),
                d.optString("category", ""), d.optString("approval", ""),
                d.optString("connectivity", ""), d.optString("accuracy", ""),
                measures, d.optString("priceINR", ""),
                d.optBoolean("madeInIndia", false), d.optString("description", ""),
                d.optString("productionTier", ""),
                d.optBoolean("connected", false), d.optInt("battery", 0),
                d.optInt("signalStrength", 0), d.optInt("secondsSinceLastSeen", 0)
            )
        } catch (e: Exception) { null }
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────

    private fun get(path: String, token: String): JSONObject? {
        val req = Request.Builder()
            .url(BASE + path)
            .addHeader("Authorization", "Bearer $token")
            .get().build()
        return execute(req) { JSONObject(it) }
    }

    private fun getArray(path: String, token: String): JSONArray? {
        val req = Request.Builder()
            .url(BASE + path)
            .addHeader("Authorization", "Bearer $token")
            .get().build()
        return execute(req) { JSONArray(it) }
    }

    private fun post(path: String, jsonBody: String, token: String? = null): JSONObject? {
        val builder = Request.Builder()
            .url(BASE + path)
            .post(jsonBody.toRequestBody(JSON_TYPE))
        if (token != null) builder.addHeader("Authorization", "Bearer $token")
        return execute(builder.build()) { JSONObject(it) }
    }

    private fun <T> execute(req: Request, parse: (String) -> T): T? {
        return try {
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return null
            if (!resp.isSuccessful) return null
            parse(body)
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
