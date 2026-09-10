# ReJivan — Project AGENTS.md (auto-loaded by opencode when working in this folder)

**Owner:** Samrat — teacher trainee, NON-TECHNICAL. Explain in plain steps, avoid jargon.

## SESSION-OPEN PROTOCOL (auto-READ — do this FIRST, always, without being asked)
- The instant a session starts in this folder, BEFORE any other work or reply work: **Read `CONVERSATION.md` and `CONTEXT.md`** (and, if the user mentions it, `README.md`). This is non-optional — it is how the project memory loads automatically.
- Then acknowledge in ONE short line, e.g. "Loaded project memory (current: DAY N, last topic: ...)." — do NOT dump the whole history back at the user.
- If markers/setup are pending from the AUTO-SETUP rule below, handle that too before project work.
- Rule applies on every device, every session, so the user never has to explain anything twice.

## The project
ReJivan = "A Personal Nurse for Every Family" — Hack for Social Cause 2027 (VBYLD 2027, MoYAS + IIT Bombay).
- Idea: remote health monitoring + on-time medicines + automatic emergency help.
- Works **at home AND in hospitals** ("Virtual Ward") — rooms where doctors/nurses can't always be present.
- Two monitoring layers: **wearables/At-Home Monitor** (vitals: HR, SpO2, BP, temp) + **privacy-first camera zones** (falls, out-of-bed, low activity — NO video recorded/stored).
- **Deadline: 15 October 2026** (submission window 1 Sep – 15 Oct 2026). National showcase 10–12 Jan 2027, Delhi.
- **Region: Andaman & Nicobar Islands (UT)** — Samrat's home; HSC state/regional round = A&N (23 Oct – 5 Nov 2026). No district round: Institution → State/Regional → National (IIT-B, 36 finalists) → National Showcase. The prototype demo is anchored in A&N (homes at Junglighat/Little Andaman, Virtual Ward = GB Pant Hospital, Port Blair).

## Where everything lives (this folder = single source of truth, works from any drive)
- `docs/ReJivan_Concept_Document_v1.1.pdf` — concept doc, built from `docs/source/ReJivan_doc_source.html` via `tools/build_pdf.ps1` (needs Edge + Python).
- `references/hsc_guidelines_summary.md` — competition rules/deadlines.
- `opencode-config/` — backup of owner's global opencode config + activity log.
- `prototype/` (to build) · `video/` (to build) · `CONTEXT.md` (full snapshot) · `README.md` (overview) · `CHANGELOG.md` (project log).
- GitHub (**PUBLIC**, branch main): `github.com/EternalFlames131/ReJivan`.

## AUTO-SAVE CONVERSATION MEMORY (so the user never re-explains)
- **RULE (always):** After EVERY turn where decisions, thoughts, or new info were exchanged, append a short dated entry to `CONVERSATION.md` (the project memory). When resuming later, read `CONVERSATION.md` + `CONTEXT.md` first and continue from there — never make the user repeat previously settled details.
- The user has repeatedly asked for this; treat it as non-optional.

## AUTO-SETUP (automatic - the assistant does it, the user never types a command)
- **RULE (always):** At the start of EVERY session in this folder, check whether a marker file `tools\.setup-done-<this-computer-name>.txt` exists. If it does NOT exist, RUN automatically, without asking and without the user requesting it:
  `pwsh -ExecutionPolicy Bypass -File tools\setup.ps1`
  Then tell the user one short line: "Auto-setup completed on this device." (or list any yellow reminders).
- Do not block waiting for permission: setup is safe (installs missing tools via winget, locks remote to THIS repo, enables auto-push + auto-deploy). Never skip it silently.
- If the user works from a USB drive, the marker is per-computer, so each new PC triggers setup exactly once.

## First use on a NEW device (one-time, ~2 min)
Run: `pwsh -File tools\setup.ps1`
It checks/installs Python + pypdf + Edge + Git (via winget, asks first), sets LOCAL git identity for this repo only, locks the remote to **this** repo's GitHub, enables auto-push, enables auto-deploy to Vercel (if CLI + login present), checks GitHub login, and tests the PDF pipeline.

## Auto-push + Auto-deploy (enabled for this project only)
- After every `git commit`, a hook **automatically pushes** to `github.com/EternalFlames131/ReJivan` (branch main). If the Vercel CLI is installed and logged in, it **also deploys the production website** (`prototype\` → Vercel). Commits are never blocked; if offline/not-logged-in the hook prints a yellow note and moves on.
- Disable auto-deploy on a machine: create `.git\no-deploy` or set `REJIVAN_NO_DEPLOY=1`.
- **SAFETY (never cross-repo):** the hook only fires when this folder's git `origin` is exactly the ReJivan repo; otherwise it does nothing. The owner has OTHER GitHub repos and global git/opencode settings are never touched. Commits themselves are still deliberate (git add + git commit).
- If offline/not logged in, the push is skipped but the commit is safe — run `git push` later.

## Rules
- Owner is non-technical: plain language, no unexplained jargon.
- After finishing/substantial work, append a timestamped line to `CHANGELOG.md` (and the master log `C:\Users\samra\OneDrive\Desktop\Opencode task\LOG.md`). Then commit (auto-push takes care of GitHub).
- Full auto-read behaviour lives in the SESSION-OPEN PROTOCOL above.
- Prototype honesty: always label what is REAL vs SIMULATED in the demo.