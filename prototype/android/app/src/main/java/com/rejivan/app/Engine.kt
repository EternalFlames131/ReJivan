package com.rejivan.app

import java.security.MessageDigest
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Offline ReJivan engine — a Kotlin port of the server's pure logic
 * (simulator.js / rules.js / camerazone.js / reliability.js / medical-devices.js).
 *
 * It is DETERMINISTIC: state is a pure function of (patient, wall-clock time),
 * exactly like the server, so the app and the website show the same information.
 * When offline the app uses this engine; when online it fetches from the server.
 */
object Engine {

    const val REGION = "Andaman & Nicobar Islands (UT), India"
    const val SLOT_MS = 200_000.0
    const val SPAN_ALERTS = 30

    // ---- token hashing (MD5, identical to server) ----
    fun hash01(seed: String): Double {
        val md = MessageDigest.getInstance("MD5")
        val hex = md.digest(seed.toByteArray(Charsets.UTF_8)).joinToString("") { String.format("%02x", it) }
        return hex.substring(0, 8).toLong(16) / 4_294_967_295.0
    }

    private fun clamp(v: Double, lo: Double, hi: Double) = maxOf(lo, minOf(hi, v))
    private fun round1(v: Double): Double = Math.round(v * 10) / 10.0

    // ---- baseline profiles ----
    private val BASELINES = mapOf(
        "healthy" to mapOf("hr" to 74.0, "spo2" to 98.0, "sbp" to 120.0, "dbp" to 80.0, "temp" to 36.8, "glucose" to 100.0),
        "heart-arrhythmia" to mapOf("hr" to 88.0, "spo2" to 96.0, "sbp" to 128.0, "dbp" to 82.0, "temp" to 36.9, "glucose" to 104.0),
        "post-surgery" to mapOf("hr" to 80.0, "spo2" to 97.0, "sbp" to 118.0, "dbp" to 76.0, "temp" to 37.0, "glucose" to 110.0),
        "diabetes" to mapOf("hr" to 78.0, "spo2" to 97.0, "sbp" to 130.0, "dbp" to 85.0, "temp" to 36.7, "glucose" to 150.0),
        "hypertension" to mapOf("hr" to 80.0, "spo2" to 97.0, "sbp" to 150.0, "dbp" to 96.0, "temp" to 36.8, "glucose" to 108.0),
        "elderly" to mapOf("hr" to 76.0, "spo2" to 96.0, "sbp" to 135.0, "dbp" to 82.0, "temp" to 36.5, "glucose" to 105.0),
    )

    private val EPISODES = mapOf(
        "healthy" to mapOf(
            "tachycardia spike" to mapOf("hr" to 168.0),
            "brief desaturation" to mapOf("spo2" to 88.0),
            "stress rise" to mapOf("sbp" to 152.0, "dbp" to 98.0),
        ),
        "heart-arrhythmia" to mapOf(
            "arrhythmia episode" to mapOf("hr" to 172.0, "sbp" to 140.0),
            "rapid HR" to mapOf("hr" to 182.0),
        ),
        "post-surgery" to mapOf(
            "fever spike" to mapOf("temp" to 39.1),
            "desaturation" to mapOf("spo2" to 86.0),
        ),
        "diabetes" to mapOf(
            "hypoglycaemia" to mapOf("glucose" to 62.0),
            "hyperglycaemia" to mapOf("glucose" to 255.0),
        ),
        "hypertension" to mapOf(
            "hypertensive crisis" to mapOf("sbp" to 178.0, "dbp" to 112.0),
        ),
        "elderly" to mapOf(
            "bradycardia" to mapOf("hr" to 46.0),
            "desaturation" to mapOf("spo2" to 87.0),
            "fall-risk episode" to mapOf("sbp" to 146.0),
        ),
    )

    private val NOISE = mapOf(
        "hr" to Noise(a1 = 6.0, a2 = 2.5, p1 = 21.0, p2 = 6.0, j = 1.5, lo = 40.0, hi = 200.0, dp = 0),
        "spo2" to Noise(a1 = 0.7, a2 = 0.3, p1 = 25.0, p2 = 8.0, j = 0.3, lo = 70.0, hi = 100.0, dp = 1),
        "sbp" to Noise(a1 = 5.0, a2 = 2.0, p1 = 31.0, p2 = 9.0, j = 2.0, lo = 70.0, hi = 210.0, dp = 0),
        "dbp" to Noise(a1 = 3.5, a2 = 1.5, p1 = 31.0, p2 = 9.0, j = 1.5, lo = 40.0, hi = 140.0, dp = 0),
        "temp" to Noise(a1 = 0.15, a2 = 0.06, p1 = 47.0, p2 = 15.0, j = 0.05, lo = 34.0, hi = 42.0, dp = 1),
        "glucose" to Noise(a1 = 8.0, a2 = 4.0, p1 = 41.0, p2 = 13.0, j = 4.0, lo = 60.0, hi = 300.0, dp = 0),
    )

    private data class Noise(val a1: Double, val a2: Double, val p1: Double, val p2: Double, val j: Double, val lo: Double, val hi: Double, val dp: Int)

    // ---- vitals ----
    fun generateVitals(spec: Patient, nowMs: Long): Triple<Vitals, Episode?, Long> {
        val t = nowMs / 1000.0
        val base = BASELINES[spec.condition] ?: BASELINES["healthy"]!!
        val slot = (t / (SLOT_MS / 1000)).toLong()
        val rh = hash01(spec.id + ":ep:" + slot)
        val types = (EPISODES[spec.condition] ?: EPISODES["healthy"]!!).keys.toList()

        var targets: Map<String, Double> = base
        var episode: Episode? = null
        if (rh < 0.5) {
            val idx = minOf(types.size - 1, ((rh / 0.5) * types.size).toInt())
            val type = types[idx]
            val ep = (EPISODES[spec.condition] ?: EPISODES["healthy"]!!)[type]!!
            episode = Episode(type, slot * SLOT_MS.toLong(), (slot + 1) * SLOT_MS.toLong())
            val frac = (t % (SLOT_MS / 1000)) / (SLOT_MS / 1000)
            val ramp = sin(Math.PI * clamp(frac, 0.0, 1.0))
            val mixed = mutableMapOf<String, Double>()
            for ((m, bv) in base) mixed[m] = bv + (ep[m]?.let { it - bv } ?: 0.0) * ramp
            targets = mixed
        }

        val v = mutableMapOf<String, Double>()
        for ((m, n) in NOISE) {
            val ph1 = hash01(spec.id + ":ph:" + m + ":a") * Math.PI * 2
            val ph2 = hash01(spec.id + ":ph:" + m + ":b") * Math.PI * 2
            val seq = (t / 2).toLong()
            val jitter = (hash01(spec.id + ":j:" + m + ":" + seq) - 0.5) * 2 * n.j
            val wander = n.a1 * sin((t / n.p1) * Math.PI * 2 + ph1) + n.a2 * sin((t / n.p2) * Math.PI * 2 + ph2)
            val valRaw = clamp(targets[m]!! + wander + jitter, n.lo, n.hi)
            v[m] = if (n.dp == 1) round1(valRaw) else Math.round(valRaw).toDouble()
        }

        val vitals = Vitals(
            hr = v["hr"]!!.roundToInt(),
            spo2 = v["spo2"]!!,
            sbp = v["sbp"]!!.roundToInt(),
            dbp = v["dbp"]!!.roundToInt(),
            temp = v["temp"]!!,
            glucose = v["glucose"]!!.roundToInt(),
        )
        return Triple(vitals, episode, slot)
    }

    // ---- rules (clinical thresholds, REAL logic) ----
    private val RULES = mapOf(
        "hr" to Rule(lo = 60.0, cautionLo = 50.0, hi = 100.0, cautionHi = 120.0),
        "spo2" to Rule(lo = 95.0, hi = null, danger = 90.0),
        "sbp" to Rule(lo = 90.0, hi = 140.0, cautionHi = 160.0),
        "dbp" to Rule(lo = 60.0, hi = 90.0, cautionHi = 100.0),
        "temp" to Rule(lo = 36.0, hi = 37.5, cautionHi = 38.5),
        "glucose" to Rule(lo = 70.0, hi = 140.0, cautionLo = 70.0, cautionHi = 180.0),
    )
    private data class Rule(val lo: Double?, val hi: Double?, val cautionLo: Double? = null, val cautionHi: Double? = null, val danger: Double? = null)

    private fun statusFor(metric: String, value: Double): String {
        val r = RULES[metric] ?: return "normal"
        if (metric == "spo2") {
            if (r.danger != null && value < r.danger) return "danger"
            if (value < r.lo!!) return "caution"
            return "normal"
        }
        if (value < r.lo!!) {
            if (r.cautionLo != null && value >= r.cautionLo) return "caution"
            return "danger"
        }
        if (r.hi != null && value > r.hi) {
            if (r.cautionHi != null && value <= r.cautionHi) return "caution"
            return "danger"
        }
        return "normal"
    }

    private fun bloodPressureStatus(sbp: Double, dbp: Double): String {
        val s = statusFor("sbp", sbp)
        val d = statusFor("dbp", dbp)
        return if (s == "danger" || d == "danger") "danger" else if (s == "caution" || d == "caution") "caution" else "normal"
    }

    fun vitalsReport(p: Patient, vitals: Vitals): Report {
        val status = mapOf(
            "hr" to (vitals.hr?.let { statusFor("hr", it.toDouble()) } ?: "normal"),
            "spo2" to (vitals.spo2?.let { statusFor("spo2", it) } ?: "normal"),
            "sbp" to (vitals.sbp?.let { statusFor("sbp", it.toDouble()) } ?: "normal"),
            "dbp" to (vitals.dbp?.let { statusFor("dbp", it.toDouble()) } ?: "normal"),
            "temp" to (vitals.temp?.let { statusFor("temp", it) } ?: "normal"),
            "glucose" to (vitals.glucose?.let { statusFor("glucose", it.toDouble()) } ?: "normal"),
        )
        val bp = if (vitals.sbp != null && vitals.dbp != null) bloodPressureStatus(vitals.sbp.toDouble(), vitals.dbp.toDouble()) else "normal"
        return Report(vitals, status, bp)
    }

    // ---- reliability (validation / confidence / consecutive / rate limit) ----
    private val PHYSIO = mapOf(
        "hr" to Pair(20.0, 250.0), "spo2" to Pair(50.0, 100.0), "sbp" to Pair(40.0, 280.0),
        "dbp" to Pair(20.0, 180.0), "temp" to Pair(30.0, 44.0), "glucose" to Pair(20.0, 600.0),
    )

    fun validateVitals(v: Vitals): Vitals {
        fun pick(m: String, value: Double?): Double? {
            if (value == null) return null
            val (lo, hi) = PHYSIO[m]!!
            return if (value < lo || value > hi) null else value
        }
        var sbp = pick("sbp", v.sbp?.toDouble())
        var dbp = pick("dbp", v.dbp?.toDouble())
        if (sbp != null && dbp != null && sbp <= dbp) { sbp = null; dbp = null }
        return Vitals(
            hr = pick("hr", v.hr?.toDouble())?.roundToInt(),
            spo2 = pick("spo2", v.spo2),
            sbp = sbp?.roundToInt(),
            dbp = dbp?.roundToInt(),
            temp = pick("temp", v.temp),
            glucose = pick("glucose", v.glucose?.toDouble())?.roundToInt(),
        )
    }

    private val SENSOR_TIMEOUT_MS = 120_000L

    fun degradationStatus(v: Vitals): Pair<List<String>, Boolean> {
        val missing = mutableListOf<String>()
        val mNames = mapOf("hr" to "Heart Rate", "spo2" to "SpO2", "sbp" to "Systolic BP", "dbp" to "Diastolic BP", "temp" to "Temperature", "glucose" to "Glucose")
        for ((m, _) in PHYSIO) if (v.value(m) == null) missing.add(mNames[m]!!)
        val sufficient = v.hr != null && (v.spo2 != null || v.sbp != null)
        return missing to sufficient
    }

    private val consecutive = mutableMapOf<String, Pair<Int, Long>>()

    fun checkConsecutive(patientId: String, metric: String, isDanger: Boolean, currentSlot: Long): Boolean {
        val key = "$patientId:$metric"
        val prev = consecutive[key]
        if (!isDanger) {
            consecutive[key] = Pair(0, currentSlot)
            return false
        }
        if (prev != null && prev.second == currentSlot) return prev.first >= 2
        val newCount = if (prev != null && prev.second == currentSlot - 1) prev.first + 1 else 1
        consecutive[key] = Pair(newCount, currentSlot)
        return newCount >= 2
    }

    private val rateTimestamps = mutableMapOf<String, MutableList<Long>>()
    private const val MAX_ALERTS = 5
    private const val RATE_WINDOW = 300_000L

    fun rateLimitCheck(patientId: String, now: Long): Boolean {
        val list = rateTimestamps.getOrPut(patientId) { mutableListOf() }
        list.removeAll { now - it >= RATE_WINDOW }
        if (list.size >= MAX_ALERTS) return false
        list.add(now)
        return true
    }

    private fun Vitals.value(m: String): Double? = when (m) {
        "hr" -> hr?.toDouble(); "spo2" -> spo2; "sbp" -> sbp?.toDouble()
        "dbp" -> dbp?.toDouble(); "temp" -> temp; "glucose" -> glucose?.toDouble()
        else -> null
    }

    fun confidenceScore(metric: String, value: Double?, deviceTier: String = "simulated"): Int {
        if (value == null) return 0
        val limits = PHYSIO[metric] ?: return 50
        val tierBase = mapOf("medical" to 85, "consumer" to 70, "simulated" to 60)
        val base = tierBase[deviceTier] ?: 60
        val range = limits.second - limits.first
        val mid = (limits.second + limits.first) / 2
        val dist = Math.abs(value - mid) / (range / 2)
        val penalty = if (dist > 0.6) (dist - 0.6) * 50 else 0.0
        return maxOf(0, minOf(100, Math.round(base - penalty).toInt()))
    }

    fun overallConfidence(v: Vitals, deviceTier: String): Pair<Int, Map<String, Int>> {
        val per = mutableMapOf<String, Int>()
        var total = 0.0
        var count = 0
        for (m in PHYSIO.keys) {
            val valM = v.value(m)
            if (valM != null) { per[m] = confidenceScore(m, valM, deviceTier); total += per[m]!!; count++ } else per[m] = 0
        }
        return (if (count > 0) Math.round(total / count).toInt() else 0) to per
    }

    // ---- camera zones (privacy-first, SIMULATED events) ----
    private const val CAM_SLOT_MS = 150_000L
    private val ZONES = listOf(
        CameraZone("CAM1", "P1", "Home — Living Room", "Home · Junglighat, Port Blair"),
        CameraZone("CAM2", "P2", "Home — Bedroom", "Home · Hut Bay, Little Andaman"),
        CameraZone("BED1", "P3", "GB Pant Virtual Ward · Bed 1", "GB Pant Hospital, Port Blair", ward = true),
        CameraZone("BED2", "P4", "GB Pant Virtual Ward · Bed 2", "GB Pant Hospital, Port Blair", ward = true),
    )
    private val KINDS_MSG = mapOf(
        "fall" to { z: CameraZone -> "${z.name}: possible fall detected — no movement response." },
        "out_of_bed" to { z: CameraZone -> "${z.name}: out of bed at an unexpected hour." },
        "low_activity" to { z: CameraZone -> "${z.name}: very low activity over 6 hours." },
        "no_activity_10min" to { z: CameraZone -> "${z.name}: no movement for 10 minutes." },
    )
    private val KINDS_SEV = mapOf("fall" to "danger", "out_of_bed" to "caution", "low_activity" to "caution", "no_activity_10min" to "caution")

    fun cameraFor(userIds: Set<String>, now: Long): Pair<List<CameraZone>, List<CameraEvent>> {
        val zones = ZONES.filter { it.patientId in userIds }
        val events = mutableListOf<CameraEvent>()
        val cur = now / CAM_SLOT_MS
        val keys = KINDS_SEV.keys.toList()
        for (k in SPAN_ALERTS downTo 0) {
            val s = cur - k
            val rh = hash01("cam:" + s)
            if (rh < 0.32) {
                val zone = ZONES[((hash01("camz:" + s) * ZONES.size).toInt())]
                val kind = keys[((hash01("camk:" + s) * keys.size).toInt())]
                if (zone.patientId in userIds) {
                    events.add(CameraEvent("EVENT-CAM-$s", zone.id, zone.patientId, zone.name, kind, KINDS_SEV[kind]!!, KINDS_MSG[kind]!!(zone), s * CAM_SLOT_MS))
                }
            }
        }
        return zones to events
    }

    fun liveFrame(zoneId: String, nowMs: Long): Frame {
        val t = nowMs / 1000.0
        val ph1 = hash01(zoneId + ":m") * Math.PI * 2
        val ph2 = hash01(zoneId + ":p") * Math.PI * 2
        val seq = (t / 2).toLong()
        val jit = (hash01(zoneId + ":j:" + seq) - 0.5) * 0.2
        val motion = clamp(0.45 + 0.25 * sin(t * 0.7 + ph1) + jit, 0.0, 1.0)
        val person = zoneId.startsWith("BED") || sin(t * 0.13 + ph2) > -0.25
        val lighting = if (hash01(zoneId + ":day:" + (t / 900).toLong()) < 0.12) "night" else "day"
        return Frame(nowMs, motion, person, lighting)
    }

    // ---- medical devices (CDSCO/FDA profiles + per-patient registry) ----
    private val CATALOGUE = listOf(
        Device("SANKETLIFE_12", "SanketLife 12-Lead ECG", "Agatsa (Pune, India)", "ecg", "CDSCO Class B, CE certified", "BLE", "98.5% (validated at Narayana Health)", listOf("hr"), "5000", true),
        Device("HEXOSKIN_MEDICAL", "Hexoskin Medical System", "Hexoskin (Canada)", "ecg", "FDA 510(k) cleared", "BLE", "Clinical-grade ECG + respiration", listOf("hr"), "45000", false),
        Device("CHOICEMMED_MD300", "ChoiceMMed MD300C228", "Beijing Choice (China)", "spo2", "FDA 510(k) cleared", "BLE 5.0", "±2% SpO2", listOf("spo2"), "4000", false),
        Device("LEPU_AP10", "Lepu AP-10 Wrist Oximeter", "Lepu Medical", "spo2", "FDA cleared, CE certified", "BLE 4.0", "±2% SpO2, continuous wrist-worn", listOf("spo2"), "10000", false),
        Device("OMRON_BP", "Omron HEM-7156T", "Omron (Japan)", "bp", "FDA, CE, CDSCO", "BLE", "±3 mmHg", listOf("sbp", "dbp"), "4500", false),
        Device("BIOBEAT_CHEST", "Biobeat BB-613 Chest Patch", "Biobeat (Israel)", "bp", "FDA 510(k) cleared, CE marked", "BLE → Cloud API", "Cuffless BP from PPG (±5 mmHg)", listOf("hr", "sbp", "dbp", "spo2", "temp"), "25000", false),
        Device("TEMPTRAQ_PATCH", "TempTraq Continuous Temp Patch", "Blue Spark Technologies (USA)", "temperature", "FDA Class II cleared", "BLE", "±0.1°C", listOf("temp"), "2000", false),
        Device("AION_TEMPSHIELD", "AION TempShield", "AION Biosystems (USA)", "temperature", "FDA 510(k) cleared", "BLE + NFC", "±0.05°C (extrapolated core temp)", listOf("temp"), "15000", false),
        Device("FREESTYLE_LIBRE3", "FreeStyle Libre 3", "Abbott", "glucose", "FDA cleared, CDSCO approved (India)", "BLE", "MARD 7.9% (real-time, 1-min intervals)", listOf("glucose"), "4670", false),
        Device("GLUCORX_VIXXA2", "GlucoRx Vixxa 2", "MicroTech Medical (China, sold by GlucoRx India)", "glucose", "CE, CDSCO Class B certified", "BLE", "60-second readings", listOf("glucose"), "3200", false),
        Device("H360_HEALTH360", "H360 Health360", "Medilogy Inc (India)", "multi", "CDSCO (Made in India)", "WiFi (2.4GHz)", "SpO2 ±2%, ECG clinical-grade", listOf("hr", "spo2"), "7000", true),
    )
    private val deviceDefaults = mapOf(
        "P1" to listOf("SANKETLIFE_12", "OMRON_BP", "TEMPTRAQ_PATCH"),
        "P2" to listOf("FREESTYLE_LIBRE3", "CHOICEMMED_MD300", "OMRON_BP"),
        "P3" to listOf("BIOBEAT_CHEST", "FREESTYLE_LIBRE3"),
        "P4" to listOf("BIOBEAT_CHEST", "SANKETLIFE_12", "TEMPTRAQ_PATCH"),
    )

    fun getPatientDevices(patientId: String, now: Long): List<Device> {
        val out = mutableListOf<Device>()
        for (deviceId in deviceDefaults[patientId] ?: emptyList()) {
            val cat = CATALOGUE.find { it.id == deviceId } ?: continue
            val batteryDrift = ((now / 60000) % 60).toInt()
            val battery = maxOf(5, ((40 + batteryDrift * 10) - Math.floor(batteryDrift * 0.1).toInt()) % 100)
            val signal = minOf(100, maxOf(10, 60 + Math.floor(sin(now / 30000.0) * 5).toInt()))
            out.add(cat.copy(connected = true, battery = battery, signalStrength = signal))
        }
        return out
    }

    private fun deviceConfidence(v: Vitals, patientId: String, now: Long): Pair<String, Double> {
        val devices = getPatientDevices(patientId, now)
        val medical = devices.filter { it.connected && it.productionTier == "medical" }
        return if (medical.isEmpty()) "simulated" to 1.0 else "medical" to 1.4
    }

    // ---- demo accounts + patients (same as server seeds) ----
    val DEMO_USERS = listOf(
        DemoUser("asharma@demo.in", "Sharma Family", "family", listOf("P1")),
        DemoUser("rprakash@demo.in", "Ram Prakash Family", "family", listOf("P2")),
        DemoUser("wardnurse@demo.in", "Ward Nurse", "ward", listOf("P3", "P4")),
    )

    val PATIENTS = listOf(
        Patient("P1", "Anita Sharma", 67, "F", "hypertension", "Home — Living Room", "Junglighat, Port Blair"),
        Patient("P2", "Ram Prakash", 74, "M", "diabetes", "Home — Bedroom", "Hut Bay, Little Andaman (served via PHC)"),
        Patient("P3", "Meera Nair", 58, "F", "post-surgery", "Virtual Ward", "GB Pant Hospital, Port Blair", "Ward A · Bed 1"),
        Patient("P4", "Kavitha Rao", 61, "F", "heart-arrhythmia", "Virtual Ward", "GB Pant Hospital, Port Blair", "Ward A · Bed 2"),
    )

    private val CONTACTS = mapOf(
        "P1" to Contact("+91 98300 11001", "+91 98300 11002"),
        "P2" to Contact("+91 98300 12001", "+91 98300 12002"),
        "P3" to Contact("Ward A nurse · +91 98300 13001", "Duty doctor · +91 98300 13002"),
        "P4" to Contact("Ward A nurse · +91 98300 14001", "Duty doctor · +91 98300 14002"),
    )
    private data class Contact(val family: String, val backup: String)

    fun patientsForUser(email: String): List<Patient> {
        val userIds = DEMO_USERS.find { it.email.equals(email, true) }?.patientIds ?: return emptyList()
        return PATIENTS.filter { it.id in userIds }
    }

    // ---- per-patient snapshot (offline mirror of the server endpoint) ----
    fun snapshot(patient: Patient, now: Long): PatientSnapshot {
        val (raw, episode, _) = generateVitals(patient, now)
        val clean = validateVitals(raw)
        val (tier, multiplier) = deviceConfidence(clean, patient.id, now)
        var overall = overallConfidence(clean, if (tier == "medical") "medical" else "simulated")
        if (tier == "medical") overall = Pair(minOf(100, Math.round(overall.first * multiplier).toInt()), overall.second)
        val (missing, sufficient) = degradationStatus(clean)
        return PatientSnapshot(
            patient = patient,
            vitals = clean,
            rawVitals = raw,
            episode = episode,
            reliability = ReliabilityInfo(
                confidence = overall.first,
                perMetricConfidence = overall.second,
                validationPassed = clean.valueAllPresent(),
                rejectedMetrics = emptyList(),
                degradationMissing = missing,
                degradationSufficient = sufficient,
            ),
            deviceTier = tier,
            devices = getPatientDevices(patient.id, now),
            report = vitalsReport(patient, clean),
        )
    }

    private fun Vitals.valueAllPresent(): Boolean = hr != null && spo2 != null && sbp != null && dbp != null && temp != null && glucose != null

    // ---- alerts / escalations / calls (offline mirror of the server) ----
    fun alertsFor(patients: List<Patient>, now: Long): Pair<List<Alert>, List<Escalation>> {
        val ids = patients.map { it.id }.toSet()
        val alerts = mutableListOf<Alert>()
        val escalations = mutableListOf<Escalation>()
        val slot = (now / SLOT_MS).toLong()
        for (k in SPAN_ALERTS downTo 0) {
            val s = slot - k
            val center = (s + 0.5) * SLOT_MS
            for (p in patients) {
                val (vitals, _, _) = generateVitals(p, center.toLong())
                val clean = validateVitals(vitals)
                val report = vitalsReport(p, clean)
                val confirmed = mutableListOf<String>()
                val suspect = mutableListOf<String>()
                for (m in listOf("hr", "spo2", "bp", "temp", "glucose")) {
                    val st = if (m == "bp") report.bpStatus else report.metricStatus[m] ?: "normal"
                    if (st == "danger") {
                        val label = if (m == "bp") "Blood Pressure" else m.uppercase()
                        if (checkConsecutive(p.id, m, true, s)) confirmed.add(label) else suspect.add(label)
                    } else {
                        checkConsecutive(p.id, m, false, s)
                    }
                }
                if (confirmed.isNotEmpty()) {
                    pushDanger(Alert(
                        id = "ALT-${p.id}-$s", patientId = p.id, patientName = p.name, age = p.age,
                        ward = p.ward, location = p.location, type = "vitals", severity = "danger",
                        message = "${p.name} — CONFIRMED DANGER: ${confirmed.joinToString(", ")} out of normal range (persisted across readings). ${p.ward?.let { "Virtual Ward $it" } ?: "At home"}. Automated check.",
                        confirmed = true, suspect = suspect, createdAt = s * SLOT_MS.toLong(),
                    ), alerts, escalations, now)
                } else if (suspect.isNotEmpty()) {
                    // single-reading danger — logged, not escalated (audit entry omitted for brevity)
                }
            }
        }
        val (_, events) = cameraFor(ids, now)
        for (e in events.filter { it.severity == "danger" }) {
            val p = patients.find { it.id == e.patientId }
            if (p != null) {
                pushDanger(Alert(
                    id = "EVT-${e.id}", patientId = e.patientId, patientName = p.name,
                    ward = p.ward, location = e.zoneName, type = "camera", severity = "danger",
                    message = e.message, createdAt = e.at,
                ), alerts, escalations, now)
            }
        }
        alerts.sortByDescending { it.createdAt }
        escalations.sortByDescending { it.dispatchedAt }
        return (alerts.take(15) to escalations.take(10))
    }

    private fun pushDanger(alert: Alert, alerts: MutableList<Alert>, escalations: MutableList<Escalation>, now: Long) {
        if (!rateLimitCheck(alert.patientId, now)) return
        alerts.add(alert)
        val to = if (alert.ward != null) "Nurse station + on-duty nurse" else "Family caregiver"
        escalations.add(Escalation(
            id = "ESC-" + alert.id, alertId = alert.id, patientId = alert.patientId,
            patientName = alert.patientName, severity = "danger",
            channels = listOf("SMS + Emergency alert"), to = to,
            dispatchedAt = alert.createdAt + 2000, delivered = true,
            simulationNotice = "Simulated delivery. In production: caregiver SMS/WhatsApp, email, and emergency-services dispatch via live APIs.",
        ))
    }

    private fun stateAt(dial: Long, ans: Long, answered: Boolean, el: Long): String = when {
        el < dial -> "pending"
        el < ans -> "dialing"
        answered -> "answered"
        else -> "unanswered"
    }

    fun callForAlert(alert: Alert, now: Long): CallChain {
        val c = CONTACTS[alert.patientId] ?: Contact("Family caregiver", "Backup contact")
        val el = now - alert.createdAt
        val aFam = hash01(alert.id + ":fam") < 0.55
        val aBak = hash01(alert.id + ":bak") < 0.45
        val aEm = hash01(alert.id + ":em") < 0.9
        val fam = stateAt(0, 4200, aFam, el)
        val bak = if (!aFam) stateAt(5500, 9800, aBak, el) else "pending"
        val em = if (!aFam && !aBak) stateAt(11000, 13200, aEm, el) else "pending"

        val status = when {
            em == "answered" -> "dispatched"
            fam == "answered" || bak == "answered" -> "answered"
            fam == "dialing" || bak == "dialing" || em == "dialing" -> "dialing"
            el >= 5500 && !aFam && (bak == "pending" || em == "pending") -> "escalating"
            else -> "complete"
        }

        val log = mutableListOf<Pair<Long, String>>()
        fun push(tt: Long, msg: String) { if (el >= tt) log.add(Pair(alert.createdAt + tt, msg)) }
        push(0, "Danger alert ${alert.id} — automatic call chain started immediately")
        push(0, "Calling family → ${c.family} (voice)")
        if (fam == "answered") {
            push(4200, "family answered on ${c.family} — vitals + alert shared live")
        } else if (el >= 4200) {
            push(4200, "family did not answer after 2 attempts — escalating to next contact NOW")
            push(5500, "Calling backup → ${c.backup} (voice)")
            if (bak == "answered") {
                push(9800, "backup answered on ${c.backup} — vitals + alert shared live")
            } else if (el >= 9800) {
                push(9800, "backup did not answer after 2 attempts — escalating to emergency services NOW")
                push(11000, "Calling emergency services → 108 / 112 (voice-dispatch)")
                if (em == "answered") {
                    push(13200, "Emergency services reached on 108 / 112 — ambulance dispatched, GPS + vitals sent")
                } else if (el >= 15000) {
                    push(15000, "Emergency line busy — on-duty staff + hospital alerted directly")
                }
            }
        }
        log.sortBy { it.first }
        return CallChain(
            id = "CAL-" + alert.id.removePrefix("ALT-"), alertId = alert.id,
            patientId = alert.patientId, patientName = alert.patientName,
            status = status,
            ladder = listOf(
                CallLadder("family", c.family, fam, emergency = false),
                CallLadder("backup", c.backup, bak, emergency = false),
                CallLadder("emergency", "108 / 112", em, emergency = true),
            ),
            log = log,
        )
    }

    fun callsFor(patients: List<Patient>, now: Long): List<CallChain> {
        val ids = patients.map { it.id }.toSet()
        val alerts = alertsFor(patients, now).first
        return alerts.take(3).map { callForAlert(it, now) }.filter { it.patientId in ids }
    }
}