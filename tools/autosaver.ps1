# autosaver.ps1 — ReJivan AutoSave watcher
# Watches the project folder. As soon as you change any file, waits until the
# change-set stays identical for a short quiet period, then runs:
#   git add -A  &&  git commit
# The .githooks/post-commit hook then AUTOMATICALLY pushes to GitHub and
# redeploys rejivan.vercel.app — so you never have to run any command again.
#
# Start (background):   pwsh -File "tools\autosaver.ps1"
# Keep it always on:    the "ReJivan AutoSave" scheduled task starts it at
#                       Windows logon (registered during setup).
# Stop temporarily:     put a file named 'no-autosave' inside the .git folder.
# Log:                  tools\autosaver.log  (never committed — see .gitignore)

$ErrorActionPreference = "Stop"

$root  = Split-Path -Parent $PSScriptRoot
$log   = Join-Path $PSScriptRoot "autosaver.log"
$pause = Join-Path $root ".git\no-autosave"

$poll      = 10   # seconds between checks
$quiet     = 40   # change-set must be identical this long before auto-committing
$cooldown  = 120  # minimum seconds between auto-commits (protects Vercel quota)

function Log($msg) {
  Add-Content -LiteralPath $log -Value ("{0:yyyy-MM-dd HH:mm:ss} | {1}" -f (Get-Date), $msg)
}
function Get-Signature {
  # NUL-separated list of changed paths => a stable "fingerprint" of the worktree
  $z = git -C $root status --porcelain -z 2>$null
  if ($null -eq $z) { return "" }
  $path = ($z -split "`0")
  $out = [System.Collections.Generic.List[string]]::new()
  foreach ($e in $path) {
    if ($e.Length -gt 3) { $out.Add(($e.Substring(3))) }   # strip status code (XY )
  }
  return ($out -join "`n")
}

Log "AutoSave started for $root"
Write-Host "[ReJivan AutoSave] watching '$root'" -ForegroundColor Cyan
if (Test-Path -LiteralPath $pause) {
  Log "      suspended: .git\no-autosave exists (remove it to resume)"
  Write-Host "[ReJivan AutoSave] SUSPENDED (.git\no-autosave exists)." -ForegroundColor Yellow
}

$lastCommit = Get-Date
$prevSig    = Get-Signature
$changedAt  = Get-Date

while ($true) {
  Start-Sleep -Seconds $poll
  if (Test-Path -LiteralPath $pause) { continue }

  $sig = Get-Signature
  if ($sig -ne $prevSig)            { $prevSig = $sig; $changedAt = Get-Date; continue }
  if ($sig.Length -eq 0)            { continue }
  if (((Get-Date) - $lastCommit).TotalSeconds -lt $cooldown) { continue }
  if (((Get-Date) - $changedAt).TotalSeconds -lt $quiet)     { continue }

  # --- commit time (hook auto-pushes + auto-deploys) ---
  $paths = $sig -split "`n"
  $desc = (($paths | Select-Object -First 3) -join ", ")
  if ($desc.Length -gt 60) { $desc = $desc.Substring(0, 60) + "..." }

  git -C $root add -A 2>$null
  git -C $root commit -m "Auto-save: $desc" 2>$null
  if ($LASTEXITCODE -eq 0) {
    Log "auto-committed (push + deploy run via hook): $desc"
    $lastCommit = Get-Date
  } else {
    Log "commit rejected/skipped: $desc"
  }
  $prevSig = Get-Signature
  $changedAt = Get-Date
}