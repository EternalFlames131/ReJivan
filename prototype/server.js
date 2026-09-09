"use strict";
/**
 * SanjivanAI — prototype server (serverless-ready).
 * Express + lightweight JSON storage. Serves the SPA from /public and exposes
 * a REST API. Runs the same on a laptop (`npm start`) and on Vercel.
 *
 * Serverless design note:
 *   There is NO background tick loop. The whole simulation (vitals, alerts,
 *   escalations, emergency calls, camera events) is a pure, deterministic
 *   function of (patient, wall-clock time). Every request therefore computes
 *   the system state instantly and consistently on any server instance.
 *
 * Authentication: every data endpoint is behind a login. Each account (family
 * or Virtual Ward) sees ONLY the patients it registered. Passwords are hashed
 * (scrypt); sessions are stateless signed tokens.
 */
const path = require("path");
const fs = require("fs");
const express = require("express");
const cors = require("cors");

const { generateVitals, hash01, SLOT_MS } = require("./simulator");
const { vitalsReport, dangerLabels } = require("./rules");
const { AuthStore } = require("./auth");
const { MedicationStore } = require("./medications");
const { cameraZones, deriveCameraEvents, liveFrame } = require("./camerazone");

const DATA_DIR = path.join(__dirname, "data");
try {
  if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });
} catch (e) {
  /* cloud read-only fs — fine */
}

// Demo scene is anchored in the Andaman & Nicobar Islands (UT) — Samrat's home:
// family patients at Port Blair / outer-island homes, ward patients at the main
// referral hospital (GB Pant Hospital, Port Blair).
const REGION = "Andaman & Nicobar Islands (UT), India";

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, "public")));

// ---- Seed users & patients -------------------------------------------------
const auth = new AuthStore(DATA_DIR); // auto-seeds the three demo accounts

// Static patient metadata (specs). Vitals are generated on demand.
const patients = [
  { id: "P1", name: "Anita Sharma", age: 67, sex: "F", condition: "hypertension", location: "Home — Living Room", address: "Junglighat, Port Blair", userId: auth.findByEmail("asharma@demo.in").id },
  { id: "P2", name: "Ram Prakash", age: 74, sex: "M", condition: "diabetes", location: "Home — Bedroom", address: "Hut Bay, Little Andaman (served via PHC)", userId: auth.findByEmail("rprakash@demo.in").id },
  { id: "P3", name: "Meera Nair", age: 58, sex: "F", condition: "post-surgery", location: "Virtual Ward", ward: "Ward A · Bed 1", address: "GB Pant Hospital, Port Blair", userId: auth.findByEmail("wardnurse@demo.in").id },
  { id: "P4", name: "Kavitha Rao", age: 61, sex: "F", condition: "heart-arrhythmia", location: "Virtual Ward", ward: "Ward A · Bed 2", address: "GB Pant Hospital, Port Blair", userId: auth.findByEmail("wardnurse@demo.in").id },
];

const meds = new MedicationStore(DATA_DIR);
if (meds.list().length === 0) {
  meds.add({ patientId: "P1", name: "Amlodipine", dose: "5 mg", frequency: "daily", times: ["08:00", "20:00"], notes: "After food" });
  meds.add({ patientId: "P2", name: "Metformin", dose: "500 mg", frequency: "twice daily", times: ["09:00", "21:00"], notes: "With meals" });
  meds.add({ patientId: "P3", name: "Paracetamol", dose: "650 mg", frequency: "8 hourly", times: ["08:00", "16:00", "00:00"], notes: "For fever" });
}

// ---- Helpers ---------------------------------------------------------------
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
function snapshotFor(p, now) {
  const g = generateVitals(p, now);
  return {
    id: p.id, name: p.name, age: p.age, sex: p.sex,
    location: p.location, addr: p.address || null, ward: p.ward || null,
    region: REGION, condition: p.condition,
    vitals: g.vitals, lastUpdated: now, episode: g.episode,
  };
}

const CONTACTS = {
  P1: { family: "+91 98300 11001", backup: "+91 98300 11002" },
  P2: { family: "+91 98300 12001", backup: "+91 98300 12002" },
  P3: { family: "Ward A nurse · +91 98300 13001", backup: "Duty doctor · +91 98300 13002" },
  P4: { family: "Ward A nurse · +91 98300 14001", backup: "Duty doctor · +91 98300 14002" },
};

// ---- Deterministic state derivation (whole engine, computed on request) ----
const SPAN_ALERTS = 30; // look back ~30 slots (~100 min) for the alerts feed

function alertsFor(user, now) {
  const ids = ownPatientIds(user);
  const slot = Math.floor(now / SLOT_MS);
  const alerts = [];
  const escalations = [];
  const pushDanger = (alert) => {
    alerts.push(alert);
    escalations.push({
      id: "ESC-" + alert.id,
      alertId: alert.id,
      patientId: alert.patientId,
      patientName: alert.patientName,
      severity: "danger",
      channels: ["SMS + Emergency alert"],
      to: alert.ward ? "Nurse station + on-duty nurse" : "Family caregiver",
      dispatchedAt: (alert.createdAt || now) + 2000,
      delivered: true,
      simulationNotice:
        "Simulated delivery. In production: caregiver SMS/WhatsApp, email, and emergency-services dispatch via live APIs.",
    });
  };

  for (let k = SPAN_ALERTS; k >= 0; k--) {
    const s = slot - k;
    const center = (s + 0.5) * SLOT_MS;
    for (const p of patients) {
      if (!ids.has(p.id)) continue;
      const g = generateVitals(p, center);
      const report = vitalsReport(p, g.vitals);
      const labels = dangerLabels(report);
      if (labels.length) {
        pushDanger({
          id: "ALT-" + p.id + "-" + s,
          patientId: p.id,
          patientName: p.name,
          age: p.age,
          ward: p.ward || null,
          location: p.location,
          type: "vitals",
          severity: "danger",
          message: `${p.name} — DANGER: ${labels.join(", ")} out of normal range. ${
            p.ward ? "Virtual Ward " + p.ward : "At home"
          }. Automated check.`,
          vitals: report,
          createdAt: s * SLOT_MS,
        });
      }
    }
  }

  for (const e of deriveCameraEvents(now).filter((ev) => ids.has(ev.patientId))) {
    if (e.severity === "danger") {
      const z = cameraZones.find((x) => x.id === e.zoneId);
      pushDanger({
        id: "EVT-" + e.id,
        patientId: e.patientId,
        patientName: patientName(e.patientId),
        ward: z && z.ward ? z.name : null,
        type: "camera",
        severity: "danger",
        message: e.message,
        createdAt: e.at,
      });
    }
  }

  alerts.sort((a, b) => b.createdAt - a.createdAt);
  escalations.sort((a, b) => b.dispatchedAt - a.dispatchedAt);
  return { alerts: alerts.slice(0, 15), escalations: escalations.slice(0, 10) };
}

function stateAt(dial, ans, answered, el) {
  if (el < dial) return "pending";
  if (el < ans) return "dialing";
  return answered ? "answered" : "unanswered";
}

/**
 * Emergency call chain for one danger alert, derived from elapsed time.
 * REAL trigger/priority/retry/escalation logic — SIMULATED call placement.
 */
function callForAlert(alert, now) {
  const c = CONTACTS[alert.patientId] || { family: "Family caregiver", backup: "Backup contact" };
  const el = now - alert.createdAt;
  const aFam = hash01(alert.id + ":fam") < 0.55;
  const aBak = hash01(alert.id + ":bak") < 0.45;
  const aEm = hash01(alert.id + ":em") < 0.9;

  const fam = stateAt(0, 4200, aFam, el);
  const bak = !aFam ? stateAt(5500, 9800, aBak, el) : "pending";
  const em = !aFam && !aBak ? stateAt(11000, 13200, aEm, el) : "pending";

  let status = "dialing";
  if (em === "answered") status = "dispatched";
  else if (fam === "answered" || bak === "answered") status = "answered";
  else if (fam === "dialing" || bak === "dialing" || em === "dialing") status = "dialing";
  else if (el >= 5500 && !aFam && (bak === "pending" || em === "pending")) status = "escalating";
  else status = "complete";

  const push = (t, msg) => {
    if (el >= t) log.push({ t: alert.createdAt + t, msg });
  };
  const log = [];
  push(0, `Danger alert ${alert.id} — automatic call chain started immediately`);
  push(0, `Calling family → ${c.family} (voice)`);
  if (fam === "answered") {
    push(4200, `family answered on ${c.family} — vitals + alert shared live`);
  } else if (el >= 4200) {
    push(4200, `family did not answer after 2 attempts — escalating to next contact NOW`);
    push(5500, `Calling backup → ${c.backup} (voice)`);
    if (bak === "answered") {
      push(9800, `backup answered on ${c.backup} — vitals + alert shared live`);
    } else if (el >= 9800) {
      push(9800, `backup did not answer after 2 attempts — escalating to emergency services NOW`);
      push(11000, `Calling emergency services → 108 / 112 (voice-dispatch)`);
      if (em === "answered") {
        push(13200, `Emergency services reached on 108 / 112 — ambulance dispatched, GPS + vitals sent`);
      } else if (el >= 15000) {
        push(15000, `Emergency line busy — on-duty staff + hospital alerted directly`);
      }
    }
  }

  return {
    id: "CAL-" + alert.id.replace(/^ALT-/, ""),
    alertId: alert.id,
    patientId: alert.patientId,
    patientName: alert.patientName,
    startedAt: alert.createdAt,
    status,
    ladder: [
      { label: "family", to: c.family, mode: "voice", emergency: false, state: fam },
      { label: "backup", to: c.backup, mode: "voice", emergency: false, state: bak },
      { label: "emergency", to: "108 / 112", mode: "voice-dispatch", emergency: true, state: em },
    ],
    log: log.sort((a, b) => a.t - b.t),
  };
}

function callsFor(user, now) {
  const ids = ownPatientIds(user);
  const { alerts } = alertsFor(user, now);
  return alerts.slice(0, 3).map((a) => callForAlert(a, now)).filter((c) => ids.has(c.patientId));
}

function cameraFor(user, now) {
  const ids = ownPatientIds(user);
  return {
    zones: cameraZones.filter((z) => ids.has(z.patientId)),
    events: deriveCameraEvents(now).filter((e) => ids.has(e.patientId)).slice(0, 12),
  };
}

// ---- Auth API --------------------------------------------------------------
app.post("/api/auth/register", (req, res) => {
  const { name, email, password, role } = req.body || {};
  if (!name || !email || !password) return res.status(400).json({ error: "missing_fields" });
  if (String(password).length < 6) return res.status(400).json({ error: "weak_password" });
  const out = auth.register({ name, email, password, role });
  if (out.error) return res.status(409).json(out);
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

// ---- Patients / vitals -----------------------------------------------------
app.get("/api/patients", requireAuth, (req, res) => {
  const now = Date.now();
  res.json({ patients: ownPatients(req.user).map((p) => snapshotFor(p, now)) });
});

app.get("/api/vitals", requireAuth, (req, res) => {
  const now = Date.now();
  res.json({
    user: auth.publicUser(req.user),
    patients: ownPatients(req.user).map((p) => {
      const snap = snapshotFor(p, now);
      return { ...snap, report: vitalsReport(p, snap.vitals) };
    }),
  });
});

// ---- Alerts / escalations / calls (auto call chain) ------------------------
app.get("/api/alerts", requireAuth, (req, res) => {
  const { alerts, escalations } = alertsFor(req.user, Date.now());
  res.json({ alerts, escalations });
});

app.get("/api/escalations", requireAuth, (req, res) => {
  res.json({ escalations: alertsFor(req.user, Date.now()).escalations });
});

app.get("/api/calls", requireAuth, (req, res) => {
  res.json({ calls: callsFor(req.user, Date.now()) });
});

// ---- Medications -----------------------------------------------------------
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

// ---- Camera zones (privacy-first, no video stored) -------------------------
app.get("/api/camera-zones", requireAuth, (req, res) => {
  res.json(cameraFor(req.user, Date.now()));
});

app.get("/api/camera-zones/:id/live", requireAuth, (req, res) => {
  const ids = ownPatientIds(req.user);
  const zone = cameraZones.find((z) => z.id === req.params.id);
  if (!zone || !ids.has(zone.patientId)) return res.status(404).json({ error: "not_found" });
  res.json({
    connected: true,
    simulated: true,
    zone: { id: zone.id, name: zone.name, patientName: patientName(zone.patientId) },
    frame: liveFrame(zone.id),
    note: "Simulated live preview. On-device AI only — no video is recorded or stored.",
  });
});

// ---- Misc ------------------------------------------------------------------
app.get("/api/simulation/status", (req, res) => {
  res.json({
    region: REGION,
    simulated: ["vitals", "camera-events", "live-preview", "billing", "SMS/WhatsApp delivery", "emergency phone calls"],
    real: ["authentication", "data isolation per user", "dashboard", "medications", "rules engine", "alerts", "escalation", "emergency auto-call chain (priority + retry + escalation)", "multilingual UI"],
    disclaimer: "Prototype: SanjivanAI is not a certified medical device. Always involve a human caregiver/doctor for decisions.",
  });
});

app.get("/api/health", (req, res) => res.json({ ok: true, service: "SanjivanAI", time: Date.now() }));

// ---- Export for Vercel; direct listen only when run locally -----------------
module.exports = app;

if (require.main === module) {
  const PORT = process.env.PORT || 8080;
  app.listen(PORT, () => {
    console.log(`SanjivanAI prototype running at http://localhost:${PORT}`);
    console.log(`Demo accounts: asharma@demo.in / rprakash@demo.in / wardnurse@demo.in  (password: demo123)`);
  });
}