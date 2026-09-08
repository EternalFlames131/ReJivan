# SanjivanAI — Project AGENTS.md (auto-loaded by opencode when working in this folder)

**Owner:** Samrat — teacher trainee, NON-TECHNICAL. Explain in plain steps, avoid jargon.

## The project
SanjivanAI = "A Personal AI Nurse for Every Family" — Hack for Social Cause 2027 (VBYLD 2027, MoYAS + IIT Bombay).
- Idea: remote health monitoring + on-time medicines + automatic emergency help.
- Works **at home AND in hospitals** ("Virtual Ward") — rooms where doctors/nurses can't always be present.
- Two monitoring layers: **wearables/At-Home Monitor** (vitals: HR, SpO2, BP, temp) + **privacy-first camera zones** (falls, out-of-bed, low activity — NO video recorded/stored).
- **Deadline: 15 October 2026** (submission window 1 Sep – 15 Oct 2026). National showcase 10–12 Jan 2027, Delhi.

## Where everything lives (this folder = single source of truth, works from any drive)
- `docs/SanjivanAI_Concept_Document_v1.1.pdf` — concept doc, built from `docs/source/SanjivanAI_doc_source.html` via `tools/build_pdf.ps1` (needs Edge + Python).
- `references/hsc_guidelines_summary.md` — competition rules/deadlines.
- `opencode-config/` — backup of owner's global opencode config + activity log.
- `prototype/` (to build) · `video/` (to build) · `CONTEXT.md` (full snapshot) · `README.md` (overview) · `CHANGELOG.md` (project log).
- GitHub (PRIVATE, branch main): `github.com/EternalFlames131/SanjivanAI`. ⚠️ Make PUBLIC before 15 Oct for submission.

## First use on a NEW device (one-time, ~2 min)
Run: `pwsh -File tools\setup.ps1`
It checks/installs Python + pypdf + Edge + Git (via winget, asks first), sets LOCAL git identity for this repo only, locks the remote to **this** repo's GitHub, enables auto-push, checks GitHub login, and tests the PDF pipeline.

## Auto-push (enabled for this project only)
- After every `git commit`, a hook **automatically pushes** to `github.com/EternalFlames131/SanjivanAI` (branch main). It never touches any other repo.
- Commits themselves are still deliberate (git add + git commit).
- If offline/not logged in, the push is skipped but the commit is safe — run `git push` later.

## Rules
- Owner is non-technical: plain language, no unexplained jargon.
- After finishing/substantial work, append a timestamped line to `CHANGELOG.md` (and the master log `C:\Users\samra\OneDrive\Desktop\Opencode task\LOG.md`). Then commit (auto-push takes care of GitHub).
- Read `CONTEXT.md` first when resuming work.
- Prototype honesty: always label what is REAL vs SIMULATED in the demo.