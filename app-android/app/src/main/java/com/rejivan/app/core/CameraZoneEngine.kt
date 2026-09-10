package com.rejivan.app.core

import kotlin.math.sin

// Ported 1:1 from prototype/camerazone.js — SIMULATED privacy-first camera events.
object CameraZoneEngine {
    const val CAM_SLOT_MS = 150_000L

    private enum class Kind(val sev: String, val msg: (String) -> String) {
        fall("danger", { z -> "$z: possible fall detected - no movement response." }),
        out_of_bed("caution", { z -> "$z: out of bed at an unexpected hour." }),
        low_activity("caution", { z -> "$z: very low activity over 6 hours." }),
        no_activity_10min("caution", { z -> "$z: no movement for 10 minutes." })
    }

    data class CamEvent(
        val id: String, val zoneId: String, val patientId: String, val zoneName: String,
        val kind: String, val severity: String, val message: String, val at: Long
    )

    data class LiveFrame(val ts: Long, val motion: Double, val person: Boolean, val lighting: String)

    fun deriveCameraEvents(nowMs: Long = System.currentTimeMillis(), span: Int = 20): List<CamEvent> {
        val cur = (nowMs / CAM_SLOT_MS).toInt()
        val out = mutableListOf<CamEvent>()
        val keys = Kind.values()
        for (k in span downTo 0) {
            val s = cur - k
            val rh = Hash.hash01("cam:$s")
            if (rh < 0.32) {
                val zone = DemoData.CAMERA_ZONES[((Hash.hash01("camz:$s") * DemoData.CAMERA_ZONES.size).toInt())
                    .coerceIn(0, DemoData.CAMERA_ZONES.size - 1)]
                val kind = keys[((Hash.hash01("camk:$s") * keys.size).toInt()).coerceIn(0, keys.size - 1)]
                out.add(CamEvent(
                    "EVENT-CAM-$s", zone.id, zone.patientId, zone.name,
                    kind.name, kind.sev, kind.msg(zone.name), s.toLong() * CAM_SLOT_MS
                ))
            }
        }
        return out
    }

    fun zoneForPatient(patientId: String): CameraZone? = DemoData.CAMERA_ZONES.firstOrNull { it.patientId == patientId }

    fun liveFrame(zoneId: String, nowMs: Long = System.currentTimeMillis()): LiveFrame {
        val t = nowMs / 1000.0
        val ph1 = Hash.hash01("$zoneId:m") * Math.PI * 2
        val ph2 = Hash.hash01("$zoneId:p") * Math.PI * 2
        val seq = (t / 2).toInt()
        val jit = (Hash.hash01("$zoneId:j:$seq") - 0.5) * 0.2
        val motion = (0.45 + 0.25 * sin(t * 0.7 + ph1) + jit).coerceIn(0.0, 1.0)
        val person = zoneId.startsWith("BED") || sin(t * 0.13 + ph2) > -0.25
        val lighting = if (Hash.hash01("$zoneId:day:${(t / 900).toInt()}") < 0.12) "night" else "day"
        return LiveFrame(nowMs, motion, person, lighting)
    }
}
