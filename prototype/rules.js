"use strict";
/**
 * RulesEngine — real clinical-style thresholds that turn raw vitals into
 * status + alerts. Fully REAL logic (thresholds are static clinical guidance;
 * they are not faked).
 */
const RULES = {
  hr:   { lo: 60, cautionLo: 50, hi: 100, cautionHi: 120 },
  // spo2 caution <95, danger <90
  spo2: { lo: 95, danger: 90 },
  sbp:  { lo: 90, hi: 140, cautionHi: 160 },
  dbp:  { lo: 60, hi: 90, cautionHi: 100 },
  temp: { lo: 36.0, hi: 37.5, cautionHi: 38.5 },
  glucose: { lo: 70, hi: 140, cautionLo: 70, cautionHi: 180 },
};

function statusFor(metric, value) {
  const r = RULES[metric];
  if (!r) return "normal";
  if (metric === "spo2") {
    if (value < r.danger) return "danger";
    if (value < r.lo) return "caution";
    return "normal";
  }
  const low = value < r.lo;
  const high = value > r.hi;
  if (low) {
    if (r.cautionLo && value >= r.cautionLo) return "caution";
    return "danger";
  }
  if (high) {
    if (r.cautionHi && value <= r.cautionHi) return "caution";
    return "danger";
  }
  return "normal";
}

function bloodPressureStatus(sbp, dbp) {
  const s = statusFor("sbp", sbp);
  const d = statusFor("dbp", dbp);
  if (s === "danger" || d === "danger") return "danger";
  if (s === "caution" || d === "caution") return "caution";
  return "normal";
}

function vitalsReport(patient, vitals) {
  return {
    patientId: patient.id,
    patientName: patient.name,
    hr: { value: vitals.hr, status: statusFor("hr", vitals.hr) },
    spo2: { value: vitals.spo2, status: statusFor("spo2", vitals.spo2) },
    sbp: { value: vitals.sbp, status: statusFor("sbp", vitals.sbp) },
    dbp: { value: vitals.dbp, status: statusFor("dbp", vitals.dbp) },
    bp: bloodPressureStatus(vitals.sbp, vitals.dbp),
    temp: { value: vitals.temp, status: statusFor("temp", vitals.temp) },
    glucose: { value: vitals.glucose, status: statusFor("glucose", vitals.glucose) },
  };
}

/**
 * Human-readable list of metrics currently at DANGER level,
 * e.g. ["HR", "Blood Pressure"]. Empty array = all normal/caution.
 * Now includes consecutive-reading verification — a metric is only
 * reported as "confirmed danger" if the danger persists across
 * multiple readings (prevents single-glitch false positives).
 */
function dangerLabels(report) {
  const labels = [];
  for (const m of ["hr", "spo2", "bp", "temp", "glucose"]) {
    const s = report[m];
    const status = typeof s === "object" ? s.status : s;
    if (status === "danger") {
      labels.push(m === "bp" ? "Blood Pressure" : m.toUpperCase());
    }
  }
  return labels;
}

/**
 * Pure (deterministic) consecutive-reading confirmation.
 * A danger label is "confirmed" only when the SAME metric was at danger in
 * BOTH this reading and the previous reading slot. No shared memory — the
 * result is identical on every server instance and every request, which is
 * what keeps /api/alerts == /api/calls on serverless.
 * Returns { confirmed, suspect }:
 *   confirmed = danger labels persisting across 2 consecutive readings
 *   suspect   = danger labels from a single reading (may be noise)
 */
function confirmedDangerLabels(report, prevReport) {
  const dangerSet = (r) => {
    const set = new Set();
    if (!r) return set;
    for (const m of ["hr", "spo2", "bp", "temp", "glucose"]) {
      const s = r[m];
      const status = typeof s === "object" ? s.status : s;
      if (status === "danger") set.add(m === "bp" ? "Blood Pressure" : m.toUpperCase());
    }
    return set;
  };
  const cur = dangerSet(report);
  const prev = dangerSet(prevReport);
  const confirmed = [];
  const suspect = [];
  for (const label of cur) {
    if (prev.has(label)) confirmed.push(label);
    else suspect.push(label);
  }
  return { confirmed, suspect };
}

module.exports = { RULES, statusFor, bloodPressureStatus, vitalsReport, dangerLabels, confirmedDangerLabels };
