# User Context (auto-loaded every session)

## Who the user is
- Name: Samrat (Samrat Dasgupta). Not technical — explain in plain steps, avoid jargon.
- Teacher trainee. Works on lesson-plan PDFs (Mathematics, Class VIII 'B', 40-minute periods).
- Environment: Windows 11, PowerShell 7 (pwsh). Shell is pwsh.

## Where the user's data lives (always remember)
- **Desktop is redirected by OneDrive.** Real path: `C:\Users\samra\OneDrive\Desktop`
- **NEW logs/status folder (created 2026-08-24 at user request): `C:\Users\samra\OneDrive\Desktop\Opencode task\`** — contains LOG.md with the running activity log + project status snapshot. Update it when finishing significant work.
- **OLD main project folder `C:\Users\samra\OneDrive\Desktop\Opencode Trial\` NO LONGER EXISTS** (verified missing 2026-08-24; user may have moved/deleted it — ask before assuming). Until a new home is confirmed, save deliverables to Downloads or the Opencode task folder.
  - It used to contain: `index.html`, `NOTES.md`, and generated PDFs (incl. We_Distribute_Yet_Things_Multiply_6LPs_Improved.pdf).
- Temp work area (safe for scratch files): `C:\Users\samra\AppData\Local\Temp\opencode`
- Downloads: `C:\Users\samra\Downloads` (original PDFs the user uploads usually land here).
- Older PDFs also seen in: `C:\Users\samra\Dropbox\PC\Downloads`

## Project: PDF lesson-plan improvement (main ongoing task)
- Original PDFs live in `C:\Users\samra\Downloads` or Dropbox Downloads.
- Pipeline for building an improved PDF:
  1. Extract original text (pypdf) into a scratch .txt in the temp area.
  2. Write/update the HTML source in the temp area (`lp_better.html` pattern).
  3. Render with Edge headless:
     `& "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --headless=new --disable-gpu --no-pdf-header-footer --print-to-pdf="C:\Users\samra\Downloads\<out>.pdf" "file:///C:/Users/samra/AppData/Local/Temp/opencode/<src>.html"`
  4. Verify with pypdf (page count, footers, content placement). ALWAYS use `python -X utf8` (console is cp1252 and crashes on `→`/`²`).
- Deliverable so far: `We_Distribute_Yet_Things_Multiply_6LPs_Improved.pdf` — 14 pages, one cover + index + 6 lesson plans x 2 pages. Saved in the Opencode Trial folder.
- Related pending: `Number_Play_Lesson_Plans_7LPs_FixedLP7.pdf` and `Number_Play_Lesson_Plans_7LPs_Expanded.pdf` in Dropbox Downloads (7 lesson plans) — may need the same treatment if the user asks.
- Known pitfall: footers were hard-coded as "Page N of 8" when content spans 14 pages — always compute "Page N of TOTAL" from the actual rendered page count.

## Lesson-plan format (APPROVED TEMPLATE — follow exactly)
- The approved reference template is `C:\Users\samra\Downloads\Number_Play_Lesson_Plans_7LPs_Expanded.pdf` (15 pages, 7 lesson plans; extracted text in `C:\Users\samra\AppData\Local\Temp\opencode\np_expanded.txt`). From now on the user supplies a chapter PDF and wants lesson plans in THIS format.
- Per lesson plan, exactly TWO pages:
  - **Page 1:** PRELIMINARY INFORMATION (table: Name of Teacher Trainee, Name of Practising School, Date, Period, Class and Section, Subject, Name of Lesson, Name of Topic, Duration of Period 40 mins) → SPECIFIC INSTRUCTIONAL OBJECTIVES (intro line "At the end of the class, the pupil will be able to—" + exactly 5 bullets) → TEACHING AIDS AND TECHNOLOGY PROPOSED (comma-separated) → ENTRY BEHAVIOUR (exactly 5 Ques/Ans pairs) → ANNOUNCEMENT OF THE TOPIC → INSTRUCTIONAL PROCEDURE table with EXACTLY 4 columns: CONTENT | LEARNING EXPERIENCES | BEHAVIOURAL OBJECTIVES | EVALUATION, and EXACTLY 5 rows (one per objective).
  - **Page 2:** SUMMARY (4 bullets) → EVALUATION (3 numbered questions) → "Signature of Teacher Trainee" / "Signature of Guide Teacher".
- **NEVER** split Learning Experiences into "what the teacher does / what the pupil does" columns — the user explicitly rejected that. Learning Experiences is ONE narrative column.
- **Teaching-aid illustrations must be embedded INSIDE the Learning Experiences cell of the row where that aid is used**, each with a "Teaching aid: <name>" caption. NEVER place them in a separate figure/illustration section after the Instructional Procedure (user explicitly rejected that).
- Number Play chapter content (from the template): Sum of Consecutive Numbers; Parity of Signed Expressions; Breaking Even – Parity of Arithmetic Expressions; Always-Even Algebraic Expressions; Multiples of 4; Always / Sometimes / Never Statements; Remainders and Divisibility by 9.

## Project: Song / audio editing (ongoing)
- User asks for YouTube cutting/merging and audio effects. Work folder: **`C:\Dance`**.
- Tools already installed (do NOT reinstall):
  - `yt-dlp` via pip (`python -m yt_dlp ...`)
  - `ffmpeg` v7.1 via imageio-ffmpeg package. Exe path:
    `C:\Users\samra\AppData\Roaming\Python\Python312\site-packages\imageio_ffmpeg\binaries\ffmpeg-win-x86_64-v7.1.exe`
    (no ffprobe available — check durations with `& $ff -i <file> 2>&1 | Select-String Duration`). Pass this exe to yt-dlp via `--ffmpeg-location`.
- Deliverable: `C:\Dance\Chanakya_GarajGaraj_Mix.mp3` (3:06, 192 kbps). Part 1 = "Chanakya" Rishab Rikhiram Sharma (youtu.be/2dBHTz6g5AM) 0:06–1:06; Part 2 = "Garaj Garaj (Rock)" Bandish Bandits S2 (youtu.be/DkgNWHeo5ZI) 0:08–2:14. Full downloads kept as `song1.m4a`, `song2.m4a` in C:\Dance for re-cuts.
- Radio effect DONE 2026-08-21: applied light old-AM-radio effect to `WhatsApp Audio 2026-08-20 at 8.33.49 PM.mp4` (1:25) → saved as `C:\Dance\WhatsApp_Audio_AM_Radio.mp3` (original untouched). First "light" version was TOO SUBTLE (user couldn't hear it); current STRONG version: aformat mono + highpass 450/lowpass 2800 + 1.7kHz +6dB + acrusher(bits=9,mix=0.25) + acompressor(-24dB,5:1,+4dB) + tremolo(4Hz,8%) + pink noise amp0.05 vol0.45 band-limited. Tune stronger/weaker via noise volume, EQ gain, acrusher mix.
- Network is flaky: large downloads (winget, pip wheels >20 MB) stall/reset. Use `curl.exe -L -C -` resume loops in short chunks from files.pythonhosted.org / github.com (gyan.dev is blocked).

## Project: Andaman tourism website (active)
- **Working folder: `C:\andaman-gems 2`** — user instruction 2026-08-22: ALL work on this project happens ONLY in this folder. Never edit the old copy.
- **User wants this folder EMPTY at the start** — a copy of the old project was made on 2026-08-22 and then removed at the user's request. Rebuild work must be done fresh inside it, file by file.
- Reference source (read-only): `C:\Users\samra\OneDrive\Desktop\Opencode Trial\AndamanTourism` — old copy left untouched as backup.
- Node/Express app. Run: `node server.js` inside `C:\andaman-gems 2` → serves at **http://localhost:8080** (NOT port 3000 despite what old SETUP.md says).
- Pages: `/` home, `/places`, `/booking`, `/souvenirs`, `/map`, `/admin`. Data in `data/` (`tourism_data.js`, bookings/permits/analytics JSON). Front-end files in `public/`.
- Dependencies if rebuilding from scratch: express ^4.18.2, cors ^2.8.5, body-parser ^1.20.2, uuid ^9.0.0 (`npm install`; network is flaky — package-lock.json exists in old folder for reference).

## Project: Gems of India Challenge 2026 — competition video
- **IMPORTANT (corrected 2026-08-22): Samrat works on TWO folders as SEPARATE projects — `C:\andaman-gems 2` AND `C:\andaman-gems`. Neither is abandoned. NEVER move, merge, copy between, or treat one as replacing the other. Always confirm WHICH folder before doing any work on this project.**
- **`C:\andaman-gems 2`** (this conversation): fresh build started here — directory scaffold (sources/*, graphics, metadata, subtitles, scripts, previews, exports), project-info.txt (creator name placeholder), CSV headers written. No media downloaded yet. Commons API research done: CC BY/BY-SA photo candidates identified for Radhanagar Beach, Cellular Jail, Ross Island ruins, Baratang mangroves, Chidiya Tapu sunset (licences+authors recorded in this conversation; failed search: "Port Blair harbour boats" returned PDFs — retry with jetty/ferry terms).
- **`C:\andaman-gems`** (other conversation, separate variant): its own detailed instructions live in `C:\andaman-gems\PROGRESS.md` — read FIRST when working there. State per that file: RESUME POINT = downloading remaining 15 of 26 Commons photos via verified URLs in `metadata/commons-photo-urls.json` (never guess upload.wikimedia.org hash paths — earlier guesses saved error pages).
- Shared task spec: documentary video "ANDAMAN — THE ISLAND THAT REMEMBERS", ~2:30, English narration+subtitles, 1920x1080@30fps H.264/AAC 48kHz yuv420p MP4.
- User decisions: visuals = free-licence online media (Wikimedia Commons, documented locations); narration = Samrat records his own voice later — NO TTS; no uploads ever.
- Tooling: ffmpeg 7.1 at imageio path (see Song project); ffprobe NOT installed — parse `ffmpeg -i` + blackdetect/freezedetect/silencedetect/volumedetect for QC. Fonts: arialbd.ttf, segoeui.ttf.
- Hard rules: no uploads, never delete originals, no AI/invented footage as evidence, no unverified tribal imagery (dignity policy), don't present wrong-location stock as Andaman, attribution screen required (CC BY/BY-SA), don't claim completion unless QC passes.

## Project: Animated video (started 2026-08-24)
- User asked for a standalone animated video, style = mix (photos-with-motion + text/titles + shapes/graphics), no topic chosen yet.
- DONE: style demo `C:\Users\samra\Downloads\Animated_Video_Demo.mp4` — 18.6s, 1920x1080@30fps, silent, 4 scenes crossfaded: title card / moving shapes / Ken Burns on generated gradient image / mandelbrot finale. QC passed.
- Build recipe: scenes rendered separately into `C:\Users\samra\AppData\Local\Temp\opencode\animdemo\s1–s4.mp4`, joined with xfade (offsets 4.2/8.4/13.6, d=0.8). Full technical notes in `Desktop\Opencode task\LOG.md`.
- AWAITING user decision: which style he liked + topic/content for the real video. Nothing else started.
- QC gotcha learned: blackdetect false-positives on dark-background scenes — verify text via crop+signalstats YMAX instead; drawtext expressions use h/w (NOT ih/iw).

## Permission rules (set up 2026-08-21 at user request)
- Global config `C:\Users\samra\.config\opencode\opencode.jsonc` now has explicit permission rules:
  - Read-only PowerShell (Get-*, Test-*, Select-String, etc.), web fetch/search: auto-allowed.
  - File edits and all other commands: ask every time.
  - Hard-blocked (deny): format/disk tools, `reg delete/add/import`, shutdown/restart.
  - external_directory pre-approved for: OneDrive Desktop, Downloads, Temp\opencode, Dropbox, C:\Dance — keeps Samrat's normal workflow prompt-free.
- Do not loosen these without asking; user wanted destructive actions gated.

## Session continuity
- To resume a previous conversation in opencode: use the session picker / list in the TUI and select the last session instead of starting fresh.
- If the user says "continue from where we left off", consult this file AND check the opencode log/session history rather than asking again.
- Broad recursive scans of `C:\Users\samra` time out — use targeted paths (this file's paths are the targets).

## Standing rule - ALWAYS auto-update the logs
- After ANY change made in a session (code, config, files, cloud settings), IMMEDIATELY append one
  timestamped line to BOTH:
  1. The project's changelog file inside that project (e.g. CHANGELOG_LPGenerator_2026-09-02.md)
  2. The running activity log C:\Users\samra\OneDrive\Desktop\Opencode task\LOG.md
- Line format: - YYYY-MM-DD HH:MM | short plain-language summary
- This is not optional - even if the user does not ask, do it at the end of every working session.
- If a session changes nothing meaningful, add [skip-log] to the summary instead.
