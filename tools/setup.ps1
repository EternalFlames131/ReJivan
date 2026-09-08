# setup.ps1  —  One-time AUTO-SETUP so SanjivanAI works on any Windows PC with a drive-only folder.
# What it does (no clicks needed, hands-free by default):
#   1. Installs missing tools automatically (Python, pypdf, Edge, Git, GitHub CLI) using winget.
#   2. Sets LOCAL git identity for this repo only (never touches your global git settings).
#   3. Locks this folder's git remote to the SanjivanAI GitHub repo ONLY (never any other repo).
#   4. Enables the auto-push hook (every commit -> pushed to GitHub automatically).
#   5. Checks GitHub login so auto-push can work without fail.
#   6. Tests the PDF pipeline and writes a "setup done" marker for this machine.
# Run from the SanjivanAI folder:  pwsh -ExecutionPolicy Bypass -File tools\setup.ps1
# Optional interactive mode (ask before installing):  add -Ask

[CmdletBinding()]
param([switch]$Ask)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot
$pc = $env:COMPUTERNAME
$problems = [System.Collections.Generic.List[string]]::new()

function Write-Step([string]$n, [string]$txt) { Write-Host ("[{0}] {1}" -f $n, $txt) -ForegroundColor Cyan }
function Test-Cmd([string]$name) { return $null -ne (Get-Command $name -ErrorAction SilentlyContinue) }
function Refresh-Path {
    $m = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $u = [Environment]::GetEnvironmentVariable('Path', 'User')
    $env:Path = if ($u) { "$m;$u" } else { "$m" }
}
function Try-Locate([string]$name, [string[]]$paths) {
    foreach ($p in $paths) {
        if (Test-Path -LiteralPath $p) {
            $env:Path = $env:Path + ";" + (Split-Path -Parent $p)
            return $true
        }
    }
    return $false
}
function Should-Install([string]$label) {
    if (-not $Ask) { return $true }
    $a = Read-Host "Install $label now? (y/n)"
    return $a -match '^y'
}

Write-Host "`n===== SanjivanAI AUTO-SETUP =====" -ForegroundColor Cyan
Write-Host ("Folder : " + $root)
Write-Host ("Machine: " + $pc)

# ---------- 1. Python + pypdf ----------
Write-Step "1/6" "Python (needed for building/editing PDFs)"
if (Test-Cmd python) { python -X utf8 --version }
else {
    Refresh-Path
    if (-not (Test-Cmd python)) {
        if (Should-Install "Python") {
            if (Test-Cmd winget) { winget install -e --id Python.Python.3.12 --accept-package-agreements --accept-source-agreements }
            else { $problems.Add("Python: winget missing - install from python.org, restart terminal, rerun.") }
        }
        Refresh-Path
        if (-not (Test-Cmd python)) {
            if (-not (Try-Locate 'python.exe' @("$env:LOCALAPPDATA\Programs\Python\Python312\python.exe", "$env:LOCALAPPDATA\Programs\Python\Python313\python.exe"))) {
                $problems.Add("Python missing. After installing, restart the terminal and rerun setup.")
            }
        }
    }
}
if (Test-Cmd python) {
    python -X utf8 -m pip install --quiet --disable-pip-version-check pypdf 2>$null
    python -X utf8 -c "import pypdf; print('pypdf', pypdf.__version__)" 2>$null
    if ($LASTEXITCODE -ne 0) { $problems.Add("pypdf missing - run: python -m pip install pypdf") }
}

# ---------- 2. Microsoft Edge ----------
Write-Step "2/6" "Microsoft Edge (renders the PDF)"
$edge1 = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
$edge2 = "C:\Program Files\Microsoft\Edge\Application\msedge.exe"
if ((Test-Path -LiteralPath $edge1) -or (Test-Path -LiteralPath $edge2)) { Write-Host "  Edge found." }
else {
    if (Should-Install "Microsoft Edge") {
        if (Test-Cmd winget) { winget install -e --id Microsoft.Edge --accept-package-agreements --accept-source-agreements }
        else { $problems.Add("Edge missing - install Microsoft Edge, then rerun setup.") }
    }
    if (-not ((Test-Path -LiteralPath $edge1) -or (Test-Path -LiteralPath $edge2))) { $problems.Add("Edge missing - install Microsoft Edge, then rerun setup.") }
}

# ---------- 3. Git ----------
Write-Step "3/6" "Git (syncing to GitHub)"
if (Test-Cmd git) { git --version }
else {
    Refresh-Path
    if (Should-Install "Git") {
        if (Test-Cmd winget) { winget install -e --id Git.Git --accept-package-agreements --accept-source-agreements }
        else { $problems.Add("Git missing - install from git-scm.com, restart terminal, rerun.") }
    }
    Refresh-Path
    if (-not (Test-Cmd git)) {
        if (-not (Try-Locate 'git.exe' @("C:\Program Files\Git\cmd\git.exe", "C:\Program Files (x86)\Git\cmd\git.exe"))) {
            $problems.Add("Git missing - install from git-scm.com, restart terminal, rerun.")
        }
    }
}

Push-Location $root

# ---------- 4. Local (repo-only) git identity ----------
Write-Step "4/6" "Git identity (this repo only)"
if (Test-Cmd git) {
    if (-not (git config user.name))  { git config user.name  "Samrat";  Write-Host "  local user.name set" }
    if (-not (git config user.email)) { git config user.email "samrat@users.noreply.github.com"; Write-Host "  local user.email set" }
}

# ---------- 5. Lock remote + enable auto-push ----------
Write-Step "5/6" "Locking remote + enabling auto-push (SanjivanAI repo ONLY)"
if (Test-Cmd git) {
    if (-not (Test-Path -LiteralPath "$root\.git")) { git init -b main | Out-Null }
    $target = "https://github.com/EternalFlames131/SanjivanAI.git"
    $origin = git config --get remote.origin.url
    if ($origin -ne $target -and -not [string]::IsNullOrWhiteSpace($origin)) { git remote remove origin; Write-Host "  removed wrong origin: $origin" }
    if ($origin -ne $target) {
        git remote add origin $target
        Write-Host "  origin -> "(git remote get-url origin) -NoNewline
    } else { Write-Host "  origin correct: $origin" }
    git config core.hooksPath .githooks
    Write-Host "  auto-push hook ENABLED"
}

# ---------- 6. GitHub login ----------
Write-Step "6/6" "GitHub login (needed for auto-push)"
if (Test-Cmd gh) {
    gh auth status 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  Not logged in. Login once so every commit auto-pushes:" -ForegroundColor Yellow
        Write-Host "  gh auth login -h github.com -p https" -ForegroundColor Yellow
        if ($Ask) { gh auth login -h github.com -p https }
        else { Write-Host "  (skipped so setup stays hands-free - run that one command when ready.)" -ForegroundColor Yellow }
    }
} else {
    Write-Host "  GitHub CLI (gh) missing - installing..." -ForegroundColor Yellow
    if (Test-Cmd winget) {
        winget install -e --id GitHub.cli --accept-package-agreements --accept-source-agreements
        Refresh-Path
    }
    if (-not (Test-Cmd gh)) { $problems.Add("gh missing - install: winget install -e --id GitHub.cli ; then: gh auth login -h github.com -p https") }
}

# ---------- Test the PDF pipeline ----------
Write-Step "T" "Testing the PDF pipeline (needs Python + Edge)"
try { & "$root\tools\build_pdf.ps1" | Out-Host }
catch { $problems.Add("PDF pipeline test failed: " + $_.Exception.Message) }

Pop-Location

# ---------- Mark this machine as set up ----------
$marker = Join-Path $root ("tools\.setup-done-" + $pc + ".txt")
Set-Content -LiteralPath $marker -Value ((Get-Date -Format "yyyy-MM-dd HH:mm") + " | setup ran on $pc")

if ($problems.Count -eq 0) {
    Write-Host "`n===== SETUP COMPLETE - ALL GREEN. Auto-push is ready on this device. =====" -ForegroundColor Green
} else {
    Write-Host "`n===== SETUP FINISHED (see reminders below) =====" -ForegroundColor Yellow
    $problems | ForEach-Object { Write-Host ("  * " + $_) -ForegroundColor Yellow }
}
Write-Host "Every commit is auto-pushed to github.com/EternalFlames131/SanjivanAI (origin-checked; other repos NEVER touched)."
Write-Host "Your global git settings and other repositories are untouched."
Write-Host "If a push is skipped (offline / not logged in), your commit is safe - run 'git push' later."