# SanjivanAI — Project CONTEXT (full snapshot)

> This is the "brain" of the project. Update it whenever things change.
> Companion files: `README.md` (overview) and `AGENTS.md` (auto-loaded by opencode in this folder).

## One-line idea
A personal AI nurse for every family — affordable health monitoring with on-time medicines and automatic emergency help, working **at home and in hospital "Virtual Ward" rooms** using **wearables PLUS privacy-first camera zones**.

## Competition & deadline (do not forget)
- **Hack for Social Cause 2027** (VBYLD 2027), MoYAS + IIT Bombay.
- **LAST SUBMISSION DATE: 15 October 2026.** Window: 1 Sep – 15 Oct 2026.
- State hackathon: 16 Oct – 30 Nov 2026 · IIT-B screening to 36 finalists: 1–15 Dec · National showcase: 10–12 Jan 2027, New Delhi.
- Full rules: `references/hsc_guidelines_summary.md`.

## What exists today
- Concept document PDF (v1.1, 9 pages) in `docs/` — built from `docs/source/SanjivanAI_doc_source.html`.
- PDF pipeline fully portable from this drive: `tools/build_pdf.ps1` (Edge headless) + `tools/verify_pdf.py` (page/content checks).
- **Working prototype in `prototype/` (built 2026-09-09):** Node + Express web app. **Login required** — each account sees only its own registered patients (real per-user data isolation; scrypt-hashed passwords). Live vitals dashboard, medicines (add/take/delete), real rules engine + alerts + escalations, Virtual Ward nurse view, privacy-first camera-zone feed + **live camera preview** (privacy-safe simulated metadata, nothing recorded/stored), 5-language UI. Simulated parts clearly labelled (vitals data, camera events, SMS/WhatsApp, live preview). Run: `cd prototype && npm start` → http://localhost:8080. Demo logins: asharma@demo.in / rprakash@demo.in / wardnurse@demo.in (password: demo123).
- Private GitHub repo: `github.com/EternalFlames131/SanjivanAI` (user `EternalFlames131`), branch `main`.
  - ⚠️ **Must be made PUBLIC before 15 Oct 2026** (HSC requires public repo link).
- Copy of owner's opencode global config + running activity log: `opencode-config/`.

## Key decisions made (so far)
1. **Theme:** Healthcare, Wellbeing & Service Delivery + Elderly Care & Healthy Ageing.
2. **Format:** responsive web app (PWA) so judges click it live; Android packaging later. Node + Express + SQLite/JSON; alerting via email/SMS/WhatsApp APIs.
3. **Two monitoring layers:** wearables/At-Home Monitor (vitals) + CCTV-style room cameras (falls, out-of-bed, low activity → alert-only).
4. **Hospital mode "Virtual Ward":** nurse-station view, bed/room list, priority alert queue.
5. **Privacy-first by design:** camera AI runs on-device, NO video recorded/stored, consent-based, DPDP-aligned — this is a highlighted winning point.
6. **Prototype honesty:** vitals + camera events + billing **simulated**; dashboard, medicines, rules engine, alerts, escalation, multilingual UI **fully real**. Safety disclaimer included (not a medical device; human-in-the-loop).
7. **Business model:** affordable home subscriptions (₹ family plans) + hospital/institutional per-bed "Virtual Ward" B2B. Roadmap: low-cost "Made in India" monitor (<₹5,000).
8. **Extras (winning points):** DPI alignment (ABHA, tele-MANAS 14416, Ayushman), vernacular + offline + feature-phone SMS fallback, SDG 3, heatwave/climate alerting, AI-tools disclosure honesty, measured impact story.

## How to continue from any device (drive-only workflow)
1. Carry this folder (USB drive or a synced cloud folder like OneDrive/Dropbox).
2. On the device: opencode is already installed (per user). Open THIS folder with opencode — `AGENTS.md` inside loads the context automatically.
3. **Setup is automatic - nothing to type:** at the start of every session opencode checks for the per-computer marker `tools\.setup-done-<PC>.txt`; if missing it runs `pwsh -ExecutionPolicy Bypass -File tools\setup.ps1` on its own (installs Python/pypdf/Edge/Git via winget, sets repo-local identity, locks remote to SanjivanAI ONLY, enables auto-push, checks GitHub login). The user just sees "Auto-setup completed on this device."
4. The folder is the single source of truth. Commit normally (`git add -A`, `git commit -m "..."`) — **push to GitHub is automatic** via `.githooks/post-commit` (this project only). If offline, the commit stays safe; run `git push` later. On a second device, `git pull` first if you want its latest state.
5. To rebuild the PDF after editing `docs/source/SanjivanAI_doc_source.html`, run `pwsh -File tools\build_pdf.ps1`.

## Open items / next steps
- [x] Build the working prototype in `prototype/` (core + dashboard + simulator DONE; PWA offline service-worker still pending — low priority).
- [x] Add login + per-user data isolation + privacy-first live camera view (both DONE via commit d6dec9b).
- [ ] User reviews the running prototype at http://localhost:8080 (login with a demo account); collect feedback.
- [ ] "Add patient" flow for newly registered families (currently only the 3 demo accounts own the 4 seeded patients).
- [ ] Wire escalation delivery to real APIs (SMS/WhatsApp/email) OR keep as clearly-labelled stubs for the demo.
- [ ] Final team name + up to 3 members; confirm AISHE institution + individual registration on MyBharat portal.
- [ ] Write problem statement sheet (state-specific, West Bengal) in `docs/`.
- [ ] 6–7 slide deck with AI disclosure; 3–5 min demo video (720p); Annexure 1 + student IDs.
- [ ] Make repo public + push everything before 15 Oct 2026.

## Standing rules (per owner's global AGENTS.md)
- After any change: append a timestamped line to `CHANGELOG.md` here AND to the master activity log `C:\Users\samra\OneDrive\Desktop\Opencode task\LOG.md` (snapshot kept in `opencode-config\LOG.md`).
- Owner is non-technical — explain plainly, avoid jargon.
- Full owner context (other projects, permissions) lives in global `~/.config/opencode/AGENTS.md`.