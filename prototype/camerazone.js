"use strict";
/**
 * CameraZone — deterministic, serverless-ready SIMULATED room-event detection.
 * Real product: edge intelligence on a privacy-first camera (fall, out-of-bed,
 * low-activity). NO video recorded or stored — only events/alerts.
 *
 * Serverless design: events and live-preview frames are pure, deterministic
 * functions of (zone, wall-clock time) — no background loop, same answer on
 * any instance.
 */
const crypto = require("crypto");

function hash01(seed) {
  return parseInt(crypto.createHash("md5").update(String(seed)).digest("hex").slice(0, 8), 16) / 0xffffffff;
}

function clamp(v, lo, hi) {
  return Math.max(lo, Math.min(hi, v));
}

const CAM_SLOT_MS = 150000; // 150 s event-scheduling slot

const ZONES = [
  { id: "CAM1", patientId: "P1", name: "Home — Living Room", room: "Home · Junglighat, Port Blair" },
  { id: "CAM2", patientId: "P2", name: "Home — Bedroom", room: "Home · Hut Bay, Little Andaman" },
  { id: "BED1", patientId: "P3", name: "GB Pant Virtual Ward · Bed 1", room: "GB Pant Hospital, Port Blair", ward: true },
  { id: "BED2", patientId: "P4", name: "GB Pant Virtual Ward · Bed 2", room: "GB Pant Hospital, Port Blair", ward: true },
];

const KINDS = {
  fall: { sev: "danger", msg: (z) => `${z.name}: possible fall detected — no movement response.` },
  out_of_bed: { sev: "caution", msg: (z) => `${z.name}: out of bed at an unexpected hour.` },
  low_activity: { sev: "caution", msg: (z) => `${z.name}: very low activity over 6 hours.` },
  no_activity_10min: { sev: "caution", msg: (z) => `${z.name}: no movement for 10 minutes.` },
};

/**
 * Deterministic camera events for the most recent `span` slots,
 * newest first.
 */
function deriveCameraEvents(nowMs = Date.now(), span = 20) {
  const cur = Math.floor(nowMs / CAM_SLOT_MS);
  const out = [];
  const keys = Object.keys(KINDS);
  for (let k = span; k >= 0; k--) {
    const s = cur - k;
    const rh = hash01("cam:" + s);
    if (rh < 0.32) {
      const zone = ZONES[Math.floor(hash01("camz:" + s) * ZONES.length)];
      const kind = keys[Math.floor(hash01("camk:" + s) * keys.length)];
      const l = KINDS[kind];
      out.push({
        id: "EVENT-CAM-" + s,
        zoneId: zone.id,
        patientId: zone.patientId,
        zoneName: zone.name,
        kind,
        severity: l.sev,
        message: l.msg(zone),
        at: s * CAM_SLOT_MS,
      });
    }
  }
  return out;
}

/**
 * SIMULATED live-preview frame descriptor (privacy-safe metadata only).
 * Real product: on-device edge intelligence returns only scene metadata — never video.
 */
function liveFrame(zoneId, nowMs = Date.now()) {
  const t = nowMs / 1000;
  const ph1 = hash01(zoneId + ":m") * Math.PI * 2;
  const ph2 = hash01(zoneId + ":p") * Math.PI * 2;
  const seq = Math.floor(t / 2);
  const jit = (hash01(zoneId + ":j:" + seq) - 0.5) * 0.2;
  return {
    ts: nowMs,
    motion: clamp(0.45 + 0.25 * Math.sin(t * 0.7 + ph1) + jit, 0, 1),
    person: zoneId.startsWith("BED") || Math.sin(t * 0.13 + ph2) > -0.25,
    lighting: hash01(zoneId + ":day:" + Math.floor(t / 900)) < 0.12 ? "night" : "day",
  };
}

module.exports = { cameraZones: ZONES, deriveCameraEvents, liveFrame, CAM_SLOT_MS, hash01 };