"use strict";
/**
 * Medications — real scheduling & tracking logic (records are persisted to
 * data/medications.json). Reminders are real; delivery of the reminder
 * (SMS/notification) would use live APIs in production.
 */
const fs = require("fs");
const path = require("path");

class MedicationStore {
  constructor(dataDir) {
    this.file = path.join(dataDir, "medications.json");
    this.data = { list: [], log: [] };
    this._load();
  }

  _load() {
    try {
      if (fs.existsSync(this.file)) {
        const raw = fs.readFileSync(this.file, "utf8");
        this.data = JSON.parse(raw);
      }
    } catch (e) {
      console.error("medications load error", e.message);
    }
  }

  _save() {
    try {
      fs.writeFileSync(this.file, JSON.stringify(this.data, null, 2), "utf8");
    } catch (e) {
      // Cloud read-only fs — medical schedule still works in memory.
    }
  }

  list() {
    return this.data.list;
  }

  add(med) {
    const item = {
      id: "MED" + String(this.data.list.length + 1).padStart(3, "0"),
      patientId: med.patientId,
      name: med.name,
      dose: med.dose,
      frequency: med.frequency || "daily",
      times: med.times || ["08:00", "20:00"],
      notes: med.notes || "",
      active: true,
    };
    this.data.list.push(item);
    this._save();
    return item;
  }

  remove(id) {
    this.data.list = this.data.list.filter((m) => m.id !== id);
    this._save();
  }

  markTaken(medId, time = new Date().toISOString()) {
    this.data.log.push({ medId, time });
    this._save();
    return this.data.log[this.data.log.length - 1];
  }

  dueList(now = new Date()) {
    const hm = `${String(now.getHours()).padStart(2, "0")}:${String(
      now.getMinutes()
    ).padStart(2, "0")}`;
    return this.data.list.filter((m) => m.active && m.times.includes(hm));
  }
}

module.exports = { MedicationStore };
