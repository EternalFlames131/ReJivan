package com.rejivan.app.core

import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.sin

// Ported 1:1 from prototype/simulator.js (serverless-ready, deterministic).
// Same algorithm -> same vitals for the same (patient, wall-clock time) as the web app.

object Hash {
    fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash01(seed: String): Double {
        val hex = md5Hex(seed)
        val first8 = hex.substring(0, 8)
        return first8.toLong(16) / 4294967295.0
    }
}

data class VitalSet(
    val hr: Int, val spo2: Int, val sbp: Int, val dbp: Int,
    val temp: Double, val glucose: Int, val sys: Int
)

data class Episode(
    val type: String,
    val from: Long,
    val until: Long
)

data class VitalsResult(
    val vitals: VitalSet,
    val episode: Episode?,
    val slot: Int,
    val at: Long
)

object VitalSimulator {
    const val SLOT_MS = 200_000L

    val BASELINES = mapOf(
        "healthy" to doubleArrayOf(74.0, 98.0, 120.0, 80.0, 36.8, 100.0),
        "heart-arrhythmia" to doubleArrayOf(88.0, 96.0, 128.0, 82.0, 36.9, 104.0),
        "post-surgery" to doubleArrayOf(80.0, 97.0, 118.0, 76.0, 37.0, 110.0),
        "diabetes" to doubleArrayOf(78.0, 97.0, 130.0, 85.0, 36.7, 150.0),
        "hypertension" to doubleArrayOf(80.0, 97.0, 150.0, 96.0, 36.8, 108.0),
        "elderly" to doubleArrayOf(76.0, 96.0, 135.0, 82.0, 36.5, 105.0)
    )

    // metric order: hr, spo2, sbp, dbp, temp, glucose
    val EPISODES = mapOf(
        "healthy" to mapOf(
            "tachycardia spike" to doubleArrayOf(168.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            "brief desaturation" to doubleArrayOf(0.0, 88.0, 0.0, 0.0, 0.0, 0.0),
            "stress rise" to doubleArrayOf(0.0, 0.0, 152.0, 98.0, 0.0, 0.0)
        ),
        "heart-arrhythmia" to mapOf(
            "arrhythmia episode" to doubleArrayOf(172.0, 0.0, 140.0, 0.0, 0.0, 0.0),
            "rapid HR" to doubleArrayOf(182.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        ),
        "post-surgery" to mapOf(
            "fever spike" to doubleArrayOf(0.0, 0.0, 0.0, 0.0, 39.1, 0.0),
            "desaturation" to doubleArrayOf(0.0, 86.0, 0.0, 0.0, 0.0, 0.0)
        ),
        "diabetes" to mapOf(
            "hypoglycaemia" to doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 62.0),
            "hyperglycaemia" to doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 255.0)
        ),
        "hypertension" to mapOf(
            "hypertensive crisis" to doubleArrayOf(0.0, 0.0, 178.0, 112.0, 0.0, 0.0)
        ),
        "elderly" to mapOf(
            "bradycardia" to doubleArrayOf(46.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            "desaturation" to doubleArrayOf(0.0, 87.0, 0.0, 0.0, 0.0, 0.0),
            "fall-risk episode" to doubleArrayOf(0.0, 0.0, 146.0, 0.0, 0.0, 0.0)
        )
    )

    // noise: a1, a2, p1, p2, j, lo, hi, dp(decimals)
    data class Noise(val a1: Double, val a2: Double, val p1: Double, val p2: Double,
                     val j: Double, val lo: Double, val hi: Double, val dp: Int)

    private val NOISE = mapOf(
        "hr" to Noise(6.0, 2.5, 21.0, 6.0, 1.5, 40.0, 200.0, 0),
        "spo2" to Noise(0.7, 0.3, 25.0, 8.0, 0.3, 70.0, 100.0, 1),
        "sbp" to Noise(5.0, 2.0, 31.0, 9.0, 2.0, 70.0, 210.0, 0),
        "dbp" to Noise(3.5, 1.5, 31.0, 9.0, 1.5, 40.0, 140.0, 0),
        "temp" to Noise(0.15, 0.06, 47.0, 15.0, 0.05, 34.0, 42.0, 1),
        "glucose" to Noise(8.0, 4.0, 41.0, 13.0, 4.0, 60.0, 300.0, 0)
    )

    private val METRICS = listOf("hr", "spo2", "sbp", "dbp", "temp", "glucose")

    private fun clamp(v: Double, lo: Double, hi: Double) = maxOf(lo, minOf(hi, v))

    fun generateVitals(patientId: String, condition: String, nowMs: Long = System.currentTimeMillis()): VitalsResult {
        val t = nowMs / 1000.0
        val base = BASELINES[condition] ?: BASELINES["healthy"]!!
        val slot = (t / (SLOT_MS / 1000.0)).toInt()

        var targets = base
        var episode: Episode? = null
        val rh = Hash.hash01("$patientId:ep:$slot")
        val types = (EPISODES[condition] ?: EPISODES["healthy"]!!).keys.toList()
        if (rh < 0.5) {
            val idx = minOf(types.size - 1, ((rh / 0.5) * types.size).toInt())
            val type = types[idx]
            val ep = (EPISODES[condition] ?: EPISODES["healthy"]!!)[type]!!
            episode = Episode(type, slot.toLong() * SLOT_MS, (slot + 1).toLong() * SLOT_MS)
            val frac = (t % (SLOT_MS / 1000.0)) / (SLOT_MS / 1000.0)
            val ramp = sin(PI * clamp(frac, 0.0, 1.0))
            targets = DoubleArray(6) { i -> base[i] + (ep[i] - base[i]) * ramp }
        }

        val hr = noise(patientId, "hr", t, targets[0], slot) { it.toInt() }
        val spo2 = noise(patientId, "spo2", t, targets[1], slot) { it.toInt() }
        val sbp = noise(patientId, "sbp", t, targets[2], slot) { it.toInt() }
        val dbp = noise(patientId, "dbp", t, targets[3], slot) { it.toInt() }
        val temp = noise(patientId, "temp", t, targets[4], slot) { Math.round(it * 10.0) / 10.0 }
        val glucose = noise(patientId, "glucose", t, targets[5], slot) { it.toInt() }

        val vitals = VitalSet(hr, spo2, sbp, dbp, temp, glucose, sbp)
        return VitalsResult(vitals, episode, slot, System.currentTimeMillis())
    }

    private inline fun <T> noise(patientId: String, metric: String, t: Double,
                                 target: Double, slot: Int, convert: (Double) -> T): T {
        val n = NOISE[metric]!!
        val ph1 = Hash.hash01("$patientId:ph:$metric:a") * PI * 2
        val ph2 = Hash.hash01("$patientId:ph:$metric:b") * PI * 2
        val seq = (t / 2).toInt()
        val jitter = (Hash.hash01("$patientId:j:$metric:$seq") - 0.5) * 2 * n.j
        val wander = n.a1 * sin((t / n.p1) * PI * 2 + ph1) +
                n.a2 * sin((t / n.p2) * PI * 2 + ph2)
        val v = clamp(target + wander + jitter, n.lo, n.hi)
        return convert(v)
    }
}
