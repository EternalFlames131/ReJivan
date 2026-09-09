"use strict";
/**
 * VitalSim — deterministic, serverless-ready simulated vital-signs engine.
 *
 * REAL vs SIMULATED (prototype honesty, required by project spec):
 *   - The clock-driven data this module produces is SIMULATED (in the real
 *     product it comes from a wearable / At-Home Monitor plus contact-free
 *     camera sensing).
 *   - Everything downstream — dashboard, rules engine, alerts and escalation —
 *     is REAL logic.
 *
 * Serverless design (Vercel-compatible):
 *   there is NO background tick loop and NO mutable state. Vitals are a pure,
 *   deterministic function of (patient, wall-clock time), so every request
 *   gets the same answer on any server instance. The demo behaves identically
 *   on a laptop and in the cloud, with no long-running process required.
 */
const crypto = require("crypto");

function hash01(seed) {
  return parseInt(crypto.createHash("md5").update(String(seed)).digest("hex").slice(0, 8), 16) / 0xffffffff;
}

const SLOT_MS = 200000; // 200 s episode-scheduling slot

function clamp(v, lo, hi) {
  return Math.max(lo, Math.min(hi, v));
}
function round1(v) {
  return Math.round(v * 10) / 10;
}

// Baseline profiles
const BASELINES = {
  healthy:        { hr: 74,  spo2: 98, sbp: 120, dbp: 80, temp: 36.8, glucose: 100 },
  "heart-arrhythmia": { hr: 88, spo2: 96, sbp: 128, dbp: 82, temp: 36.9, glucose: 104 },
  "post-surgery": { hr: 80,  spo2: 97, sbp: 118, dbp: 76, temp: 37.0, glucose: 110 },
  diabetes:       { hr: 78,  spo2: 97, sbp: 130, dbp: 85, temp: 36.7, glucose: 150 },
  hypertension:   { hr: 80,  spo2: 97, sbp: 150, dbp: 96, temp: 36.8, glucose: 108 },
  elderly:        { hr: 76,  spo2: 96, sbp: 135, dbp: 82, temp: 36.5, glucose: 105 },
};

// Episodes map: per condition, named events → target danger values
const EPISODES = {
  healthy: {
    "tachycardia spike": { hr: 168 },
    "brief desaturation": { spo2: 88 },
    "stress rise": { sbp: 152, dbp: 98 },
  },
  "heart-arrhythmia": {
    "arrhythmia episode": { hr: 172, sbp: 140 },
    "rapid HR": { hr: 182 },
  },
  "post-surgery": {
    "fever spike": { temp: 39.1 },
    "desaturation": { spo2: 86 },
  },
  diabetes: {
    "hypoglycaemia": { glucose: 62 },
    "hyperglycaemia": { glucose: 255 },
  },
  hypertension: {
    "hypertensive crisis": { sbp: 178, dbp: 112 },
  },
  elderly: {
    "bradycardia": { hr: 46 },
    "desaturation": { spo2: 87 },
    "fall-risk episode": { sbp: 146 },
  },
};

// Per-metric wandering (smooth sines) + small jitter so values look alive.
const NOISE = {
  hr:      { a1: 6,   a2: 2.5, p1: 21, p2: 6,  j: 1.5, lo: 40, hi: 200, dp: 0 },
  spo2:    { a1: 0.7, a2: 0.3, p1: 25, p2: 8,  j: 0.3, lo: 70, hi: 100, dp: 1 },
  sbp:     { a1: 5,   a2: 2,   p1: 31, p2: 9,  j: 2,   lo: 70, hi: 210, dp: 0 },
  dbp:     { a1: 3.5, a2: 1.5, p1: 31, p2: 9,  j: 1.5, lo: 40, hi: 140, dp: 0 },
  temp:    { a1: 0.15, a2: 0.06, p1: 47, p2: 15, j: 0.05, lo: 34, hi: 42, dp: 1 },
  glucose: { a1: 8,   a2: 4,   p1: 41, p2: 13, j: 4,   lo: 60, hi: 300, dp: 0 },
};

/**
 * Deterministic vitals for a patient spec at time nowMs.
 * Returns { vitals, episode, slot, at }.
 *   vitals:   { hr, spo2, sbp, dbp, temp, glucose, sys }
 *   episode:  { type, from, until } | null  — danger episode active in this slot
 */
function generateVitals(spec, nowMs) {
  const t = nowMs / 1000;
  const base = BASELINES[spec.condition] || BASELINES.healthy;
  const slot = Math.floor(t / (SLOT_MS / 1000));

  // Deterministic episode decision for this slot.
  const rh = hash01(spec.id + ":ep:" + slot);
  const types = Object.keys(EPISODES[spec.condition] || EPISODES.healthy);
  let targets = base;
  let episode = null;
if (rh < 0.5) {
      const type = types[Math.min(types.length - 1, Math.floor((rh / 0.5) * types.length))];
      const ep = (EPISODES[spec.condition] || EPISODES.healthy)[type];
      episode = { type, from: slot * SLOT_MS, until: (slot + 1) * SLOT_MS };
      // Smooth rise → peak → fall across the whole slot (peak in the middle).
      const frac = (t % (SLOT_MS / 1000)) / (SLOT_MS / 1000);
      const ramp = Math.sin(Math.PI * clamp(frac, 0, 1));
      const mixed = {};
      for (const m of Object.keys(base)) {
        mixed[m] = base[m] + (ep[m] != null ? ep[m] - base[m] : 0) * ramp;
      }
      targets = mixed;
    }

  const vitals = {};
  for (const m of Object.keys(NOISE)) {
    const n = NOISE[m];
    const ph1 = hash01(spec.id + ":ph:" + m + ":a") * Math.PI * 2;
    const ph2 = hash01(spec.id + ":ph:" + m + ":b") * Math.PI * 2;
    const seq = Math.floor(t / 2);
    const jitter = (hash01(spec.id + ":j:" + m + ":" + seq) - 0.5) * 2 * n.j;
    const wander =
      n.a1 * Math.sin((t / n.p1) * Math.PI * 2 + ph1) +
      n.a2 * Math.sin((t / n.p2) * Math.PI * 2 + ph2);
    const v = clamp(targets[m] + wander + jitter, n.lo, n.hi);
    vitals[m] = n.dp ? round1(v) : Math.round(v);
  }
  vitals.sys = vitals.sbp;

  return { vitals, episode, slot, at: Date.now() };
}

module.exports = { BASELINES, EPISODES, hash01, SLOT_MS, generateVitals };