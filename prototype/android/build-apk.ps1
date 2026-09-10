$ErrorActionPreference = "Stop"
Set-Location -LiteralPath (Split-Path -Parent $MyInvocation.MyCommand.Path)

Write-Host ""
Write-Host "===============================" -ForegroundColor Cyan
Write-Host "  ReJivan - Android APK build" -ForegroundColor Cyan
Write-Host "===============================" -ForegroundColor Cyan
$sw = [System.Diagnostics.Stopwatch]::StartNew()

$env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot" }

# --offline first: all dependencies are already cached, so this avoids the flaky
# network (which used to make the build look stuck). Falls back to online if needed.
& ".\gradlew.bat" assembleDebug --offline --console=plain 2>&1 | ForEach-Object { Write-Host $_ }
if ($LASTEXITCODE -ne 0) {
    Write-Host "Offline attempt failed (deps changed?) - retrying with network..." -ForegroundColor Yellow -NoNewline
    & ".\gradlew.bat" assembleDebug --console=plain 2>&1 | ForEach-Object { Write-Host $_ }
}
$sw.Stop()
if ($LASTEXITCODE -ne 0) {
    Write-Host "BUILD FAILED (see messages above)." -ForegroundColor Red
    exit 1
}

$src = "app\build\outputs\apk\debug\app-debug.apk"
$dest = Join-Path ([Environment]::GetFolderPath("UserProfile")) ("Downloads\ReJivan-Android-" + (Get-Date -Format "yyyyMMdd-HHmm") + ".apk")
Copy-Item -LiteralPath $src -Destination $dest -Force

Write-Host ""
Write-Host ("DONE in {0:n1} seconds" -f $sw.Elapsed.TotalSeconds) -ForegroundColor Green
Write-Host ("APK: {0}  ({1:N0} KB)" -f $dest, ((Get-Item -LiteralPath $dest).Length / 1KB)) -ForegroundColor Green