# setup.ps1  —  One-time setup to run SanjivanAI from THIS folder on any Windows PC.
# How to use:  open PowerShell here and run:  pwsh -File tools\setup.ps1
# What it does:
#   1. Installs missing tools if you have winget (Python, pypdf, Edge, Git) - asks first.
#   2. Sets LOCAL git identity for this repo only (never touches your global git settings).
#   3. Points this repo's git remote AT SanjivanAI's GitHub repo ONLY (never any other repo).
#   4. Enables the auto-push hook (after each commit, changes go to GitHub automatically).
#   5. Checks your GitHub login (needed for pushes).
#   6. Tests the PDF pipeline.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Write-Host "`n===== SanjivanAI one-time setup =====" -ForegroundColor Cyan
Write-Host ("Folder: " + $root + "`n")

function Test-Cmd([string]$name) { return $null -ne (Get-Command $name -ErrorAction SilentlyContinue) }

# ---------- helper: winget install with acceptance flags ----------
function Install-App([string]$id, [string]$label) {
    if (-not (Test-Cmd winget)) {
        Write-Host "winget not available - install '$label' manually from its official site, then rerun this script." -ForegroundColor Yellow
        return $false
    }
    $a = Read-Host ("Install " + $label + " now? (y/n, default y)")
    if ($a -notin "y", "", "Y") { return $false }
    winget install -e --id $id --accept-package-agreements --accept-source-agreements
    return $true
}

# ---------- 1. Python + pypdf (needed for PDF verify/edits) ----------
Write-Host "[1/6] Python (for PDF tooling)..." -ForegroundColor Cyan
if (Test-Cmd python) { python --version }
else {
    Write-Host "Python is missing."
    $env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' + [Environment]::GetEnvironmentVariable('Path', 'User')
    if (-not (Test-Cmd python)) { Install-App "Python.Python.3.12" "Python" }
    else { python --version }
}
$env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' + [Environment]::GetEnvironmentVariable('Path', 'User')
if (Test-Cmd python) {
    python -X utf8 -m pip install --quiet --disable-pip-version-check pypdf 2>$null
    python -X utf8 -c "import pypdf; print('pypdf OK')"
}
if (-not (Test-Cmd python)) { Write-Host "Python still not available. Please restart the terminal after install, then rerun setup." -ForegroundColor Yellow }

# ---------- 2. Microsoft Edge (for PDF rendering) ----------
Write-Host "[2/6] Microsoft Edge (for PDF rendering)..." -ForegroundColor Cyan
$edge32 = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
$edge64 = "C:\Program Files\Microsoft\Edge\Application\msedge.exe"
if ((Test-Path -LiteralPath $edge32) -or (Test-Path -LiteralPath $edge64)) { Write-Host "Edge present." }
else { Write-Host "Edge is missing."; Install-App "Microsoft.Edge" "Microsoft Edge" }

# ---------- 3. Git (for GitHub sync) ----------
Write-Host "[3/6] Git..." -ForegroundColor Cyan
if (Test-Cmd git) { git --version } else { Install-App "Git.Git" "Git" }

# ---------- 4. Local (repo-only) git identity ----------
Write-Host "[4/6] Git identity for THIS repo..." -ForegroundColor Cyan
Push-Location $root
if (-not (git config user.name))  { git config user.name  "Samrat";  Write-Host "  set local user.name" }
if (-not (git config user.email)) { git config user.email "samrat@users.noreply.github.com"; Write-Host "  set local user.email" }

# ---------- 5. Lock remote to THIS project's GitHub repo ----------
Write-Host "[5/6] Locking git remote to SanjivanAI's GitHub repo..." -ForegroundColor Cyan
$target = "https://github.com/EternalFlames131/SanjivanAI.git"
if (-not (Test-Path -LiteralPath "$root\.git")) { git init -b main }
$origin = git config --get remote.origin.url
if ($origin -ne $target) {
    if ($origin) { git remote remove origin }
    git remote add origin $target
    git pull --set-upstream origin main 2>$null
    Write-Host "  origin -> " -NoNewline; git remote get-url origin
} else {
    Write-Host "  origin already correct: $origin"
}
git config core.hooksPath .githooks
Write-Host "  auto-push hook ENABLED (every commit is pushed to GitHub for this project)."
Pop-Location

# ---------- 6. GitHub login check ----------
Write-Host "[6/6] GitHub login for pushes..." -ForegroundColor Cyan
if (Test-Cmd gh) { gh auth status 2>&1 | Out-Host }
else {
    Write-Host "GitHub CLI not installed. To allow auto-push, install it:" -ForegroundColor Yellow
    Write-Host '  winget install -e --id GitHub.cli'
    Write-Host '  gh auth login'
}

# ---------- Test the PDF pipeline ----------
Write-Host "Testing PDF pipeline..." -ForegroundColor Cyan
try { & "$root\tools\build_pdf.ps1" | Out-Host }
catch { Write-Host ("PDF test skipped/failed: " + $_.Exception.Message) -ForegroundColor Yellow }

Write-Host "`n===== Setup finished =====" -ForegroundColor Green
Write-Host "Important: commit normally with 'git add -A', 'git commit -m ""message""' - push is automatic."
Write-Host "If a push says offline/failed, run 'git push' later - your commit is safely saved."