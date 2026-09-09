# SanjivanAI — CONVERSATION & MEMORY LOG (auto-saved)

> Purpose: EVERY meaningful exchange, thought and decision about this project is saved here so the user NEVER has to re-explain anything. Sessions are resumed by reading this file + CONTEXT.md.
> Auto-save rule: after each turn that involves decisions/new info, a dated entry is appended by the assistant automatically.

---

## 2026-09-09 (Day 2, afternoon — public repo + serverless/Vercel refactor)

### What the user asked (in order)
1. Refactor the engine to be stateless so the prototype can run as a website on **Vercel** (user chose Vercel, on a public/different domain) for a friend to monitor.
2. **Make the GitHub repo PUBLIC** and give the link → done: **https://github.com/EternalFlames131/SanjivanAI** (now PUBLIC, branch main; HSC requirement satisfied — no longer a pending task).
3. "Did you save every last detail?" → this entry is that save.

### Repo made public
- `gh repo edit EternalFlames131/SanjivanAI --visibility public --accept-visibility-change-consequences` — verified PUBLIC before finishing.
- Note: `--accept-visibility-change-consequences` flag is required by gh before the visibility takes effect.

### SERVERLESS-READY REFACTOR (the big change)
Why: Vercel functions are short-lived — no 24/7 process, no shared memory. The old prototype ran a `setInterval` tick loop holding all state in memory → that cannot work on Vercel. Solution: made the whole engine a **pure, deterministic function of (patient, wall-clock time)** — same output for the same time on any instance, works on a laptop and in the cloud unchanged.
- `prototype/simulator.js` (rewritten): no more `Patient` class / `tick()`. Export `generateVitals(spec, nowMs)` → vitals + optional episode. Per-200s slot: `hash01(id+":ep:"+slot)` < 0.5 → one named danger episode active for that whole slot, rising/falling sinusoidally (peak mid-slot); always-drifting sines + 2s jitter keep values alive. **Diabetes baseline glucose 190 → 150** (190 was permanently "danger" because cautionHi is 180).
- `prototype/camerazone.js` (rewritten): `deriveCameraEvents(now)` (150s slots, 32% chance of an event: fall/out-of-bed/low-activity/no-activity-10min) + `liveFrame(zoneId, now)` — all deterministic, no state.
- `prototype/server.js` (rewritten): no background loop. Everything computed per request: `alertsFor()`, `callsFor()`, `cameraFor()`. The **emergency call chain lives here now** (the old `alerts.js` + `caller.js` classes were removed): per alert, ladder = family (dial 0 → answer attempt 4.2s) → backup (5.5s → 9.8s) → emergency 108/112 (11s → 13.2s); deterministic answer odds family 55% / backup 45% / emergency 90%; status derived from elapsed time; log lines derived from elapsed; contact numbers unchanged. Exports the Express app; `app.listen` only when `require.main === module` (so `npm start` still works). `/api/simulation/status` kept identical (the PDF builder parses it).
- `prototype/auth.js` (rewritten): **stateless signed tokens** — login signs `{uid, exp}` with HMAC-SHA256 (`SESSION_SECRET` env, else dev fallback), 7-day expiry; no session Map (that died between serverless instances). Demo accounts auto-seeded in code (even if data/users.json is unreadable); disk writes are best-effort (cloud fs is read-only). Logout is client-side discard.
- `prototype/medications.js`: `_save()` now try/catch (in-memory schedule on cloud, persisted JSON on laptop).
- `prototype/rules.js`: added `dangerLabels(report)` (moved from old evaluate()).
- NEW `prototype/api/index.js` — Vercel serverless entrypoint (`module.exports = require("../server.js")`).
- NEW `prototype/vercel.json` — rewrites every route to `/api/index`.

### Bug found + fixed during testing (important)
- Auth tokens were **double-encoded**: `digest("base64")` returns a *string*, then `_b64url()` base-64-url-encoded that text again → token issued ≠ token verified → first request after login returned `401 auth_required`. Fixed `_issueToken` to hash the raw digest once. Verified: fresh token verifies, tampered token rejected.
- Also learned: flakiness earlier was NOT a server bug — a leftover background server on port 8080 was intercepting tests (killed PID 16900); after the fix + clean start, login is 8/8 and two full E2E runs gave identical output.

### Verification (all green, deterministic)
- Nurse login: sees only P3 Meera (post-surgery) + P4 Kavitha (heart-arrhythmia, mid DANGER episode hr≈175); alerts capped 15; escalations 10; calls 3; newest call ladder family:unanswered → backup:answered → emergency:pending; camera zones 2, events 3; BED1 live frame person=true.
- Sharma family login: sees only P1 — 1 patient, 1 med, 1 zone (isolation holds).
- Static site + lang.json served; /api/simulation/status returns SIM=6 / REAL=9 (unchanged for the PDF).

### Vercel deployment — IN PROGRESS, waiting on the user
- `vercel` CLI 59.13.1 installed globally (`npm i -g vercel`; npm warned about esbuild postinstall allow-scripts — harmless).
- Not logged in → started `vercel login github` in background → device-code flow:
  - URL: **https://vercel.com/oauth/device?user_code=DHLK-VNLG** (user signs in with GitHub / creates account → Authorize).
  - After that: `vercel` deploy from `prototype/` → free `<project>.vercel.app` URL; custom domain attachable later in the dashboard.
- The user pivoted to a localhost login problem before finishing the browser step — root cause was **no server running** (test instances were killed), not a bug. Persistent server relaunched: `node server.js` in prototype\ → **http://localhost:8080** (asharma@demo.in / demo123).

### To-do after this save
- Finish Vercel auth (user) → run `vercel deploy` → give the live public URL → verify the app fully on Vercel (logins, danger episode, calls panel, live camera, 2 languages).
- Note honestly in docs: on Vercel, data (new registrations, med "taken" log) is in-memory per instance — demo accounts + seeds are the source of truth; fine for the hack, real persistence would need a DB (Postgres/Redis).

---

## 2026-09-08 (Day 1 — project start)

### 1. Idea origin
- User asked whether "make a fully functional Android/iOS app from scratch" was possible. Answer: yes, via Flutter/cross-platform; iOS compile needs a Mac later; App Store/Play Store publishing needs user's own accounts.
- User revealed target: **MyBharat "HSC" competition**. Researched: this is **Hack for Social Cause (HSC) 2027**, part of **VBYLD 2027** (Ministry of Youth Affairs & Sports + **IIT Bombay** as knowledge partner).

### 2. Competition facts (verified from mybharat.gov.in/pages/hack_social on 2026-09-08)
- **LAST SUBMISSION DATE: 15 October 2026** (window 1 Sep – 15 Oct). Registration open.
- Eligibility: Indian citizen, 18–29 as of 17 Aug 2026, enrolled in AISHE-registered institution; team up to 3 (same or different institutions; solo allowed); each member registers individually; 1 Team Lead submits.
- Deliverables: Problem Statement + 6–7 slide deck (≤10 MB, with AI-tools disclosure) + Working Prototype (PUBLIC GitHub repo, MIT, README, architecture, sample data) + Demo Video 3–5 min / ≥720p / ≤80 MB + Annexure 1 self-declaration & IDs.
- Stages: submission by 15 Oct → State hackathon 16 Oct–30 Nov (3 teams shortlist) → IIT-B screening 1–15 Dec → **36 national finalists**, National Showcase 10–12 Jan 2027 Delhi. Prizes ₹75k/50k/25k/15k/15k; finalists get ₹6k dev grant.
- Evaluation (6 params): Relevance · Technical Strength · Functionality · Creativity/Innovation · Social Cause Impact · Presentation & Team.

### 3. Idea selection
- User theme choice: **Healthcare & Wellbeing** (plus fits **Elderly Care & Healthy Ageing** — 2 themes deliberately).
- User's own idea (chose over my 4 suggestions): a "**personal AI nurse**" — continuous monitoring, on-time medicines, family can care at home instead of hospital, automatic emergency signals to emergency services, no person needed on-site, affordable subscription.
- Verified feasibility: full hardware product = multi-year; **hackathon-realistic = working software prototype that SIMULATES sensors** and makes dashboard/meds/alerts/escalation real.

### 4. Name, folder, repo
- New working folder (started as "HSC AI Nurse") → renamed **SanjivanAI** (user's choice), path `C:\Users\samra\OneDrive\Desktop\SanjivanAI`.
- Concept PDF built: **SanjivanAI_Concept_Document_v1.1.pdf** (9 pages) using Edge headless + HTML source (pipeline from owner's AGENTS.md).
- GitHub: **private repo created** `EternalFlames131/SanjivanAI` (account EternalFlames131), branch **main**. ⚠️ Must be made PUBLIC before 15 Oct (submission requirement).

### 5. Scope expansion (user's additions)
- Use case extended beyond home: **hospitals** where doctors/nurses can't always be present → **"Virtual Ward"** mode (nurse-station view, rooms, priority alerts).
- Monitoring NOT only wearables → **CCTV-style room cameras**: fall detection, out-of-bed, low activity; video can also estimate heart/resp rate contact-free. **Privacy-first design is mandatory** (on-device AI, NO video recorded/stored, consent, DPDP-aligned) — positioned as a winning point.
- Prototype honesty: camera events + vitals + billing **simulated**; dashboard, medications, rules engine, alerts, escalation, multilingual **fully real**.

### 6. Portability ("perfect folder" + drive)
- Folder made **self-contained** → works from any drive: `docs/source` (PDF HTML), `tools/build_pdf.ps1` + `verify_pdf.py`, `references/hsc_guidelines_summary.md`, `opencode-config/` (backup of owner's global opencode AGENTS.md, opencode.jsonc, master LOG.md), plus README/CONTEXT/AGENTS.
- **Removable drive F:** → full copy at `F:\SanjivanAI` (mirrored, includes .git). F: = "live" folder with opencode on the other device.

### 7. Automation & safety (multi-repo protection)
- **setup.ps1**: one-time auto-setup per PC — installs missing Python/pypdf/Edge/Git via winget, sets repo-LOCAL git identity, locks remote to SanjivanAI ONLY, enables auto-push, checks GitHub login, tests PDF pipeline, writes per-PC marker `tools\.setup-done-<PC>.txt`.
- **Auto-push hook** `.githooks/post-commit`: after every commit pushes to SanjivanAI repo. **Hardened:** only fires when origin == SanjivanAI URL; otherwise does nothing (tested with a throwaway repo — other repos cannot be touched). Global git settings untouched (verified).
- **Fully automatic setup:** opencode auto-runs setup.ps1 at session start whenever the per-PC marker is missing — user never types a command (AGENTS.md RULE).
- Entered as rule in AGENTS.md: keep commits deliberate; auto-push is enabled.

### 8. Time estimates (user asked "exactly how much time")
- Prototype itself ~20 working hours (I build, user decides). Breakdown: core+dashboard+simulator 5h; meds+danger engine 4h; camera zones+alerts+escalation 4h; views+multilingual+polish 4h; docs+repo+deck help 3h.
- Video (user narrates): ~6–8 hours extra. Buffer built before 15 Oct.

---

## 2026-09-08 (Day 1 — CLOSED, night)

- User tuned off for the night; work resumes TOMORROW (Day 2).
- **DAY 2 FIRST ACTION: build the prototype** — start with core app + dashboard + vital simulator so the user sees something on screen quickly. ~5h block. Get user's go-ahead at session start.
- Setup verified all-green on this PC; auto-read/auto-save fully wired. Nothing is blocking.

---

## 2026-09-09 (Day 2 — prototype build, morning)

- **DAY 2 GOAL (from yesterday's plan) achieved:** built the working prototype in `prototype/`.
- User said "continue" → I resumed (no re-explaining needed per protocol) and built the full first increment in one sitting.
- Stack: Node + Express + JSON storage. Real-time simulation loop (2s ticks, 4 demo patients: 2 at home, 2 in hospital Virtual Ward Ward A).
- **REAL:** dashboard with live vitals + green/amber/red status; medicines (add / mark taken / delete, persisted); rules engine (clinical thresholds for HR/SpO2/BP/temp/glucose); alert generation + escalation workflow; Virtual Ward nurse-station view with priority queue (HIGH/MEDIUM/NORMAL); camera-zone feed (privacy-first, no video); UI in 5 languages (EN/HI/BN/TA/TE) with a toggle.
- **SIMULATED (clearly labelled in UI + /api/simulation/status):** vitals data, camera events, SMS/WhatsApp delivery. Billing noted as simulated, not yet built into UI.
- Verified live: 6 alerts + 5 escalations over a 95-second run (danger BP, glucose, HR + a no-activity camera event), HTTP 200 on the page. 0 npm vulnerabilities.
- **How to run:** `cd prototype && npm start` → http://localhost:8080.
- Next steps: user reviews the running app; then wire escalation channels to real APIs OR move on to problem-statement sheet + slide deck + demo video planning. PWA service-worker (offline) still pending, low priority.
---

## 2026-09-09 (Day 2 — auth + live camera, late morning)

- User asked for two additions to the prototype:
  1. **Login so every user only sees their own registered patients.**
  2. **Live camera view so family can observe the patient anytime.**
- Built both:
  - **Auth (REAL):** `auth.js` — register/login/logout; passwords hashed with Node scrypt (never plain text); session tokens; `GET /api/me`. Every data endpoint now requires `Authorization: Bearer <token>` and is filtered by the logged-in user (patients, vitals, meds, alerts, escalations, cameras). Cross-user action returns 403/404.
  - **Demo accounts:** `asharma@demo.in` (owns Anita P1), `rprakash@demo.in` (owns Ram P2), `wardnurse@demo.in` (owns ward beds P3+P4). Password for all: `demo123`.
  - **Live camera (UI REAL, feed SIMULATED + labelled):** `View live` button on each camera zone → modal with animated privacy-safe room preview (canvas) + person/motion/lighting metadata from `/api/camera-zones/:id/live`. On-device-AI framing — **no video recorded or stored**, consistent with privacy-first promise. Connect/Disconnect + live clock.
  - All new UI text translated into all 5 languages (EN/HI/BN/TA/TE).
- Verified end-to-end: no-token → 401; Sharma family sees only P1 + own meds + CAM1; nurse sees only P3/P4; nurse blocked from CAM1; wrong password rejected. Committed + auto-pushed (d6dec9b).
- Note for later: registering a NEW family does not yet create a patient for them (no "Add patient" flow yet) — the seeded demo accounts own the 4 demo patients.
---

## 2026-09-09 (Day 2 — auto emergency-call chain, noon)

- User asked: "add auto alert feature to call the emergency services and family members immediately without delay."
- Built **AutoCaller** (`caller.js`): the instant any DANGER alert fires, a call chain starts with NO delay:
  1. **Family caregiver** → 2. **Backup contact** (2 retries, then escalate) → 3. **Emergency services 108/112** (automatic ambulance dispatch, GPS + vitals sent).
- Wired into `AlertManager` via `onDangerAlert` callback → fires for BOTH vitals danger alerts AND camera fall/danger events.
- Real-time **call-flow panel** on the Alerts tab: one card per call, status per step (pending/dialing/answered/unanswered), timestamps, full event log, next-in-line escalation indicator.
- Honesty: call PLACEMENT is SIMULATED (real product uses a telecom API such as Twilio/India's 108 integration); the auto-trigger, priority order, retry and escalation logic is REAL and runs live.
- Verified: ward danger alert → CAL002, family (ward nurse) answered → backup + emergency stayed pending.
- All new strings translated EN/HI/BN/TA/TE. Committed + auto-pushed (8a45aba).
- Still open: "Add patient" flow for newly registered families; real API wiring; PWA offline SW.
---

## 2026-09-09 (Day 2 — PDF auto-update, early afternoon)

- User asked: "update everything into the pdf as well whenever any changes are made automatically."
- Built the **auto-updating concept PDF**:
  - New `.githooks/pre-commit` hook: before EVERY commit it rebuilds `docs\SanjivanAI_Concept_Document_v1.1.pdf` and stages it, so the PDF can never go stale. If Edge fails (e.g. PDF open), it warns but never blocks the commit.
  - New `docs/features.json` — canonical machine-readable feature list + demo accounts + "real/simulated" status + notes.
  - `tools/build_pdf.ps1` upgraded: injects an auto-generated **"Live Prototype Status"** section — feature table from features.json, plus the REAL / SIMULATED lists parsed LIVE out of `prototype/server.js` (so the document always mirrors the actual code), plus build date. Writes generated HTML to Temp\opencode, renders via Edge headless, verifies via pypdf.
  - Placeholders added in `docs/source/SanjivanAI_doc_source.html` (`{{STATUS_ROW}}`, `{{BUILD_DATE}}`, `<!--AUTO:PROTOTYPE_SNAPSHOT-->`).
  - Verified: PDF rebuilds to 9 pages, snapshot content confirmed in text (demo accounts, emergency call chain, Tamil/Telugu languages, etc.). Hook fired automatically on the commit itself. Auto-pushed (4a6dd9f).
- **How it works for the user:** no action needed — any future commit (added feature, fix, memory save) automatically refreshes the PDF to match.
- One maintenance note: when a genuinely NEW feature ships, its row should be added to `docs/features.json` once; the rest (status lists, dates, accounts) updates itself.
---

## Standing auto-save rules (do this every session)
1. After any turn with decisions/thoughts/new info, append a `## YYYY-MM-DD (Day N — note)` entry above with short bullets.
2. When resuming, first read this file + CONTEXT.md, then continue — never ask the user to re-explain settled points.
3. Also append one line to `CHANGELOG.md` and the master log for real work changes (not for pure planning).