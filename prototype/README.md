# SanjivanAI — Working Prototype

> A Personal AI Nurse for Every Family — remote health monitoring, on-time medicines, and automatic emergency help, working **at home and in hospital "Virtual Ward" rooms**.

This is the working prototype for the **Hack for Social Cause 2027** submission. It is a responsive web app with a live REST API and a real-time dashboard.

## Run it

```bash
npm install
npm start
```

Open **http://localhost:8080** in your browser. Vitals update every 2 seconds.

## What is REAL vs SIMULATED (prototype honesty)

| Layer | Status |
|---|---|
| Dashboard, patient cards, real-time UI | **Real** |
| Vitals rule engine + danger detection | **Real** (clinical thresholds) |
| Alert generation + escalation workflow | **Real** logic (delivery is stubbed) |
| Medications scheduling + "Mark taken" tracking | **Real** (persisted to JSON) |
| Multilingual UI (EN / HI / BN / TA / TE) | **Real** |
| Vital-sign data from a wearable | **Simulated** (`simulator.js`) |
| Camera-zone events (fall / out-of-bed / low activity) | **Simulated** (`camerazone.js`) |
| SMS / WhatsApp / ambulance delivery | **Simulated** (live-API hooks stubbed) |
| Billing | **Simulated** (not yet in UI) |

Privacy-first by design: camera zones run AI on-device and **never record or store video** — only events and alerts.

## Architecture

```
server.js        Express server — REST API + static SPA from /public
simulator.js     Simulated vital-signs engine (per-condition profiles + episodes)
rules.js         Real clinical thresholds → normal / caution / danger
alerts.js        Alert generation, de-dupe cooldown + escalation
medications.js   Medication CRUD + "taken" log (persists to data/medications.json)
camerazone.js    Simulated privacy-first room events (fall, out-of-bed, low activity)
public/          Front-end SPA (dashboard, medicines, camera zones, Virtual Ward, alerts)
data/            JSON persistence (created at runtime)
```

## API

| Endpoint | Purpose |
|---|---|
| `GET /api/health` | Service health |
| `GET /api/patients` | Patient roster + snapshots |
| `GET /api/vitals` | Live vitals + per-value status |
| `GET /api/alerts` | Alerts + escalations |
| `GET /api/medications` | Medication list |
| `POST /api/medications` | Add medication |
| `POST /api/medications/:id/take` | Log a dose as taken |
| `DELETE /api/medications/:id` | Remove medication |
| `GET /api/camera-zones` | Zones + event feed |
| `GET /api/simulation/status` | Real / simulated transparency |

## Disclaimer

SanjivanAI is a **prototype**, not a certified medical device. It supports — never replaces — human caregivers and doctors. A human always makes the final decision.

## License

MIT