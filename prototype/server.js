"use strict";
/**
 * SanjivanAI — prototype server.
 * Express + JSON file storage. Serves the SPA from /public and exposes a REST API.
 *
 * Authentication: every data endpoint is behind a login. Each account (family
 * or Virtual Ward) sees ONLY the patients it registered. All passwords are
 * hashed (scrypt); sessions are tokens.
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
const { AuthStore } = require("./auth");

const DATA_DIR = path.join(__dirname, "data");
if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, "public")));

// ---- Seed users & patients ---------------------------------------------
const auth = new AuthStore(DATA_DIR);

const uSharma = auth.seedUser({ name: "Sharma Family", email: "asharma@demo.in", password: "demo123", role: "family" });
const uPrakash = auth.seedUser({ name: "Prakash Family", email: "rprakash@demo.in", password: "demo123", role: "family" });
const uWard = auth.seedUser({ name: "Ward Nurse Station", email: "wardnurse@demo.in", password: "demo123", role: "ward" });

const patients = [
  new Patient({ id: "P1", name: "Anita Sharma", age: 67, sex: "F", condition: "hypertension", location: "Home — Living Room" }),
  new Patient({ id: "P2", name: "Ram Prakash", age: 74, sex: "M", condition: "diabetes", location: "Home — Bedroom" }),
  new Patient({ id: "P3", name: "Meera Nair", age: 58, sex: "F", condition: "post-surgery", location: "Virtual Ward" }),
  new Patient({ id: "P4", name: "Kavitha Rao", age: 61, sex: "F", condition: "heart-arrhythmia", location: "Virtual Ward" }),
];
patients[0].userId = uSharma.id;
patients[1].userId = uPrakash.id;
patients[2].userId = uWard.id;
patients[2].ward = "Ward A · Bed 1";
patients[3].userId = uWard.id;
patients[3].ward = "Ward A · Bed 2";

const alerts = new AlertManager();
const meds = new MedicationStore(DATA_DIR);
const cams = new CameraZoneSim(alerts, DATA_DIR);

if (meds.list().length === 0) {
  meds.add({ patientId: "P1", name: "Amlodipine", dose: "5 mg", frequency: "daily", times: ["08:00", "20:00"], notes: "After food" });
  meds.add({ patientId: "P2", name: "Metformin", dose: "500 mg", frequency: "twice daily", times: ["09:00", "21:00"], notes: "With meals" });
  meds.add({ patientId: "P3", name: "Paracetamol", dose: "650 mg", frequency: "8 hourly", times: ["08:00", "16:00", "00:00"], notes: "For fever" });
}

// ---- Auth middleware ----------------------------------------------------
function requireAuth(req, res, next) {
  const token = String(req.headers.authorization || "").replace(/^Bearer\s+/i, "");
  const user = auth.userForToken(token);
  if (!user) return res.status(401).json({ error: "auth_required" });
  req.user = user;
  req.token = token;
  next();
}

function ownPatients(user) {
  return patients.filter((p) => p.userId === user.id);
}
function ownPatientIds(user) {
  return new Set(ownPatients(user).map((p) => p.id));
}
function patientName(id) {
  const p = patients.find((x) => x.id === id);
  return p ? p.name : id;
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

// ---- Auth API ------------------------------------------------------------
app.post("/api/auth/register", (req, res) => {
  const { name, email, password, role } = req.body || {};
  if (!name || !email || !password) return res.status(400).json({ error: "missing_fields" });
  if (String(password).length < 6) return res.status(400).json({ error: "weak_password" });
  const out = auth.register({ name, email, password, role });
  if (out.error) return res.status(409).json(out);
  const user2 = auth.findByEmail(email);
  // No patient is auto-created at registration in the prototype data — the
  // demo accounts below already own the seeded patients. New accounts can
  // register a patient via a future "Add patient" flow.
  res.status(201).json(out);
});

app.post("/api/auth/login", (req, res) => {
  const { email, password } = req.body || {};
  const out = auth.login(email, password);
  if (!out) return res.status(401).json({ error: "wrong_creds" });
  res.json(out);
});

app.post("/api/auth/logout", requireAuth, (req, res) => {
  auth.logout(req.token);
  res.json({ ok: true });
});

app.get("/api/me", requireAuth, (req, res) => {
  res.json({ user: auth.publicUser(req.user) });
});

// ---- Patients / vitals ---------------------------------------------------
app.get("/api/patients", requireAuth, (req, res) => {
  const owned = ownPatients(req.user);
  res.json({ patients: owned.map((p) => p.snapshot()) });
});

app.get("/api/vitals", requireAuth, (req, res) => {
  const owned = ownPatients(req.user);
  res.json({
    user: auth.publicUser(req.user),
    patients: owned.map((p) => ({
      ...p.snapshot(),
      report: vitalsReport(p, p.vitals),
    })),
  });
});

// ---- Alerts / escalations (filtered to own patients) ----------------------
app.get("/api/alerts", requireAuth, (req, res) => {
  const ids = ownPatientIds(req.user);
  res.json({
    alerts: alerts.list().filter((a) => ids.has(a.patientId)),
    escalations: alerts.listEscalations().filter((e) => ids.has(e.patientId)),
  });
});

app.get("/api/escalations", requireAuth, (req, res) => {
  const ids = ownPatientIds(req.user);
  res.json({ escalations: alerts.listEscalations().filter((e) => ids.has(e.patientId)) });
});

// ---- Medications ----------------------------------------------------------
app.get("/api/medications", requireAuth, (req, res) => {
  const ids = ownPatientIds(req.user);
  res.json({ list: meds.list().filter((m) => ids.has(m.patientId)) });
});

app.post("/api/medications", requireAuth, (req, res) => {
  if (!ownPatientIds(req.user).has(req.body.patientId)) {
    return res.status(403).json({ error: "forbidden" });
  }
  const m = meds.add(req.body);
  res.status(201).json(m);
});

app.post("/api/medications/:id/take", requireAuth, (req, res) => {
  const med = meds.list().find((m) => m.id === req.params.id);
  if (!med) return res.status(404).json({ error: "not_found" });
  if (!ownPatientIds(req.user).has(med.patientId)) return res.status(403).json({ error: "forbidden" });
  res.json(meds.markTaken(req.params.id));
});

app.delete("/api/medications/:id", requireAuth, (req, res) => {
  const med = meds.list().find((m) => m.id === req.params.id);
  if (med && !ownPatientIds(req.user).has(med.patientId)) return res.status(403).json({ error: "forbidden" });
  meds.remove(req.params.id);
  res.json({ ok: true });
});

// ---- Camera zones ---------------------------------------------------------
app.get("/api/camera-zones", requireAuth, (req, res) => {
  const ids = ownPatientIds(req.user);
  res.json({
    zones: cams.zonesList().filter((z) => ids.has(z.patientId)),
    events: cams.eventsList().filter((e) => ids.has(e.patientId)),
  });
});

app.get("/api/camera-zones/:id/live", requireAuth, (req, res) => {
  const ids = ownPatientIds(req.user);
  const zone = cams.getZone(req.params.id);
  if (!zone || !ids.has(zone.patientId)) return res.status(404).json({ error: "not_found" });
  res.json({
    connected: true,
    simulated: true,
    zone: { id: zone.id, name: zone.name, patientName: patientName(zone.patientId) },
    frame: cams.liveFrame(zone.id),
    note: "Simulated live preview. On-device AI only — no video is recorded or stored.",
  });
});

// ---- Misc ------------------------------------------------------------------
app.get("/api/simulation/status", (req, res) => {
  res.json({
    simulated: ["vitals", "camera-events", "live-preview", "billing", "SMS/WhatsApp delivery"],
    real: ["authentication", "data isolation per user", "dashboard", "medications", "rules engine", "alerts", "escalation", "multilingual UI"],
    disclaimer: "Prototype: SanjivanAI is not a certified medical device. Always involve a human caregiver/doctor for decisions.",
  });
});

app.get("/api/health", (req, res) => res.json({ ok: true, service: "SanjivanAI", time: Date.now() }));

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`SanjivanAI prototype running at http://localhost:${PORT}`);
  console.log(`Demo accounts: asharma@demo.in / rprakash@demo.in / wardnurse@demo.in  (password: demo123)`);
});