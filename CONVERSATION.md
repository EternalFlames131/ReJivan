# ReJivan — CONVERSATION & MEMORY LOG (auto-saved)

> Purpose: EVERY meaningful exchange, thought and decision about this project is saved here so the user NEVER has to re-explain anything. Sessions are resumed by reading this file + CONTEXT.md.
> Auto-save rule: after each turn that involves decisions/new info, a dated entry is appended by the assistant automatically.

---

## 2026-09-10 (Day 3 — FULL REBRAND executed; repo now EternalFlames131/ReJivan)

### What the user asked (and approved)
- Stop calling the project SanjivanAI → **ReJivan**. There must be **zero** mentions of the old name (or of "AI") in any stage of the project. The engine/intelligence is named **"Prajñā"** (exact spelling ñ + ā; generic phrasing = "intelligence" / "on-device Prajñā"). Asked explicitly if the GitHub repo should be renamed too → **Yes** (public, keep history).

### What was done (all verified)
- Bulk ladder script (temp) over 48 tracked files: SanjivanAI→ReJivan (all case/site variants), com.sanjivanai→com.rejivan, EternalFlames131/SanjivanAI→EternalFlames131/ReJivan, plus phrase ladder (taglines, "On-device AI"→"On-device Prajñā", "AI nurse"→"Prajñā nurse", "AI-tools disclosure"→"intelligence-tools disclosure", "camera AI"→"camera intelligence", etc.).
- Files renamed: docs/source/ReJivan_doc_source.html, docs/ReJivan_Concept_Document_v1.1.pdf (pre-commit hook auto-rebuilds it), Android java dirs com/sanjivanai→com/rejivan in BOTH app-android/ and the prototype/android Capacitor wrapper (MainActivity.java package now matches its path — a mismatch caught and fixed before commit).
- Careful manual edits after the bulk pass: doc HTML (5 "AI"→intelligence/digital-tools fixes + "an Prajñā nurse"→"a Prajñā nurse"), server.js ("On-device Prajñā"), camerazone.js (2), prototype README, root README, CONTEXT.md (2), CHANGELOG URL, CONVERSATION (4 edits incl. the Vercel two-account block rewritten with old URL removed), hsc_guidelines_summary.md (3), opencode-config/AGENTS.md, capacitor.config.json allowNavigation (prototype + android assets), dist/index.html + android-assets index.html API_BASE, public index.html ("On-device AI" line).
- lang.json (public): 20 edits — appName/tagline/disclaimer/live_banner/device_banner in HI/BN/TA/TE now ReJivan (रीजीवन/রিজিভন/ரிஜீவன்/రిజీవన్) with no AI phrasing; copied to dist/lang.json + android assets lang.json (key sets verified identical).
- .githooks/pre-commit + post-commit: rewritten manually (extensionless files, skipped by the ladder) — ReJivan messages, ALLOW/ALLOW_ALT URLs = EternalFlames131/ReJivan.git, REJIVAN_NO_DEPLOY, pre-commit root pattern `*ReJivan|*SanjivanAI`, PDF path docs/ReJivan_Concept_Document_v1.1.pdf, build log /tmp/rejivan_pdf_build.log.
- GitHub: `gh repo rename ReJivan --repo EternalFlames131/SanjivanAI --yes` → now **EternalFlames131/ReJivan** (PUBLIC, history preserved, old URL redirects). `git remote set-url origin` updated, verified via git ls-remote.
- Committed **949eb65** (49 files, incl. all renames). Pre-commit auto-rebuilt the PDF; post-commit auto-pushed + auto-deployed production. Live check: https://prototype-omega-self.vercel.app health 200, served HTML shows ReJivan, zero old-name/"AI" matches.
- Sanity: node --check OK on all 11 JS, JSON parse OK on 8 files, Kotlin package com.rejivan.app consistent; rg shows zero leftover "sanjivanai" (any case) except intentionally kept historical log lines in opencode-config/LOG.md (LP-Generator project) and the opencode.ai schema URL (false positive).

### Notes / decisions
- Live URL everywhere = **prototype-omega-self.vercel.app** (the old short sanjivanai.vercel.app URL removed from all docs — it belongs to a different account).
- Local disk folder is still literally "SanjivanAI" — FINE: hooks accept both names; user may rename the folder manually anytime (close opencode first).
- APK side already com.rejivan.app (native app-android Debug APK earlier at Downloads/ReJivan_v1.0.apk); Capacitor APK would need a rebuild for a fresh package name.
- The medical-grade model stack recommendations (NEWS2/MEWS now; MediaPipe pose→LSTM falls; COMPOSER/TREWS/DeepMind-AKI as validated-upgrade research) live in the Day-3 research section below.

### Open / next
- Nothing technical left. Optional later: rename the local disk folder, rebuild APKs under the new name, wire real NEWS2 rules into the engine.

---

## 2026-09-10 (Day 3 — medical-grade ML research; user called the project "ReJivan")

### What the user asked
Research genuinely medical-grade / clinically validated ML models (NOT general LLMs) for the monitoring engine (HR, SpO2, BP, temp, glucose wearables + privacy-first camera fall/out-of-bed/low-activity + alerts + auto emergency escalation). Categories: (1) early-warning/deterioration scores & ML deterioration models, (2) vital-sign time-series anomaly detection, (3) camera fall detection, (4) RPM ML-as-a-service with clinical validation, (5) on-device/edge runtimes, (6) multi-wearable sensor fusion + concept drift. User used the working name **"ReJivan"** — docs still say ReJivan; name change not yet applied (ASK before renaming everything).

### Key research conclusions (delivered in chat, full list)
- **Truly clinically validated + usable now:** deterministic NEWS2 / MEWS scoring (RCP UK standard; NEWS2 external validation AUC 0.898 for 24h deterioration; implementable offline, ~50 lines of rules, zero training). Glucose: do NOT build glucose ML — ingest FDA-cleared CGM alarms (FreeStyle Libre 3, Dexcom) instead.
- **Published + prospective outcome evidence (model code closed):** TREWS/TREWScore (Johns Hopkins, JAMA 2022, 5 hospitals, confirmed alerts → 3.3% absolute mortality reduction; AUC 0.83 septic shock) · COMPOSER (npj Digit Med 2021, conformal feed-forward NN, sepsis AUC 0.938–0.945; npj 2024 shows deployment reduced mortality) · DeepMind/Google AKI RNN (Nature 2019, AUC 92.1%; honest caveat — not released, later ATR paper had data-leakage).
- **FDA-cleared RPM/SaMD (all proprietary, use only as reference spirit):** Biofourmis Biovitals Analytics Engine (K183282, individualized vitals baseline, decompensation weeks ahead) · CLEW ICU (K200717/K233216, hemodynamic instability) · Tempus ECG-AF (K233549).
- **Fall detection — NOT clinically validated anywhere (lab-dataset validated only):** MediaPipe BlazePose + LSTM (95.2% acc / 100% recall, UR-Fall) · AFAR 1D-CNN (CPU real-time) · bimodal IMU+vision late-fusion (F1 97.3%, FPR 3.6%, ~20fps CPU, night-tested) · YOLOv8+MediaPipe (96% acc). ALL are on-device-friendly → fits privacy-first claim. Honest pitch: "research-validated on public datasets, edge-only".
- **Anomaly detection (research stage):** LSTM autoencoders (reconstruction error), VAE-IF (Escudero 2024, unsupervised ICU artifact detection), TS2Vec (AAAI'22), PyCaret/PyOD/Isolation Forest; icu-anomaly open repo (MIMIC III/IV). MIMIC requires credentialing — for the hack, use public UR-Fall + PhysioNet 2012 challenge.
- **Edge runtimes (choose to underpin architecture):** LiteRT (formerly TFLite) ~1MB, MediaPipe Tasks (pose), ONNX Runtime Mobile, OpenVINO (Intel boxes); so no video leaves the device. Cite COMPOSER's conformal "I don't know" as design-precedent for low false alarms.
- **Fusion + drift (research stage, great citation fuel):** VitalTrackAI-GatedFusion (Springer 2026, smartphone-edge, F1 0.90) · PECS ECG-PPG drift arbitration (arXiv 2026) · IoMT LSTM-AE + XGBoost fusion (Accuracy 99.76%, edge 84ms) · DOCTOR continual learning (drift adaptation).
- **Recommended ReJivan/ReJivan stack (2026-hackathon real):** NEWS2/MEWS rules layer (clinically grounded) + per-channel LSTM-AE anomaly scores (on-device) + MediaPipe pose→LSTM/1D-CNN fall classifier (on-device, keypoints only, no video) + CGM alarm ingestion + signal-quality-aware fusion + deterministic escalation ladder. Everything runs on a phone, offline, no cloud dependency.
- Honesty tiers to quote judges: (a) clinically validated/deployed (NEWS2), (b) clinically evidenced but closed-source (COMPOSER/TREWS — we replicate the *design pattern*, not the weights), (c) research-stage (anomaly/fall/fusion — label SIMULATED per existing rules).

---

## 2026-09-09 (Day 2, afternoon — reliability / critic counter-arguments)

### What the user asked
How to counter a critic who questions ReJivan's reliability: "How can we trust this with our family or any patient? What if something goes wrong? What are the precautions?"

### Response given (6 angles)
1. **Trust** — human-in-the-loop (nurse, not doctor), transparent clinical thresholds (no black box), 3-tier escalation ladder (no single point of failure).
2. **Crash / wrong readings** — graceful degradation (independent layers), alert deduplication prevents alarm-failure, honest labelling of simulated data + roadmap for hardware validation.
3. **Privacy breach** — zero video recorded/stored (on-device Prajñā, alert-only), consent-based, DPDP-aligned, more private than existing hospital CCTV.
4. **Emergency call failure** — family → backup → 108/112 with retries, SMS/feature-phone fallback for weak-internet areas like A&N outer islands.
5. **"Just a student project"** — two-layer monitoring (vitals + camera) nobody else combines, hospital + home dual use case, Andaman-specific offline/multilingual design.
6. **Concrete safeguards table** — scrypt hashing, per-user isolation, alert cooldown, escalation ladder, threshold transparency, no video storage, consent-based camera, SMS fallback, safety disclaimer.
7. **Closing pitch** — honesty about limits is a trust signal; judges reward self-aware teams.

---

## 2026-09-09 (Day 2, afternoon — public repo + serverless/Vercel refactor)

### What the user asked (in order)
1. Refactor the engine to be stateless so the prototype can run as a website on **Vercel** (user chose Vercel, on a public/different domain) for a friend to monitor.
2. **Make the GitHub repo PUBLIC** and give the link → done: **https://github.com/EternalFlames131/ReJivan** (now PUBLIC, branch main; HSC requirement satisfied — no longer a pending task).
3. "Did you save every last detail?" → this entry is that save.

### Repo made public
- `gh repo edit EternalFlames131/ReJivan --visibility public --accept-visibility-change-consequences` — verified PUBLIC before finishing.
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
- The user pivoted to a localhost login problem before finishing — root cause was **no server running** (test instances were killed), not a bug. Persistent server relaunched: `node server.js` in prototype\ → **http://localhost:8080** (asharma@demo.in / demo123).

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
- Deliverables: Problem Statement + 6–7 slide deck (≤10 MB, with intelligence-tools disclosure) + Working Prototype (PUBLIC GitHub repo, MIT, README, architecture, sample data) + Demo Video 3–5 min / ≥720p / ≤80 MB + Annexure 1 self-declaration & IDs.
- Stages: submission by 15 Oct → State hackathon 16 Oct–30 Nov (3 teams shortlist) → IIT-B screening 1–15 Dec → **36 national finalists**, National Showcase 10–12 Jan 2027 Delhi. Prizes ₹75k/50k/25k/15k/15k; finalists get ₹6k dev grant.
- Evaluation (6 params): Relevance · Technical Strength · Functionality · Creativity/Innovation · Social Cause Impact · Presentation & Team.

### 3. Idea selection
- User theme choice: **Healthcare & Wellbeing** (plus fits **Elderly Care & Healthy Ageing** — 2 themes deliberately).
- User's own idea (chose over my 4 suggestions): a "**personal Prajñā nurse**" — continuous monitoring, on-time medicines, family can care at home instead of hospital, automatic emergency signals to emergency services, no person needed on-site, affordable subscription.
- Verified feasibility: full hardware product = multi-year; **hackathon-realistic = working software prototype that SIMULATES sensors** and makes dashboard/meds/alerts/escalation real.

### 4. Name, folder, repo
- New working folder (started as "HSC Prajñā Nurse") → renamed **ReJivan** (user's choice), path `C:\Users\samra\OneDrive\Desktop\ReJivan`.
- Concept PDF built: **ReJivan_Concept_Document_v1.1.pdf** (9 pages) using Edge headless + HTML source (pipeline from owner's AGENTS.md).
- GitHub: **private repo created** `EternalFlames131/ReJivan` (account EternalFlames131), branch **main**. ⚠️ Must be made PUBLIC before 15 Oct (submission requirement).

### 5. Scope expansion (user's additions)
- Use case extended beyond home: **hospitals** where doctors/nurses can't always be present → **"Virtual Ward"** mode (nurse-station view, rooms, priority alerts).
- Monitoring NOT only wearables → **CCTV-style room cameras**: fall detection, out-of-bed, low activity; video can also estimate heart/resp rate contact-free. **Privacy-first design is mandatory** (on-device Prajñā, NO video recorded/stored, consent, DPDP-aligned) — positioned as a winning point.
- Prototype honesty: camera events + vitals + billing **simulated**; dashboard, medications, rules engine, alerts, escalation, multilingual **fully real**.

### 6. Portability ("perfect folder" + drive)
- Folder made **self-contained** → works from any drive: `docs/source` (PDF HTML), `tools/build_pdf.ps1` + `verify_pdf.py`, `references/hsc_guidelines_summary.md`, `opencode-config/` (backup of owner's global opencode AGENTS.md, opencode.jsonc, master LOG.md), plus README/CONTEXT/AGENTS.
- Removable drive F: → full copy at `F:\ReJivan` (mirrored, includes .git). F: = "live" folder with opencode on the other device.

### 7. Automation & safety (multi-repo protection)
- `setup.ps1`: one-time auto-setup per PC — installs missing Python/pypdf/Edge/Git via winget, sets repo-LOCAL git identity, locks remote to ReJivan ONLY, enables auto-push, checks GitHub login, tests PDF pipeline, writes per-PC marker `tools\.setup-done-<PC>.txt`.
- **Auto-push hook** `.githooks/post-commit`: after every commit pushes to ReJivan repo. **Hardened:** only fires when origin == ReJivan URL; otherwise does nothing (tested with a throwaway repo — other repos cannot be touched). Global git settings untouched (verified).
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
  - **Live camera (UI REAL, feed SIMULATED + labelled):** `View live` button on each camera zone → modal with animated privacy-safe room preview (canvas) + person/motion/lighting metadata from `/api/camera-zones/:id/live`. On-device Prajñā framing — **no video recorded or stored**, consistent with privacy-first promise. Connect/Disconnect + live clock.
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
  - New `.githooks/pre-commit` hook: before EVERY commit it rebuilds `docs\ReJivan_Concept_Document_v1.1.pdf` and stages it, so the PDF can never go stale. If Edge fails (e.g. PDF open), it warns but never blocks the commit.
  - New `docs/features.json` — canonical machine-readable feature list + demo accounts + "real/simulated" status + notes.
  - `tools/build_pdf.ps1` upgraded: injects an auto-generated **"Live Prototype Status"** section — feature table from features.json, plus the REAL / SIMULATED lists parsed LIVE out of `prototype/server.js` (so the document always mirrors the actual code), plus build date. Writes generated HTML to Temp\opencode, renders via Edge headless, verifies via pypdf.
  - Placeholders added in `docs/source/ReJivan_doc_source.html` (`{{STATUS_ROW}}`, `{{BUILD_DATE}}`, `<!--AUTO:PROTOTYPE_SNAPSHOT-->`).
  - Verified: PDF rebuilds to 9 pages, snapshot content confirmed in text (demo accounts, emergency call chain, Tamil/Telugu languages, etc.). Hook fired automatically on the commit itself. Auto-pushed (4a6dd9f).
- **How it works for the user:** no action needed — any future commit (added feature, fix, memory save) automatically refreshes the PDF to match.
- One maintenance note: when a genuinely NEW feature ships, its row should be added to `docs/features.json` once; the rest (status lists, dates, accounts) updates itself.
---

## 2026-09-09 (Day 2, late — two corrections: competition levels + state = Andaman & Nicobar)

### 1) Competition levels (user's doubt) — NO district round
- Verified from mybharat.gov.in/vbyld-2027: HSC 2027 = **4 stages**:
  1. **Institutional** — internal hackathon at your college; ONE winning team nominated per institution.
  2. **State/Regional** — 23 Oct – 5 Nov 2026, top ~3 shortlisted per state/UT.
  3. **National** — IIT Bombay screening → **36 finalists** + mentorship (10 Nov – 31 Dec 2026).
  4. **National Showcase** — VBYLD 2027, New Delhi, 10–12 Jan 2027.
- Practical: also enter our own college's internal hackathon so the institution nominates ReJivan.
- `references/hsc_guidelines_summary.md` updated.

### 2) Samrat is based in **Andaman & Nicobar Islands** (UT) — not West Bengal!
- All "West Bengal" references corrected → **Andaman & Nicobar Islands (UT)**:
  - `README.md` (problem statement state-specific A&N), `CONTEXT.md` (open item), `hsc_guidelines_summary.md` (file naming example `AndamanNicobar_ReJivan_...` + UT note), `docs/source/ReJivan_doc_source.html` (Team row, "Hack Local" context section, Relevance cell, naming example).
- New "Hack Local" narrative angle for the concept doc (A&N, 36 inhabited islands): one major referral hospital (GB Pant Hospital, Port Blair), specialists centred on the main island, PHCs/Cottage Hospitals on outer islands, sea/air travel for specialist care, seasonal connectivity gaps, split island–mainland families → ReJivan's offline-friendly, multilingual, SMS-fallback, remote-monitoring design fits perfectly.
- PDF will be auto-rebuilt with these edits on next commit (pre-commit hook).

---

## 2026-09-09 (Day 2, late — reliability safeguards + medical wearables research)

### What the user asked
1. How to counter critics who question reliability ("how can we trust this with our family").
2. Smartwatches aren't medical-grade — what are proper medical wearables and how to integrate them.
3. Build actual safeguards INTO the prototype beforehand.

### What was built (IN PROGRESS — not yet tested E2E)
- **NEW FILE: `prototype/reliability.js`** — full reliability layer with 7 safeguards:
  1. Data validation — physiologically impossible readings rejected (HR >250, SpO2 <50, etc.)
  2. Confidence scoring — each reading rated 0–100 by device tier (medical/consumer/simulated) + edge penalty
  3. Consecutive-reading verification — danger must persist 2+ readings before emergency escalation
  4. Sensor heartbeat/disconnect detection — alert if no data for >2 minutes
  5. Alert rate-limiting — max 5 alerts per patient per 5 minutes (prevents alert fatigue)
  6. Immutable audit trail — every action logged with timestamp + reason
  7. Graceful degradation — system works with partial sensors, warns family
- **UPDATED: `prototype/simulator.js`** — added `deviceTier: "simulated"` to generated vitals output
- **UPDATED: `prototype/rules.js`** — added `confirmedDangerLabels()` using consecutive verification
- **UPDATED: `prototype/server.js`** — wired reliability into alert pipeline: validateVitals before rules eval, confirmedDangerLabels (only confirmed danger triggers escalation), rateLimitCheck before push, auditEvent on every escalation, new `/api/audit-log` + `/api/device-health` endpoints, updated `/api/simulation/status` with reliability safeguards list
- **UPDATED: `prototype/public/index.html`** — patient cards now show confidence score + reliability bar + degraded sensor warning; null vitals shown as "—" with danger badge
- **UPDATED: `prototype/public/lang.json`** — added i18n keys (confidence, high/medium/low reliability, confirmed, suspect, sensor offline, audit trail, device health) in all 5 languages

### Status
- Module-level test PASSED (validateVitals, confidence, degradation, audit all work)
- Server loads OK (19 routes including 2 new)
- NOT yet tested E2E (full login + dashboard + alerts flow with reliability) — that's the next step
- Medical wearables research still pending

### Reliability safeguards the user can cite to judges
- "7 built-in safeguards: validation, confidence scoring, consecutive verification, sensor heartbeat, rate limiting, audit trail, graceful degradation"
- "Danger must persist across 2+ consecutive readings before emergency escalation — single glitches are logged but not acted on"
- "Every action is in an immutable audit trail — accountability for every alert and call"

---

## 2026-09-10 (Day 3 — Vercel account note, important)

### Auto-deploy now goes to a DIFFERENT URL (two Vercel accounts exist)
- The medical-device build auto-deployed to the "prototype" project at **https://prototype-omega-self.vercel.app** (production, fully verified E2E: login, devices, 80% confidence, 11-device catalogue).
- The OLD short-account URL (a different Vercel account, still under the former project name) STILL WORKS but serves the PREVIOUS build (no medical devices) — and the Vercel API says "you don't have access to it" from the current CLI account.
- Root cause: there are TWO Vercel accounts. The current CLI login (samrat1312004-1117 / samrat1312004-1117s-projects team) owns projects: prototype, lpgenerator-new, lp-generator-v2, v0-tourism-app-prototype — that is where `prototype-omega-self.vercel.app` lives. The old short URL lives in a DIFFERENT account (likely the `vercel login github` device-flow from 2026-09-09, code DHLK-VNLG, under the GitHub identity).
- Impact: the post-commit auto-deploy hook now updates prototype-omega-self.vercel.app. If Samrat wants the new build on the old short URL, he must log into that other account once (`vercel login`) and deploy — otherwise keep using prototype-omega-self.vercel.app.

---

## 2026-09-10 (Day 3 — medical device integration + reliability explained)

### What the user asked
"Sprang about the precautions taken if any software or hardware issue occur what is the reliability? and also this project can't be depended on smartwatches or market-level smart wearables, we need proper medical wearable devices that are better reliable and more accurate, what are those and how can i integrate it with the project and also make integration with the project"

### 1) Reliability answer (7 built-in safeguards — ALL coded in the prototype)
1. **Data validation** — rejects physiologically impossible readings (no 0 or 300 heart rate)
2. **Confidence scoring** — every reading rated 0–100 by device quality + how normal the value is
3. **Consecutive verification** — danger must persist 2+ readings before emergency escalation (single glitch = logged, NOT acted on)
4. **Sensor heartbeat** — if a device stops reporting for 2+ minutes → "device may be disconnected" alert
5. **Rate limiting** — max 5 alerts/patient/5 minutes (prevents alert fatigue)
6. **Audit trail** — every action (alert, escalation, call) logged permanently, cannot be deleted
7. **Graceful degradation** — if one sensor fails, system keeps working with remaining sensors + warns family

### 2) Medical wearables research (NO smartwatches — proper FDA/CDSCO/CE devices)
- **ECG/HR:** SanketLife 12-Lead (Agatsa Pune, CDSCO Class B, ₹5,000, Made in India, 98.5% accuracy) · Hexoskin (FDA)
- **SpO2:** ChoiceMMed MD300C228 (FDA 510(k), ₹4,000) · Lepu AP-10 wrist (FDA+CE, ₹10,000)
- **BP:** Omron HEM-7156T (FDA/CDSCO, ₹4,500) · Biobeat chest patch (FDA, cuffless 13 vitals, aspirational)
- **Temperature:** TempTraq patch (FDA Class II, ₹2,000) · AION TempShield (FDA, 90-day)
- **Glucose (CGM):** FreeStyle Libre 3 (FDA+CDSCO, ₹4,670/sensor) · GlucoRx Vixxa 2 (CDSCO, ₹3,200)
- **Indian multi-parameter:** H360 Health360 (Medilogy, CDSCO, ₹7,000, IIT-designed) · SanketLife
- **Key pitch point:** NO single device covers all 5 vitals today — ReJivan's value = a Prajñā platform that aggregates multiple medical devices into one unified dashboard.

### 3) Integration BUILT (per user request)
- **NEW FILE `prototype/medical-devices.js`:** 11-device catalogue (all medically approved), per-patient device registry (connection, battery, signal, last-seen), medical confidence boost (simulated 57% → medical 80%), simulated BLE heartbeat.
- **server.js:** 3 new endpoints (/api/devices, /api/devices/catalogue, /api/devices/:patientId); device data merged into patient snapshots; /api/simulation/status now reports medical-device support.
- **index.html:** NEW "Medical Devices" tab — per-patient connected-device rows (connected/offline, battery bars, signal bars, Made-in-India badge) + the full supported-device catalogue with prices/approvals. Patient cards now show connected-device chips.
- **lang.json:** all new device keys translated to EN/HI/BN/TA/TE.
- **docs/features.json:** added "Medical device integration" + "Reliability safeguards" entries.
- **Verified E2E:** both demo logins, catalogue (11 devices, 2 Indian-made), device registry (3 per home patient, 2–3 per ward), confidence 80% on medical tier, UI loads.

### Honest labelling (unchanged)
- Device DATA is still simulated (BLE connectivity is simulated to mimic real hardware). The device profiles, approvals, prices and integration architecture are REAL. In production the BLE/API connections would stream real readings from real hardware.

### To-do after this save
- Show user the new Medical Devices tab (http://localhost:8080 → login) — explain how the confidence jumps to 80% and the catalogue is real.
- Optionally: apply the same medical-device module to the native Android app (app-android/) per standing mirror rule.

---

## Standing auto-save rules (do this every session)
1. After any turn with decisions/thoughts/new info, append a `## YYYY-MM-DD (Day N — note)` entry above with short bullets.
2. When resuming, first read this file + CONTEXT.md, then continue — never ask the user to re-explain settled points.
3. Also append one line to `CHANGELOG.md` and the master log for real work changes (not for pure planning).
