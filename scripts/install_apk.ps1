# One-click script to download the latest OOREP Android APK from GitHub Actions and install via adb
$ErrorActionPreference = "Stop"

Write-Host "Checking for connected Android devices via adb..." -ForegroundColor Cyan
$devices = adb devices
$deviceLines = $devices -split "`n" | Where-Object { $_ -match "\tdevice" }

if (-not $deviceLines) {
    Write-Host "No Android devices or emulators found in 'adb devices'." -ForegroundColor Yellow
    Write-Host "Please connect your Android phone via USB (with USB Debugging enabled) or start an emulator." -ForegroundColor Yellow
    exit 1
}

Write-Host "Found device: $($deviceLines[0].Trim())" -ForegroundColor Green

$outputDir = Join-Path $PSScriptRoot "..\build-output"
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

Write-Host "Fetching latest successful workflow artifact from dhyankrishna24/oorep-android..." -ForegroundColor Cyan
gh run download --repo dhyankrishna24/oorep-android --name oorep-debug-apk --dir $outputDir

$apkPath = Join-Path $outputDir "app-debug.apk"
if (-not (Test-Path $apkPath)) {
    # Check if inside subfolder
    $found = Get-ChildItem -Path $outputDir -Filter "*.apk" -Recurse | Select-Object -First 1
    if ($found) {
        $apkPath = $found.FullName
    } else {
        Write-Host "Could not find downloaded APK in $outputDir" -ForegroundColor Red
        exit 1
    }
}

Write-Host "Installing $apkPath to Android device..." -ForegroundColor Cyan
adb install -r $apkPath

Write-Host "OOREP Android successfully installed! Starting app..." -ForegroundColor Green
adb shell am start -n com.oorep.android/.MainActivity
