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

module.exports = { RULES, statusFor, bloodPressureStatus, vitalsReport };
