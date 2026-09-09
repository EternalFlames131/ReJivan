"use strict";
/**
 * CameraZone simulator — SIMULATED room-event detection.
 * Real product: edge AI on a privacy-first camera (fall, out-of-bed,
 * low-activity). NO video recorded or stored — only events/alerts.
 */
const { random } = Math;

class CameraZoneSim {
  constructor(alertManager, dataDir) {
    this.alerts = alertManager;
    this.zones = [];
    this.events = [];
    this.nextEventAt = Date.now() + 60000; // first demo event ~60s in
    this._seedZones();
  }

  _seedZones() {
    // Home zone + Virtual Ward beds
    this.zones = [
      { id: "CAM1", patientId: "P1", name: "Home — Living Room", room: "Home" },
      { id: "CAM2", patientId: "P2", name: "Home — Bedroom", room: "Home" },
      { id: "BED1", patientId: "P3", name: "Ward A · Bed 1", room: "Ward A", ward: true },
      { id: "BED2", patientId: "P4", name: "Ward A · Bed 2", room: "Ward A", ward: true },
    ];
  }

  zonesList() {
    return this.zones;
  }

  tick(now = Date.now()) {
    if (now > this.nextEventAt) {
      const zone = this.zones[(random() * this.zones.length) | 0];
      const kinds = ["fall", "out_of_bed", "low_activity", "no_activity_10min"];
      const kind = kinds[(random() * kinds.length) | 0];
      this._emit(zone, kind, now);
      // next event 45–150s later
      this.nextEventAt = now + 45000 + random() * 105000;
    }
  }

  _emit(zone, kind, now) {
    const labels = {
      fall: { sev: "danger", msg: `${zone.name}: possible fall detected — no movement response.` },
      out_of_bed: { sev: "caution", msg: `${zone.name}: out of bed at an unexpected hour.` },
      low_activity: { sev: "caution", msg: `${zone.name}: very low activity over 6 hours.` },
      no_activity_10min: { sev: "caution", msg: `${zone.name}: no movement for 10 minutes.` },
    };
    const l = labels[kind];
    const event = {
      id: "CAM" + String(this.events.length + 1).padStart(3, "0"),
      zoneId: zone.id,
      patientId: zone.patientId,
      zoneName: zone.name,
      kind,
      severity: l.sev,
      message: l.msg,
      at: now,
    };
    this.events.unshift(event);
    if (this.alerts) {
      this.alerts.eventAlert(zone.patientId, zone.patientId, "camera", l.msg, l.sev);
    }
    return event;
  }

  eventsList() {
    return this.events;
  }
}

module.exports = { CameraZoneSim };
