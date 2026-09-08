# SanjivanAI — Personal AI Nurse

**Project:** "SanjivanAI" — A Personal AI Nurse for Every Family
**Competition:** Hack for Social Cause 2027 (VBYLD 2027), MyBharat / MoYAS + IIT Bombay
**Owner:** Samrat (teacher trainee, non-technical — explain plainly)
**This folder is the SINGLE SOURCE OF TRUTH — portable to any device/drive.**
**GitHub (private):** github.com/EternalFlames131/SanjivanAI

## Idea in one line
Affordable health monitoring + on-time medicines + automatic emergency help — at **home and in hospital "Virtual Ward" rooms** — using **wearables plus privacy-first camera zones**, so patients are watched even when no one is in the room.

## Deadline (CRITICAL)
**Last submission date: 15 October 2026** (window 1 Sep – 15 Oct 2026). Registration open on mybharat.gov.in.
State hackathon: 16 Oct – 30 Nov 2026 · National showcase: 10–12 Jan 2027, New Delhi.
Full rules: `references/hsc_guidelines_summary.md`

## What we must eventually submit
1. Problem Statement (state-specific, West Bengal)
2. 6–7 slide Presentation Deck (≤10 MB, incl. AI-tools disclosure)
3. Working Prototype — GitHub repo (⚠️ must be PUBLIC before 15 Oct; MIT, README, architecture, sample data)
4. Demo Video 3–5 min, ≥720p, ≤80 MB
5. Annexure 1 self-declaration + ID proofs

## Folder layout (portable brain)
- `docs/` — `SanjivanAI_Concept_Document_v1.1.pdf` (9 pages) + `source/` (the HTML that builds it)
- `prototype/` — app code (to build: responsive web app PWA + Node backend)
- `video/` — demo video + script (to build)
- `references/` — HSC rules summary, notes
- `tools/` — `build_pdf.ps1` + `verify_pdf.py` (rebuild/check the PDF anywhere)
- `opencode-config/` — backup of global opencode config + master activity log
- `AGENTS.md` — auto-loaded context (see `CONTEXT.md` for the full snapshot)
- `CHANGELOG.md` — running log for this project

## How to continue from any device (drive-only workflow)
1. Carry this folder (USB stick or a synced cloud folder like OneDrive/Dropbox).
2. On the other PC (opencode already installed): boot opencode and open THIS folder — `AGENTS.md` loads the project context automatically.
3. **No setup command to remember:** the first time opencode opens this folder on any PC, it **automatically runs** `tools\setup.ps1` (installs Python/pypdf/Edge/Git if missing, locks this repo to SanjivanAI's GitHub, enables auto-push, checks GitHub login). You'll just see one line: "Auto-setup completed on this device."
4. Work on files, then **commit** (`git add -A`, `git commit -m "message"`) — **push to GitHub happens automatically** (auto-push hook, this project only). If offline, the commit is safe; run `git push` later.
5. Rebuild the PDF after editing the HTML: `pwsh -File tools\build_pdf.ps1`.

## Safety (multiple-repo guarantee)
- Everything here is scoped to **this project only**. The auto-push hook checks, on every commit, that this folder's git `origin` is exactly `EternalFlames131/SanjivanAI` — if not, it does nothing. Your **other GitHub repos and global git settings are never touched**.
- You have multiple repos on GitHub; this folder will never push to any other one, even if copied somewhere else.

## Key decisions made
- Theme: Healthcare, Wellbeing & Service Delivery + Elderly Care & Healthy Ageing
- Two monitoring layers: wearable vitals + camera zones (falls/out-of-bed); camera AI on-device, **no video stored**
- Hospital "Virtual Ward" mode with nurse-station view
- Format: responsive web app (PWA); Android packaging possible later
- Prototype honesty: vitals/camera/billing simulated; dashboard, medicines, rules engine, alerts, escalation, multilingual UI = fully real; not a medical device (disclaimer included)
- Business model: affordable home plans + hospital per-bed B2B; low-cost "Made in India" monitor roadmap
- Extra winning points: DPI alignment (ABHA/tele-MANAS), vernacular + offline + SMS fallback, privacy-by-design, SDG 3, climate alerting, honest AI disclosure

## Open items
- Final team name, up to 3 members, AISHE institution confirmation, MyBharat individual registrations
- Build the working prototype in `prototype/` (next big step)
- Problem statement sheet, 6–7 slide deck, demo video, Annexure 1