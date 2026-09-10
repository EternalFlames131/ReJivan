# build_pdf.ps1
# Regenerates the ReJivan concept PDF from its HTML source, injecting an
# AUTO-GENERATED prototype status snapshot so the document can never go stale.
# Run:   pwsh -File tools\build_pdf.ps1   (from anywhere; script finds its own folder)
# Needs: Microsoft Edge (headless) + Python (pypdf). Works on any Windows PC.
#
# The snapshot is built from:
#   1. docs\features.json            — canonical feature status (kept up to date)
#   2. prototype\server.js           — the app's own /api/simulation/status lists
#                                       are parsed live, so the real/simulated
#                                       boundary always matches the actual code.
#   3. current build date/time       — stamped automatically.

$ErrorActionPreference = "Stop"

$root   = Split-Path -Parent $PSScriptRoot
$src    = Join-Path $root "docs\source\ReJivan_doc_source.html"
$out    = Join-Path $root "docs\ReJivan_Concept_Document_v1.1.pdf"
$feat   = Join-Path $root "docs\features.json"
$server = Join-Path $root "prototype\server.js"
$tmp    = "C:\Users\samra\AppData\Local\Temp\opencode\rejivan_doc_gen.html"

$edge = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
if (-not (Test-Path -LiteralPath $edge)) {
    $edge = "C:\Program Files\Microsoft\Edge\Application\msedge.exe"
}
foreach ($p in @($src, $feat, $server, $edge)) {
    if (-not (Test-Path -LiteralPath $p)) { Write-Error "Missing: $p"; exit 1 }
}

# -- 1) Read source + features -------------------------------------------------
$html = Get-Content -LiteralPath $src -Raw -Encoding UTF8
$data = Get-Content -LiteralPath $feat -Raw -Encoding UTF8 | ConvertFrom-Json
$buildDate = (Get-Date).ToString("dd MMM yyyy, HH:mm")

# -- 2) Pull the REAL / SIMULATED lists straight out of server.js ---------------
$code = Get-Content -LiteralPath $server -Raw -Encoding UTF8
function Get-CodeList($label) {
    $m = [regex]::Match($code, "$label\s*:\s*\[([^\]]*)\]")
    if (-not $m.Success) { return @() }
    return ($m.Groups[1].Value -split ',') | ForEach-Object { $_.Trim().Trim('"') } | Where-Object { $_ }
}
$realFromCode = Get-CodeList 'real'
$simFromCode  = Get-CodeList 'simulated'

function MakePill($status) {
    if ($status -eq 'real') { return '<span class="pill">Real</span>' }
    return '<span class="pill o">Simulated</span>'
}

# -- 3) Build the snapshot HTML block ------------------------------------------
$demoRows = ($data.demoAccounts | ForEach-Object {
    "<tr><td>$($_.name)</td><td>$($_.email) &middot; password: $($data.demoPassword)</td><td>$($_.sees)</td></tr>"
}) -join "`n"

$featRows = ($data.features | ForEach-Object {
    "<tr><td>$($_.name)</td><td>$(MakePill $_.status)</td><td>$($_.notes)</td></tr>"
}) -join "`n"

$realList  = ($realFromCode  | ForEach-Object { $_ }) -join ", "
$simList   = ($simFromCode   | ForEach-Object { $_ }) -join ", "

$snapshot = @"
<div class="pagebreak"></div>
<h2 class="sec">5b &nbsp;Live Prototype Status (auto-generated)</h2>
<p class="small">This section is built automatically from the code on every PDF rebuild &mdash; generated <b>$buildDate</b>. Feature status is read live from the prototype's transparency endpoint (<tt>/api/simulation/status</tt> in <tt>prototype\server.js</tt>), so it always matches what the app actually does.</p>
<p><b>Run the prototype:</b> <tt>$($data.howToRun.command)</tt> &rarr; open <tt>$($data.howToRun.url)</tt>.</p>
<p><b>Demo region:</b> $($data.region)</p>
<h3>Demo accounts (password: $($data.demoPassword))</h3>
<table class="data">
  <tr><th>Account</th><th>Login</th><th>Sees (data isolation)</th></tr>
  $demoRows
</table>
<h3>Feature status</h3>
<table class="data">
  <tr><th>Feature</th><th>Status</th><th>Notes</th></tr>
  $featRows
</table>
<h3>As reported by the running prototype (real vs simulated)</h3>
<table class="data">
  <tr><th>Layer status</th><th>Items</th></tr>
  <tr><td class="green">Real</td><td>$realList</td></tr>
  <tr><td class="red">Simulated (honest disclosure)</td><td>$simList</td></tr>
</table>
<div class="note nobreak"><b>Prototype honesty:</b> simulated parts are clearly labelled in the app and this document. ReJivan is <b>not</b> a medical device, does not diagnose, and never replaces a doctor &mdash; a human caregiver or clinician always makes the final decision.</div>
"@

# -- 4) Inject placeholders into the HTML and write the generated copy ----------
$html = $html.Replace('{{BUILD_DATE}}', $buildDate)
$html = $html.Replace('{{STATUS_ROW}}', "Prototype v1 built &mdash; working draft (auto-updated $buildDate)")
$html = $html.Replace('<!--AUTO:PROTOTYPE_SNAPSHOT-->', $snapshot)

$tmpDir = Split-Path -Parent $tmp
if (-not (Test-Path -LiteralPath $tmpDir)) { New-Item -ItemType Directory -Path $tmpDir | Out-Null }
[System.IO.File]::WriteAllText($tmp, $html, [System.Text.Encoding]::UTF8)

# -- 5) Render with Edge --------------------------------------------------------
$url = "file:///" + ($tmp -replace '[\\/]+', '/')
Write-Output "Rendering generated HTML -> PDF"
& $edge --headless=new --disable-gpu --no-pdf-header-footer --print-to-pdf="$out" $url 2>$null
if (-not (Test-Path -LiteralPath $out)) { Write-Error "PDF not produced."; exit 1 }

# Edge can return before the last bytes are flushed to disk. Wait until the file
# size is stable (two identical reads a moment apart) so the pre-commit hook's
# `git add` never stages a half-written PDF (which caused endless dirty-state
# flaps + autosave loops).
$size = -1
for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Milliseconds 500
    $cur = (Get-Item -LiteralPath $out).Length
    if ($cur -eq $size) { break }
    $size = $cur
}
Write-Output "PDF written: $out"

# -- 6) Verify -------------------------------------------------------------------
python -X utf8 tools\verify_pdf.py $out