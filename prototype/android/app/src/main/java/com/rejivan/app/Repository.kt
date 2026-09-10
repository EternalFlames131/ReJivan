package com.rejivan.app

import android.content.Context
import org.json.JSONObject

/**
 * Repository = the "linked" layer. Tries the ReJivan server first (same
 * endpoints + data the website uses); the moment that fails it transparently
 * switches to the fully-local engine — so the app never breaks offline and the
 * two views always agree.
 */
class Repository(ctx: Context) {

    val store = Store(ctx)
    private val sync = Sync()

    private val demoPassword = "demo123"

    suspend fun login(email: String, password: String): Boolean {
        val online = sync.login(email, password)
        if (online != null) {
            store.token = online.token
            store.email = email
            store.userName = online.userName
            store.userRole = online.userRole
            return true
        }
        val demo = Engine.DEMO_USERS.find { it.email.equals(email, true) && password == demoPassword }
        if (demo != null) {
            store.token = "local-" + demo.email
            store.email = email
            store.userName = demo.name
            store.userRole = demo.role
            return true
        }
        return false
    }

    fun patients(): List<Patient> = Engine.patientsForUser(store.email)

    fun isOnlineLogin(): Boolean = !store.token.startsWith("local-")

    /** Refresh medications from the server and flush any pending on-device actions. */
    suspend fun syncMedications() {
        if (!isOnlineLogin()) return
        val serverMeds = sync.fetchMedications(store.token)
        if (serverMeds.isEmpty()) return

        // two-way: push our pending "taken" actions up (matched by patient+name)
        for (pendingId in store.pendingTakes()) {
            val local = store.medications().find { it.id == pendingId } ?: continue
            val serverMed = serverMeds.find { it.patientId == local.patientId && it.name == local.name } ?: continue
            if (sync.pushTake(store.token, serverMed)) store.clearPendingTake(pendingId)
        }
        // push any medication we created on the device (matched by patient+name)
        val merged = serverMeds.toMutableList()
        for (lm in store.medications()) {
            if (serverMeds.none { it.patientId == lm.patientId && it.name == lm.name }) {
                val newId = sync.pushCreate(store.token, lm)
                merged.add(if (newId != null) lm.copy(id = newId) else lm)
            }
        }
        store.replaceMedications(merged)
    }

    /** Main poll: server first, offline engine as automatic fallback. */
    suspend fun snapshot(): AppSnapshot {
        store.seedDefaultMeds()
        val patients = patients()
        val token = store.token
        val now = System.currentTimeMillis()

        if (isOnlineLogin()) {
            val vj = sync.fetchVitals(token)
            if (vj != null) {
                val aj = sync.fetchAlerts(token)
                val cj = sync.fetchCalls(token)
                val zj = sync.fetchCamera(token)
                val serverMeds = sync.fetchMedications(token)
                val meds = if (serverMeds.isNotEmpty()) serverMeds else store.medications()
                return AppSnapshot(
                    source = Source.ONLINE,
                    patients = parsePatients(vj),
                    alerts = aj?.let { parseAlerts(it) } ?: emptyList(),
                    escalations = aj?.let { parseEscalations(it) } ?: emptyList(),
                    calls = cj?.let { parseCalls(it) } ?: emptyList(),
                    zones = zj?.let { parseZones(it) } ?: emptyList(),
                    cameraEvents = zj?.let { parseCameraEvents(it) } ?: emptyList(),
                    meds = meds,
                )
            }
        }

        // ---- offline: derive everything from the on-device engine ----
        val sn = patients.map { Engine.snapshot(it, now) }
        val (alerts, escalations) = Engine.alertsFor(patients, now)
        val calls = Engine.callsFor(patients, now)
        val (zones, camEvents) = Engine.cameraFor(patients.map { it.id }.toSet(), now)
        return AppSnapshot(
            source = Source.OFFLINE,
            patients = sn,
            alerts = alerts,
            escalations = escalations,
            calls = calls,
            zones = zones,
            cameraEvents = camEvents,
            meds = store.medications(),
        )
    }

    fun liveFrame(zoneId: String, now: Long): Frame = Engine.liveFrame(zoneId, now)

    fun takeMed(med: Med) = store.markTaken(med.id)

    // ---------- server JSON → models ----------
    private fun parsePatients(j: JSONObject): List<PatientSnapshot> {
        val arr = j.getJSONArray("patients")
        val out = mutableListOf<PatientSnapshot>()
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                val v = o.getJSONObject("vitals")
                val r = o.getJSONObject("report")
                val rel = o.getJSONObject("reliability")
                val patient = Patient(
                    id = o.getString("id"), name = o.getString("name"),
                    age = o.optInt("age", 0), sex = o.optString("sex", ""),
                    condition = o.optString("condition", ""),
                    location = o.optString("location", ""),
                    address = o.optString("addr", "").ifBlank { null },
                    ward = o.optString("ward", "").ifBlank { null },
                )
                val vitals = Vitals(
                    hr = parseInt(optOrNull(v, "hr")), spo2 = parseDouble(optOrNull(v, "spo2")),
                    sbp = parseInt(optOrNull(v, "sbp")), dbp = parseInt(optOrNull(v, "dbp")),
                    temp = parseDouble(optOrNull(v, "temp")), glucose = parseInt(optOrNull(v, "glucose")),
                )
                val status = mapOf(
                    "hr" to st(r.optJSONObject("hr"), "status"), "spo2" to st(r.optJSONObject("spo2"), "status"),
                    "sbp" to st(r.optJSONObject("sbp"), "status"), "dbp" to st(r.optJSONObject("dbp"), "status"),
                    "temp" to st(r.optJSONObject("temp"), "status"), "glucose" to st(r.optJSONObject("glucose"), "status"),
                )
                val report = Report(vitals, status, r.optString("bp", "normal"))
                val perMetric = mutableMapOf<String, Int>()
                rel.optJSONObject("perMetricConfidence")?.let { pm -> val keys = pm.keys(); while (keys.hasNext()) { val k = keys.next(); perMetric[k] = pm.optInt(k) } }
                val missing = mutableListOf<String>()
                rel.optJSONObject("degradation")?.optJSONArray("missing")?.let { m -> for (x in 0 until m.length()) { val o2 = m.optJSONObject(x); missing.add(o2?.optString("name") ?: "?") } }
                val episode = o.optJSONObject("episode")?.let { Episode(it.optString("type"), it.optLong("from"), it.optLong("until")) }
                val devices = parseDevices(o.optJSONArray("devices") ?: org.json.JSONArray())
                out.add(PatientSnapshot(
                    patient = patient, vitals = vitals, rawVitals = vitals, episode = episode,
                    reliability = ReliabilityInfo(
                        confidence = rel.optInt("confidence", 0), perMetricConfidence = perMetric,
                        validationPassed = rel.optBoolean("validationPassed", true),
                        rejectedMetrics = rel.optJSONArray("rejectedMetrics")?.let { arr2 -> (0 until arr2.length()).map { arr2.getString(it) } } ?: emptyList(),
                        degradationMissing = missing, degradationSufficient = rel.optJSONObject("degradation")?.optBoolean("sufficient", true) ?: true,
                    ),
                    deviceTier = o.optString("deviceTier", "simulated"),
                    devices = devices,
                    report = report,
                ))
            } catch (_: Exception) { /* skip malformed entry */ }
        }
        return out
    }

    private fun parseDevices(arr: org.json.JSONArray): List<Device> {
        val out = mutableListOf<Device>()
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                out.add(Device(
                    id = o.optString("deviceId").ifBlank { o.optString("id") },
                    name = o.optString("name"), manufacturer = o.optString("manufacturer"),
                    category = o.optString("category"), approval = o.optString("approval"),
                    connectivity = o.optString("connectivity"), accuracy = o.optString("accuracy"),
                    measures = listOf<String>().plus(o.optJSONArray("measures")?.let { m -> (0 until m.length()).map { m.getString(it) } } ?: emptyList()),
                    priceINR = o.optString("priceINR"), madeInIndia = o.optBoolean("madeInIndia", false),
                    productionTier = o.optString("productionTier", "medical"),
                    connected = o.optBoolean("connected", false),
                    battery = if (o.has("battery")) o.optInt("battery") else null,
                    signalStrength = if (o.has("signalStrength")) o.optInt("signalStrength") else null,
                ))
            } catch (_: Exception) { }
        }
        return out
    }

    private fun parseAlerts(j: JSONObject): List<Alert> {
        val arr = j.getJSONArray("alerts")
        val out = mutableListOf<Alert>()
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                out.add(Alert(
                    id = o.getString("id"), patientId = o.getString("patientId"),
                    patientName = o.getString("patientName"), age = if (o.has("age") && !o.isNull("age")) o.optInt("age") else null,
                    ward = o.optString("ward", "").ifBlank { null }, location = o.optString("location", ""),
                    type = o.optString("type", "vitals"), severity = o.optString("severity", "danger"),
                    message = o.optString("message"), confirmed = o.optBoolean("confirmed", false),
                    suspect = o.optJSONArray("suspect")?.let { s -> (0 until s.length()).map { s.getString(it) } } ?: emptyList(),
                    createdAt = o.optLong("createdAt", 0),
                ))
            } catch (_: Exception) { }
        }
        return out.sortedByDescending { it.createdAt }
    }

    private fun parseEscalations(j: JSONObject): List<Escalation> {
        val arr = j.optJSONArray("escalations") ?: return emptyList()
        val out = mutableListOf<Escalation>()
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                out.add(Escalation(
                    id = o.optString("id"), alertId = o.optString("alertId"),
                    patientId = o.optString("patientId"), patientName = o.optString("patientName"),
                    severity = o.optString("severity", "danger"),
                    channels = o.optJSONArray("channels")?.let { c -> (0 until c.length()).map { c.getString(it) } } ?: emptyList(),
                    to = o.optString("to"), dispatchedAt = o.optLong("dispatchedAt", 0),
                    delivered = o.optBoolean("delivered", true), simulationNotice = o.optString("simulationNotice", ""),
                ))
            } catch (_: Exception) { }
        }
        return out
    }

    private fun parseCalls(j: JSONObject): List<CallChain> {
        val arr = j.optJSONArray("calls") ?: return emptyList()
        val out = mutableListOf<CallChain>()
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                val ladder = o.optJSONArray("ladder")?.let { l -> (0 until l.length()).mapNotNull { x ->
                    val lo = l.optJSONObject(x) ?: return@mapNotNull null
                    CallLadder(lo.optString("label"), lo.optString("to"), lo.optString("state"), lo.optBoolean("emergency", false))
                } } ?: emptyList()
                val log = o.optJSONArray("log")?.let { g -> (0 until g.length()).mapNotNull { x ->
                    val go = g.optJSONObject(x) ?: return@mapNotNull null
                    Pair(go.optLong("t"), go.optString("msg"))
                } } ?: emptyList()
                out.add(CallChain(
                    id = o.optString("id"), alertId = o.optString("alertId"),
                    patientId = o.optString("patientId"), patientName = o.optString("patientName"),
                    status = o.optString("status"), ladder = ladder, log = log,
                ))
            } catch (_: Exception) { }
        }
        return out
    }

    private fun parseZones(j: JSONObject): List<CameraZone> {
        val arr = j.optJSONArray("zones") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            try {
                val z = arr.getJSONObject(i)
                CameraZone(z.getString("id"), z.getString("patientId"), z.getString("name"), z.optString("room", ""), z.optBoolean("ward", false))
            } catch (_: Exception) { null }
        }
    }

    private fun parseCameraEvents(j: JSONObject): List<CameraEvent> {
        val arr = j.optJSONArray("events") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            try {
                val e = arr.getJSONObject(i)
                CameraEvent(e.getString("id"), e.getString("zoneId"), e.getString("patientId"), e.getString("zoneName"),
                    e.optString("kind"), e.optString("severity"), e.optString("message"), e.optLong("at", 0))
            } catch (_: Exception) { null }
        }
    }

    private fun optOrNull(o: JSONObject, key: String): Any? = if (o.has(key) && !o.isNull(key)) o.get(key) else null
    private fun parseInt(v: Any?): Int? = when (v) {
        is Int -> v; is Long -> v.toInt(); is Double -> v.toInt(); is String -> v.toIntOrNull()
        else -> null
    }
    private fun parseDouble(v: Any?): Double? = when (v) {
        is Double -> v; is Int -> v.toDouble(); is Long -> v.toDouble(); is String -> v.toDoubleOrNull()
        else -> null
    }
    private fun st(o: JSONObject?, key: String): String = o?.optString(key, "normal") ?: "normal"
}