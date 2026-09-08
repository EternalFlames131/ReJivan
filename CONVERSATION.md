# SanjivanAI — CONVERSATION & MEMORY LOG (auto-saved)

> Purpose: EVERY meaningful exchange, thought and decision about this project is saved here so the user NEVER has to re-explain anything. Sessions are resumed by reading this file + CONTEXT.md.
> Auto-save rule: after each turn that involves decisions/new info, a dated entry is appended by the assistant automatically.

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

## Standing auto-save rules (do this every session)
1. After any turn with decisions/thoughts/new info, append a `## YYYY-MM-DD (Day N — note)` entry above with short bullets.
2. When resuming, first read this file + CONTEXT.md, then continue — never ask the user to re-explain settled points.
3. Also append one line to `CHANGELOG.md` and the master log for real work changes (not for pure planning).