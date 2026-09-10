# ReJivan — Project CONTEXT (full snapshot)

> This is the "brain" of the project. Update it whenever things change.
> Companion files: `README.md` (overview) and `AGENTS.md` (auto-loaded by opencode in this folder).

## One-line idea
A personal Prajñā nurse for every family — affordable health monitoring with on-time medicines and automatic emergency help, working **at home and in hospital "Virtual Ward" rooms** using **wearables PLUS privacy-first camera zones**.

## Competition & deadline (do not forget)
- **Hack for Social Cause 2027** (VBYLD 2027), MoYAS + IIT Bombay.
- **LAST SUBMISSION DATE: 15 October 2026.** Window: 1 Sep – 15 Oct 2026.
- State hackathon: 16 Oct – 30 Nov 2026 · IIT-B screening to 36 finalists: 1–15 Dec · National showcase: 10–12 Jan 2027, New Delhi.
- Full rules: `references/hsc_guidelines_summary.md`.

## What exists today
- **NATIVE OFFLINE ANDROID APP (NEW 2026-09-09):** `app-android/` — a true native Kotlin + Jetpack Compose Android app, FULLY FUNCTIONAL OFFLINE (no server/network/API needed — all logic is embedded). Ported 1:1 from the web prototype: VitalSimulator, RulesEngine, CameraZoneEngine, AlertEngine (emergency-call chain), DemoData, MedStore (SharedPreferences offline persistence). UI tabs: Login + Dashboard / Medicines / Alerts / Ward / Camera. Built debug APK (16 MB, package com.rejivan.app, SDK 26–36) → `Download\ReJivan_v1.0.apk` (also app-android\app\build\outputs\apk\debug\app-debug.apk). Verified on Pixel 8 emulator: installs, launches, focused window, app process stable (no crash); login screen confirmed rendering. Build needs JDK 21 (AGP 8.7.3 → Gradle 8.11.1 not compatible with Android Studio's bundled JDK 25). **STANDING RULE: any user-requested change must be applied to BOTH this native app AND the web app (they share logic but not code — mirrored manually).**
- Concept document PDF (v1.1, 9 pages) in `docs/` — built from `docs/source/ReJivan_doc_source.html`. **Auto-updates on every commit** (`.githooks/pre-commit` rebuilds + injects a live Real/Simulated status snapshot parsed from `prototype/server.js` + `docs/features.json` + build date).
- PDF pipeline fully portable from this drive: `tools/build_pdf.ps1` (Edge headless) + `tools/verify_pdf.py` (page/content checks).
- **Working prototype in `prototype/` (built 2026-09-09, SERVERLESS-READY):** Node + Express web app that runs identically on a laptop AND on Vercel (deterministic, stateless engine — every request computes vitals/alerts/calls/camera from the wall clock; no background loop). **Login required** — each account sees only its own registered patients (real per-user data isolation; scrypt-hashed passwords; stateless HMAC-signed tokens). Live vitals dashboard, medicines (add/take/delete), real rules engine + alerts + escalations, **automatic emergency-call chain** (family → backup → 108/112; REAL trigger/priority/retry logic, SIMULATED placement), Virtual Ward nurse view, privacy-first camera-zone feed + **live camera preview** (privacy-safe simulated metadata, nothing recorded/stored), 5-language UI. Simulated parts clearly labelled (vitals data, camera events, SMS/WhatsApp, live preview).
  - Run: `cd prototype && npm start` → **http://localhost:8080** (server currently LIVE; demo anchored in **Andaman & Nicobar Islands** — homes at Junglighat, Port Blair + Hut Bay, Little Andaman; Virtual Ward at GB Pant Hospital, Port Blair; region shown on every dashboard + the PDF). Vercel entry: `prototype/api/index.js` + `prototype/vercel.json`. **Auto-deploy ENABLED** (post-commit hook deploys `prototype\` to Vercel production whenever the CLI is logged in; one-time `vercel login github` still pending — device flow: https://vercel.com/oauth/device?user_code=DHLK-VNLG).
  - Demo logins: asharma@demo.in / rprakash@demo.in / wardnurse@demo.in (password: demo123).
- **PUBLIC GitHub repo (DONE 2026-09-09):** `github.com/EternalFlames131/ReJivan` (user `EternalFlames131`), branch `main`. ✅ HSC public-repo requirement now satisfied.
- Copy of owner's opencode global config + running activity log: `opencode-config/`.

## Key decisions made (so far)
1. **Theme:** Healthcare, Wellbeing & Service Delivery + Elderly Care & Healthy Ageing.
2. **Format:** responsive web app (PWA) so judges click it live; Android packaging later. Node + Express + JSON (SQLite optional later). Alerting via email/SMS/WhatsApp APIs (stubbed/demo).
3. **Two monitoring layers:** wearables/At-Home Monitor (vitals) + CCTV-style room cameras (falls, out-of-bed, low activity → alert-only).
4. **Hospital mode "Virtual Ward":** nurse-station view, bed/room list, priority alert queue.
5. **Privacy-first by design:** camera intelligence runs on-device, NO video recorded/stored, consent-based, DPDP-aligned — this is a highlighted winning point.
6. **Prototype honesty:** vitals + camera events + billing + call/SMS placement **simulated**; dashboard, medicines, rules engine, alerts, escalation, emergency-call chain logic, data isolation + multilingual UI **fully real**. Safety disclaimer included (not a medical device; human-in-the-loop).
7. **Business model:** affordable home subscriptions (₹ family plans) + hospital/institutional per-bed "Virtual Ward" B2B. Roadmap: low-cost "Made in India" monitor (<₹5,000).
8. **Extras (winning points):** DPI alignment (ABHA, tele-MANAS 14416, Ayushman), vernacular + offline + feature-phone SMS fallback, SDG 3, heatwave/climate alerting, intelligence-tools disclosure honesty, measured impact story.
9. **Serverless-first architecture (2026-09-09, Vercel):** the prototype must run on Vercel for a permanent public URL (friend/judges can monitor 24/7). Engine is now a pure deterministic function of (patient, clock) — no long-running process; stateless HMAC auth tokens; JSON persistence is best-effort (real persistence → Postgres/Redis later). **Auto-deploy enabled**: post-commit hook runs `vercel deploy --prod` from `prototype\` whenever the CLI is logged in (disable via `.git/no-deploy` or `REJIVAN_NO_DEPLOY=1`). Demo is **anchored in Andaman & Nicobar Islands** (UT) — Samrat's home region and HSC state round.

## How to continue from any device (drive-only workflow)
1. Carry this folder (USB drive or a synced cloud folder like OneDrive/Dropbox).
2. On the device: opencode is already installed (per user). Open THIS folder with opencode — `AGENTS.md` inside loads the context automatically.
3. **Setup is automatic - nothing to type:** at the start of every session opencode checks for the per-computer marker `tools\.setup-done-<PC>.txt`; if missing it runs `pwsh -ExecutionPolicy Bypass -File tools\setup.ps1` on its own (installs Python/pypdf/Edge/Git via winget, sets repo-local identity, locks remote to ReJivan ONLY, enables auto-push, checks GitHub login). The user just sees "Auto-setup completed on this device."
4. The folder is the single source of truth. Commit normally (`git add -A`, `git commit -m "..."`) — **push to GitHub is automatic** via `.githooks/post-commit` (this project only). If offline, the commit stays safe; run `git push` later. On a second device, `git pull` first if you want its latest state.
5. To rebuild the PDF after editing `docs/source/ReJivan_doc_source.html`, run `pwsh -File tools\build_pdf.ps1`.

## Open items / next steps
- [x] Build the working prototype in `prototype/` (core + dashboard done).
- [x] Add login + per-user data isolation + privacy-first live camera view (done d6dec9b).
- [x] Make repo PUBLIC (done 2026-09-09, github.com/EternalFlames131/ReJivan).
- [x] Auto-updating concept PDF (pre-commit hook, done 4a6dd9f).
- [x] Serverless-ready refactor for Vercel (done + verified locally 2026-09-09).
- [x] Prototype anchored in Andaman & Nicobar Islands (homes Port Blair/Little Andaman, ward = GB Pant Hospital) — loaded in UI + API + auto-PDF (2026-09-09).
- [x] Auto-push (git) + **AUTO-DEPLOY (Vercel)** enabled via setup.ps1 + post-commit hook (2026-09-09).
- [x] **LIVE PUBLIC WEBSITE** → https://sanjivanai.vercel.app (deployed + verified end-to-end 2026-09-09; domain re-pointed to the ReJivan build 2026-09-10; prototype-omega-self.vercel.app kept as a fallback alias): both demo logins, Andaman region + addresses, GB Pant Hospital ward, alerts/escalations/calls, camera zones, /api/simulation/status. Vercel Authentication (deployment protection) was switched OFF via API (`ssoProtection: null`) so the public URL needs no login.
- [x] User reviews the running prototype at http://localhost:8080 (Persistent server is LIVE now) — collect feedback.
- [ ] Round out prototype: "Add patient" flow for newly registered families; PWA offline service-worker (low priority).
- [ ] Wire escalation delivery to real APIs (SMS/WhatsApp/email) OR keep as clearly-labelled stubs.
- [ ] Real persistence for the cloud: small Postgres/Redis if the deployed site needs to remember new registrations/med logs across instances.
- [ ] Final team name + up to 3 members; confirm AISHE institution + individual registration on MyBharat portal.
- [ ] Write problem statement sheet (state-specific, **Andaman & Nicobar Islands — Samrat is based in A&N**, a Union Territory) in `docs/`.
- [ ] 6–7 slide deck with digital-tools disclosure; 3–5 min demo video (720p); Annexure 1 + student IDs.

## Standing rules (per owner's global AGENTS.md)
- After any change: append a timestamped line to `CHANGELOG.md` here AND to the master activity log `C:\Users\samra\OneDrive\Desktop\Opencode task\LOG.md` (snapshot kept in `opencode-config\LOG.md`).
- Owner is non-technical — explain plainly, avoid jargon.
- Full owner context (other projects, permissions) lives in global `~/.config/opencode/AGENTS.md`.