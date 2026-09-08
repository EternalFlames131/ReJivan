# build_pdf.ps1
# Regenerates the SanjivanAI concept PDF from its HTML source.
# Run:   pwsh -File tools\build_pdf.ps1   (from anywhere; script finds its own folder)
# Needs: Microsoft Edge (headless). Works on any Windows PC.

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$src  = Join-Path $root "docs\source\SanjivanAI_doc_source.html"
$out  = Join-Path $root "docs\SanjivanAI_Concept_Document_v1.1.pdf"

$edge = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
if (-not (Test-Path -LiteralPath $edge)) {
    $edge = "C:\Program Files\Microsoft\Edge\Application\msedge.exe"
}
if (-not (Test-Path -LiteralPath $src)) {
    Write-Error "Source HTML not found: $src"
    exit 1
}
if (-not (Test-Path -LiteralPath $edge)) {
    Write-Error "Microsoft Edge not found. Install Edge (headless PDF printing needs it)."
    exit 1
}

$url = "file:///" + ($src -replace '[\\/]+', '/')
Write-Output "Rendering: $src"
& $edge --headless=new --disable-gpu --no-pdf-header-footer --print-to-pdf="$out" $url 2>$null
if (-not (Test-Path -LiteralPath $out)) { Write-Error "PDF not produced."; exit 1 }
Write-Output "PDF written: $out"

# Optional: quick page-count check as well
python -X utf8 tools\verify_pdf.py $out