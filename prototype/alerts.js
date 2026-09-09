"use strict";
/**
 * Alerting & escalation.
 * - The DETECTION logic (danger rules) is real.
 * - The DELIVERY (SMS / email / WhatsApp / emergency services) is SIMULATED
 *   for the prototype. Hooks are stubbed so the real product can wire live
 *   APIs (Twilio, SendGrid/Mailgun, WhatsApp Business API).
 */
const { random } = Math;

class AlertManager {
  constructor() {
    this.alerts = [];
    this.escalations = [];
    this.seq = 0;
    // dedupe: don't spam the same patient+type while "active"
    this.active = new Map(); // key -> until ms
  }

  _key(patientId, type) {
    return `${patientId}:${type}`;
  }

  /**
   * Evaluate a patient's vitals report against danger thresholds.
   * Returns true if any danger-level condition was found.
   */
  evaluate(patient, report) {
    const dangers = [];
    for (const m of ["hr", "spo2", "bp", "temp", "glucose"]) {
      const s = report[m];
      const status = typeof s === "object" ? s.status : s;
      if (status === "danger") {
        const label = m === "bp" ? "Blood Pressure" : m.toUpperCase();
        dangers.push(label);
      }
    }
    if (dangers.length === 0) return null;

    const key = this._key(patient.id, dangers.join("+"));
    const now = Date.now();
    if (this.active.has(key) && this.active.get(key) > now) {
      return null; // already alerted, cooldown
    }
    this.active.set(key, now + 60000); // 60s cooldown per patient+type

    const alert = {
      id: "ALT" + String(++this.seq).padStart(3, "0"),
      patientId: patient.id,
      patientName: patient.name,
      age: patient.age,
      ward: patient.ward || patient.location,
      type: "vitals",
      severity: "danger",
      message: `${patient.name} — DANGER: ${dangers.join(", ")} out of normal range. ${
        patient.ward ? "Virtual Ward " + patient.ward : "At home"
      }. Automated check ${new Date().toLocaleTimeString()}.`,
      vitals: report,
      createdAt: now,
    };
    this.alerts.unshift(alert);
    this._escalate(alert);
    return alert;
  }

  /**
   * Log a non-vital event alert (e.g. from a camera zone).
   */
  eventAlert(patientId, patientName, type, message, severity = "caution") {
    const now = Date.now();
    const key = this._key(patientId, type + message.split(" ")[0]);
    if (this.active.has(key) && this.active.get(key) > now) return null;
    this.active.set(key, now + 45000);

    const alert = {
      id: "EVT" + String(++this.seq).padStart(3, "0"),
      patientId,
      patientName,
      type,
      severity,
      message,
      createdAt: now,
    };
    this.alerts.unshift(alert);
    if (severity === "danger") this._escalate(alert);
    return alert;
  }

  _escalate(alert) {
    // SIMULATED delivery channels (stubbed for real APIs later).
    const channels = [alert.severity === "danger" ? "SMS + Emergency alert" : "SMS"];
    const escalation = {
      id: "ESC" + String(++this.seq).padStart(3, "0"),
      alertId: alert.id,
      patientName: alert.patientName,
      severity: alert.severity,
      channels, // real product: Twilio / WhatsApp Business / email + ambulance API
      to: alert.ward ? "Nurse station + on-duty nurse" : "Family caregiver",
      dispatchedAt: Date.now(),
      delivered: true,
      simulationNotice:
        "Simulated delivery. In production: caregiver SMS/WhatsApp, email, and emergency-services dispatch via live APIs.",
    };
    this.escalations.unshift(escalation);
    return escalation;
  }

  list() {
    return this.alerts;
  }

  listEscalations() {
    return this.escalations;
  }
}

module.exports = { AlertManager };
