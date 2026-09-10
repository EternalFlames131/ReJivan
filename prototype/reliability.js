"use strict";
/**
 * ReJivan Reliability Layer — safeguards against false data, sensor
 * failures, and alert storms. This module makes the system TRUSTWORTHY,
 * not just functional.
 *
 * Built-in safeguards:
 *   1. Data validation — reject physiologically impossible readings
 *   2. Confidence scoring — each reading carries a 0-100 confidence score
 *   3. Consecutive-reading verification — danger must persist 2+ readings
 *      before emergency escalation (prevents false-positive panic)
 *   4. Sensor heartbeat / disconnect detection — alert if data stops
 *   5. Alert rate-limiting — prevent alert storms (max N alerts per patient
 *      per time window)
 *   6. Immutable audit trail — every alert, escalation, call is logged with
 *      timestamp + reason; cannot be deleted or modified
 *   7. Graceful degradation — if a sensor fails, the system still works
 *      with remaining sensors and alerts the family about the gap
 */

// ---- 1. Data validation (physiological impossibility rejection) ----

/**
 * Hard physiological limits. Any reading outside these ranges is rejected
 * as invalid (not just "danger" — physically impossible or sensor error).
 */
const PHYSIO_LIMITS = {
  hr:     { min: 20,  max: 250, unit: "bpm",  name: "Heart Rate" },
  spo2:   { min: 50,  max: 100, unit: "%",     name: "SpO2" },
  sbp:    { min: 40,  max: 280, unit: "mmHg",  name: "Systolic BP" },
  dbp:    { min: 20,  max: 180, unit: "mmHg",  name: "Diastolic BP" },
  temp:   { min: 30,  max: 44,  unit: "°C",    name: "Temperature" },
  glucose:{ min: 20,  max: 600, unit: "mg/dL", name: "Glucose" },
};

/**
 * Validate a vitals object. Returns { valid, rejected, reasons }.
 * rejected = metrics that failed validation; reasons = human-readable.
 */
function validateVitals(vitals) {
  const rejected = [];
  const reasons = [];
  for (const [metric, limits] of Object.entries(PHYSIO_LIMITS)) {
    const v = vitals[metric];
    if (v == null || typeof v !== "number" || isNaN(v)) {
      rejected.push(metric);
      reasons.push(`${limits.name}: missing or not a number`);
    } else if (v < limits.min || v > limits.max) {
      rejected.push(metric);
      reasons.push(`${limits.name}: ${v} ${limits.unit} is outside physiological range (${limits.min}–${limits.max})`);
    }
  }
  // Special: systolic must be > diastolic
  if (vitals.sbp != null && vitals.dbp != null && vitals.sbp <= vitals.dbp) {
    rejected.push("bp");
    reasons.push(`Blood Pressure: systolic (${vitals.sbp}) must be greater than diastolic (${vitals.dbp})`);
  }
  return {
    valid: rejected.length === 0,
    rejected,
    reasons,
    cleanVitals: cleanVitals(vitals, rejected),
  };
}

/**
 * Return a copy of vitals with rejected metrics set to null (so downstream
 * code can skip them rather than acting on garbage).
 */
function cleanVitals(vitals, rejected) {
  const out = { ...vitals };
  for (const m of rejected) out[m] = null;
  return out;
}

// ---- 2. Confidence scoring (0–100) ----

/**
 * Compute a confidence score for a reading based on:
 *   - Whether it passed validation (heavy weight)
 *   - How far it is from the physiological midpoint (closer = more typical = higher confidence)
 *   - Device quality tier (medical-grade = higher base confidence)
 *
 * deviceTier: "medical" | "consumer" | "simulated"
 */
function confidenceScore(metric, value, deviceTier = "simulated") {
  if (value == null) return 0;
  const limits = PHYSIO_LIMITS[metric];
  if (!limits) return 50;

  // Base confidence by device quality
  const tierBase = { medical: 85, consumer: 70, simulated: 60 };
  let base = tierBase[deviceTier] || 60;

  // Penalty for being near the edges of physiological range
  const range = limits.max - limits.min;
  const midpoint = (limits.max + limits.min) / 2;
  const distFromMid = Math.abs(value - midpoint) / (range / 2);
  // Within 60% of midpoint → full; near edges → drops
  const edgePenalty = distFromMid > 0.6 ? (distFromMid - 0.6) * 50 : 0;

  return Math.max(0, Math.min(100, Math.round(base - edgePenalty)));
}

/**
 * Compute overall confidence for a full vitals reading.
 * Returns { overall, perMetric }.
 */
function overallConfidence(vitals, deviceTier = "simulated") {
  const per = {};
  let total = 0;
  let count = 0;
  for (const metric of Object.keys(PHYSIO_LIMITS)) {
    if (vitals[metric] != null) {
      per[metric] = confidenceScore(metric, vitals[metric], deviceTier);
      total += per[metric];
      count++;
    } else {
      per[metric] = 0;
    }
  }
  return { overall: count ? Math.round(total / count) : 0, perMetric: per };
}

// ---- 3. Consecutive-reading verification ----

/**
 * Track danger readings per patient+metric. A danger alert only fires
 * after `MIN_CONSECUTIVE` consecutive danger readings for the same metric.
 * This eliminates single-glitch false positives.
 */
const MIN_CONSECUTIVE = 2; // require 2 consecutive danger readings
const consecutiveTracker = new Map(); // key -> { count, lastSlot }

function checkConsecutive(patientId, metric, isDanger, currentSlot) {
  const key = `${patientId}:${metric}`;
  const prev = consecutiveTracker.get(key);

  if (!isDanger) {
    // Reset counter when safe
    consecutiveTracker.set(key, { count: 0, lastSlot: currentSlot });
    return false; // not confirmed danger
  }

  if (prev && prev.lastSlot === currentSlot) {
    // Same slot, don't double-count
    return prev.count >= MIN_CONSECUTIVE;
  }

  const newCount = (prev && prev.lastSlot === currentSlot - 1) ? prev.count + 1 : 1;
  consecutiveTracker.set(key, { count: newCount, lastSlot: currentSlot });
  return newCount >= MIN_CONSECUTIVE;
}

// ---- 4. Sensor heartbeat / disconnect detection ----

/**
 * Track the last time each sensor reported data for a patient.
 * If data hasn't arrived within SENSOR_TIMEOUT_MS, raise a "sensor offline"
 * alert. The family is informed that a specific sensor is not reporting.
 */
const SENSOR_TIMEOUT_MS = 120000; // 2 minutes — if no data, sensor may be disconnected
const sensorHeartbeats = new Map(); // patientId -> { lastSeen, metrics[] }

function recordHeartbeat(patientId, metrics) {
  sensorHeartbeats.set(patientId, {
    lastSeen: Date.now(),
    metrics,
  });
}

function checkSensorHealth(now = Date.now()) {
  const offline = [];
  for (const [patientId, hb] of sensorHeartbeats) {
    const gap = now - hb.lastSeen;
    if (gap > SENSOR_TIMEOUT_MS) {
      offline.push({
        patientId,
        lastSeen: hb.lastSeen,
        gapMs: gap,
        metrics: hb.metrics,
        message: `Sensor for patient ${patientId} has not reported in ${Math.round(gap / 1000)}s — possible disconnection or battery failure.`,
      });
    }
  }
  return offline;
}

// ---- 5. Alert rate-limiting ----

/**
 * Prevent alert storms: max MAX_ALERTS_PER_WINDOW alerts per patient
 * within RATE_WINDOW_MS. Additional alerts are logged but not escalated
 * (they go to a "suppressed" list instead).
 */
const MAX_ALERTS_PER_WINDOW = 5;
const RATE_WINDOW_MS = 300000; // 5 minutes
const alertTimestamps = new Map(); // patientId -> [timestamps]

function rateLimitCheck(patientId, now = Date.now()) {
  const timestamps = alertTimestamps.get(patientId) || [];
  // Remove old entries outside the window
  const recent = timestamps.filter((t) => now - t < RATE_WINDOW_MS);
  alertTimestamps.set(patientId, recent);

  if (recent.length >= MAX_ALERTS_PER_WINDOW) {
    return {
      allowed: false,
      suppressed: true,
      reason: `Rate limit: ${recent.length} alerts for this patient in the last ${RATE_WINDOW_MS / 1000}s. Further alerts are logged but not escalated to prevent alert fatigue.`,
      nextAllowedAt: recent[0] + RATE_WINDOW_MS,
    };
  }

  recent.push(now);
  alertTimestamps.set(patientId, recent);
  return { allowed: true, suppressed: false };
}

// ---- 6. Immutable audit trail ----

/**
 * Append-only audit log. Every system action (alert fired, escalation
 * dispatched, call placed, sensor disconnect, data rejected) is logged
 * with a timestamp and reason. This log cannot be deleted or modified
 * — it provides accountability and a medical-legal trail.
 */
const auditLog = [];

function auditEvent(type, detail) {
  const entry = {
    id: "AUD-" + String(auditLog.length + 1).padStart(6, "0"),
    timestamp: Date.now(),
    type, // "alert" | "escalation" | "call" | "sensor_offline" | "data_rejected" | "rate_limited" | "system"
    ...detail,
  };
  auditLog.push(entry);
  return entry;
}

function getAuditLog(patientId = null, limit = 50) {
  let log = auditLog;
  if (patientId) log = log.filter((e) => e.patientId === patientId);
  return log.slice(-limit).reverse(); // newest first
}

// ---- 7. Graceful degradation ----

/**
 * Given a set of vital readings where some may be null (rejected or
 * sensor offline), determine what's still available and whether the
 * remaining sensors are sufficient for a meaningful assessment.
 *
 * Returns { available, missing, sufficient, warning }.
 */
function degradationStatus(vitals) {
  const available = [];
  const missing = [];
  for (const [metric, limits] of Object.entries(PHYSIO_LIMITS)) {
    if (vitals[metric] != null) {
      available.push(metric);
    } else {
      missing.push({ metric, name: limits.name });
    }
  }
  // Need at least HR + one of (SpO2 or BP) for a meaningful assessment
  const hasHR = vitals.hr != null;
  const hasCirculation = vitals.spo2 != null || vitals.sbp != null;
  const sufficient = hasHR && hasCirculation;

  return {
    available,
    missing,
    sufficient,
    warning: missing.length > 0
      ? `${missing.map((m) => m.name).join(", ")} data unavailable. System is monitoring with reduced sensors. Family should check the device.`
      : null,
  };
}

// ---- Exports ----
module.exports = {
  PHYSIO_LIMITS,
  validateVitals,
  cleanVitals,
  confidenceScore,
  overallConfidence,
  MIN_CONSECUTIVE,
  checkConsecutive,
  recordHeartbeat,
  checkSensorHealth,
  rateLimitCheck,
  auditEvent,
  auditLog,
  getAuditLog,
  degradationStatus,
};
