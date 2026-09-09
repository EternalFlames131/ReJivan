# SanjivanAI — Working Prototype

> A Personal AI Nurse for Every Family — remote health monitoring, on-time medicines, and automatic emergency help, working **at home and in hospital "Virtual Ward" rooms**.

This is the working prototype for the **Hack for Social Cause 2027** submission. It is a responsive web app with a live REST API and a real-time dashboard.

## Run it

```bash
npm install
npm start
```

Open **http://localhost:8080** in your browser. Vitals update every 2 seconds.

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

Privacy-first by design: camera zones run AI on-device and **never record or store video** — only events and alerts.

## Architecture

```
server.js        Express server — auth, REST API + static SPA from /public
auth.js          User accounts (scrypt-hashed passwords) + session tokens + per-user isolation
simulator.js     Simulated vital-signs engine (per-condition profiles + episodes)
rules.js         Real clinical thresholds → normal / caution / danger
alerts.js        Alert generation, de-dupe cooldown + escalation
medications.js   Medication CRUD + "taken" log (persists to data/medications.json)
camerazone.js    Simulated privacy-first room events + live-preview frame descriptor
public/          Front-end SPA (login, dashboard, medicines, camera zones, Virtual Ward, alerts, live view)
data/            JSON persistence (users, medications — created at runtime)
```

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
| `GET /api/medications` | Own medication list |
| `POST /api/medications` | Add medication (own patient only) |
| `POST /api/medications/:id/take` | Log a dose as taken |
| `DELETE /api/medications/:id` | Remove medication |
| `GET /api/camera-zones` | Own zones + event feed |
| `GET /api/camera-zones/:id/live` | Live-preview frame descriptor (simulated) |
| `GET /api/simulation/status` | Real / simulated transparency |

All data endpoints require `Authorization: Bearer <token>`.

## Disclaimer

SanjivanAI is a **prototype**, not a certified medical device. It supports — never replaces — human caregivers and doctors. A human always makes the final decision.

## License

MIT