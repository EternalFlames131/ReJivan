# ReJivan — Working Prototype

> A Personal Nurse for Every Family — remote health monitoring, on-time medicines, and automatic emergency help, working **at home and in hospital "Virtual Ward" rooms**.

This is the working prototype for the **Hack for Social Cause 2027** submission. It is a responsive web app with a live REST API and a real-time dashboard.

## Run it

```bash
npm install
npm start
```

Open **http://localhost:8080** in your browser. Vitals update on every poll (~2.5 s).

## Deploy to Vercel (live public website)

The app is serverless-ready: the whole simulation is a **deterministic, stateless
function of (patient, wall-clock time)** — no background process, so it runs on
short-lived Vercel functions exactly as it runs on your laptop.

```bash
vercel          # link + deploy preview (first time: browser sign-in)
vercel --prod   # publish to production
```

- Generates a free domain like `https://<project>.vercel.app`; attach your own
  domain in the Vercel dashboard later.
- Entry points: `api/index.js` (serverless handler) + `vercel.json` (route all → `/api/index`).
- Cloud notes: demo accounts always exist; new registrations / medication logs
  live in memory per instance (JSON writes are best-effort). For chatty offline
  persistence, add Postgres/Redis.

## Sign in (data isolation)

Every account sees **only the patients it registered**. Passwords are stored hashed (scrypt), sessions are tokens.

| Demo account | Password | Sees |
|---|---|---|
| `asharma@demo.in` | `demo123` | Anita Sharma (home) + her camera CAM1 + meds |
| `rprakash@demo.in` | `demo123` | Ram Prakash (home) + his camera CAM2 + meds |
| `wardnurse@demo.in` | `demo123` | Virtual Ward beds (Meera, Kavitha) + ward cameras |

New families can register via the "Create account" screen. All `/api/*` endpoints (except auth + health) require `Authorization: Bearer <token>` and are filtered by the logged-in user.

## What is REAL vs SIMULATED (prototype honesty)

| Layer | Status |
|---|---|
| Authentication + per-user data isolation | **Real** (scrypt-hashed passwords, session tokens, every API filtered by user) |
| Dashboard, patient cards, real-time UI | **Real** |
| Vitals rule engine + danger detection | **Real** (clinical thresholds) |
| Alert generation + escalation workflow | **Real** logic (delivery is stubbed) |
| Medications scheduling + "Mark taken" tracking | **Real** (persisted to JSON) |
| Multilingual UI (EN / HI / BN / TA / TE) | **Real** |
| Live camera preview for family | **Real UI**, feed **simulated** (privacy-first: metadata only, nothing recorded/stored) |
| Vital-sign data from a wearable | **Simulated** (`simulator.js`) |
| Camera-zone events (fall / out-of-bed / low activity) | **Simulated** (`camerazone.js`) |
| SMS / WhatsApp / ambulance delivery | **Simulated** (live-API hooks stubbed) |
| Billing | **Simulated** (not yet in UI) |

Privacy-first by design: camera zones run Prajñā on-device and **never record or store video** — only events and alerts.

## Architecture

```
server.js        Express app — auth, REST API + static SPA from /public
                 (also the Vercel handler; app.listen only when run directly)
api/index.js     Vercel serverless entrypoint (requires ../server.js)
vercel.json      Vercel config — rewrites every route to /api/index
auth.js          User accounts (scrypt-hashed) + stateless HMAC-signed tokens
simulator.js     Deterministic vital-signs engine (pure function of the clock)
rules.js         Real clinical thresholds → normal / caution / danger
medications.js   Medication CRUD + "taken" log (JSON, best-effort persistence)
camerazone.js    Deterministic privacy-first room events + live-preview frames
public/          Front-end SPA (login, dashboard, medicines, camera zones, Virtual Ward, alerts, live view)
data/            JSON persistence (users, medications — created at runtime)
```

Design notes
- **Deterministic & stateless:** every request computes the same state for the
  same time on any instance (serverless-friendly); no `setInterval`, no mutable
  engine state. Danger episodes recur on a schedule so a demo session always
  shows real alerts + the emergency call chain progressing.
- **Emergency auto-call chain:** danger alert → family (0–4.2 s) → backup
  (5.5–9.8 s) → emergency services 108/112 (11–13.2 s). Real trigger/priority/
  retry logic, simulated placement.
- **Privacy-first:** cameras never record/store video — the "live" view plays a
  muted OPENLY-LICENSED STOCK clip (public domain / CC0 / CC BY, attribution in
  `public/videos/ATTRIBUTION.txt`) as demo footage with on-device scene metadata
  (person, motion, lighting) overlaid on top.

## API

| Endpoint | Purpose |
|---|---|
| `POST /api/auth/register` | Create an account |
| `POST /api/auth/login` | Sign in → token |
| `POST /api/auth/logout` | End session |
| `GET /api/me` | Current user |
| `GET /api/health` | Service health (public) |
| `GET /api/patients` | Own patient roster |
| `GET /api/vitals` | Live vitals + per-value status |
| `GET /api/alerts` | Own alerts + escalations |
| `GET /api/escalations` | Own escalations only |
| `GET /api/calls` | Emergency auto-call chain status (from danger alerts) |
| `GET /api/medications` | Own medication list |
| `POST /api/medications` | Add medication (own patient only) |
| `POST /api/medications/:id/take` | Log a dose as taken |
| `DELETE /api/medications/:id` | Remove medication |
| `GET /api/camera-zones` | Own zones + event feed |
| `GET /api/camera-zones/:id/live` | Live-preview frame descriptor (simulated) |
| `GET /api/simulation/status` | Real / simulated transparency |

All data endpoints require `Authorization: Bearer <token>`.

## Disclaimer

ReJivan is a **prototype**, not a certified medical device. It supports — never replaces — human caregivers and doctors. A human always makes the final decision.

## License

MIT