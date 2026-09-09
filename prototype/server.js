"use strict";
/**
 * SanjivanAI — prototype server.
 * Express + JSON file storage. Serves the SPA from /public and exposes a REST API.
 */
const path = require("path");
const fs = require("fs");
const express = require("express");
const cors = require("cors");

const { Patient } = require("./simulator");
const { vitalsReport } = require("./rules");
const { AlertManager } = require("./alerts");
const { MedicationStore } = require("./medications");
const { CameraZoneSim } = require("./camerazone");

const DATA_DIR = path.join(__dirname, "data");
if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, "public")));

// ---- Seed demo patients ------------------------------------------------
const patients = [
  new Patient({ id: "P1", name: "Anita Sharma", age: 67, sex: "F", condition: "hypertension", location: "Home — Living Room" }),
  new Patient({ id: "P2", name: "Ram Prakash", age: 74, sex: "M", condition: "diabetes", location: "Home — Bedroom" }),
  new Patient({ id: "P3", name: "Meera Nair", age: 58, sex: "F", condition: "post-surgery", location: "Virtual Ward" }),
  new Patient({ id: "P4", name: "Kavitha Rao", age: 61, sex: "F", condition: "heart-arrhythmia", location: "Virtual Ward" }),
];
// Assign ward beds for the two hospital patients
patients[2].ward = "Ward A · Bed 1";
patients[3].ward = "Ward A · Bed 2";

const alerts = new AlertManager();
const meds = new MedicationStore(DATA_DIR);
const cams = new CameraZoneSim(alerts, DATA_DIR);

// Seed a few medications if empty
if (meds.list().length === 0) {
  meds.add({ patientId: "P1", name: "Amlodipine", dose: "5 mg", frequency: "daily", times: ["08:00", "20:00"], notes: "After food" });
  meds.add({ patientId: "P2", name: "Metformin", dose: "500 mg", frequency: "twice daily", times: ["09:00", "21:00"], notes: "With meals" });
  meds.add({ patientId: "P3", name: "Paracetamol", dose: "650 mg", frequency: "8 hourly", times: ["08:00", "16:00", "00:00"], notes: "For fever" });
}

// ---- Background simulation loop -----------------------------------------
setInterval(() => {
  for (const p of patients) {
    p.tick(1.0);
    const report = vitalsReport(p, p.vitals);
    alerts.evaluate(p, report);
  }
  cams.tick();
}, 2000);

// ---- API ----------------------------------------------------------------
app.get("/api/health", (req, res) => res.json({ ok: true, service: "SanjivanAI", time: Date.now() }));

app.get("/api/patients", (req, res) => {
  res.json({ patients: patients.map((p) => p.snapshot()) });
});

app.get("/api/vitals", (req, res) => {
  res.json({
    patients: patients.map((p) => ({
      ...p.snapshot(),
      report: vitalsReport(p, p.vitals),
    })),
  });
});

app.get("/api/alerts", (req, res) => {
  res.json({ alerts: alerts.list(), escalations: alerts.listEscalations() });
});

app.get("/api/medications", (req, res) => {
  res.json({ list: meds.list() });
});

app.post("/api/medications", (req, res) => {
  const m = meds.add(req.body);
  res.status(201).json(m);
});

app.post("/api/medications/:id/take", (req, res) => {
  const entry = meds.markTaken(req.params.id);
  res.json(entry);
});

app.delete("/api/medications/:id", (req, res) => {
  meds.remove(req.params.id);
  res.json({ ok: true });
});

app.get("/api/camera-zones", (req, res) => {
  res.json({ zones: cams.zonesList(), events: cams.eventsList() });
});

app.get("/api/escalations", (req, res) => {
  res.json({ escalations: alerts.listEscalations() });
});

app.get("/api/simulation/status", (req, res) => {
  res.json({
    simulated: ["vitals", "camera-events", "billing", "SMS/WhatsApp delivery"],
    real: ["dashboard", "medications", "rules engine", "alerts", "escalation", "multilingual UI"],
    disclaimer: "Prototype: SanjivanAI is not a certified medical device. Always involve a human caregiver/doctor for decisions.",
  });
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`SanjivanAI prototype running at http://localhost:${PORT}`);
});
