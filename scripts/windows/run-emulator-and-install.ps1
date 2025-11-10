<#
Run-emulator-and-install.ps1

Usage:
  powershell -ExecutionPolicy Bypass -File .\scripts\windows\run-emulator-and-install.ps1 -AvdName "Pixel_5_API_30" -ProjectRoot "C:\VibeCoding\BeastApp"
  .\scripts\windows\run-emulator-and-install.ps1  # interactive: will list AVDs and ask for name

What it does:
  - Detects Android SDK (uses $Env:ANDROID_SDK_ROOT or $Env:LOCALAPPDATA\Android\Sdk)
  - Ensures `platform-tools` and `emulator` are on PATH for the session
  - Lists AVDs and starts the chosen AVD (if not already running)
  - Waits for emulator to finish booting (sys.boot_completed)
  - Builds debug APK with Gradle and installs it to the running emulator
  - Attempts to detect package name from `app/src/main/AndroidManifest.xml` and launches the app

Notes:
  - Run in an elevated PowerShell or with ExecutionPolicy that allows running local scripts.
  - Script assumes `gradlew.bat` is present in project root.
  - Adjust timeouts if your machine/emulator is slow.
#>

param(
    [string]$AvdName = "",
    [string]$ProjectRoot = ""
)

function Write-Log { param($msg) Write-Host "[run-emulator] $msg" }

# Resolve script and project root
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
if (-not $ProjectRoot -or -not (Test-Path $ProjectRoot)) {
    $ProjectRoot = (Get-Item -Path $ScriptDir -Force).FullName
}

# Detect Android SDK
if (-not $Env:ANDROID_SDK_ROOT -or -not (Test-Path $Env:ANDROID_SDK_ROOT)) {
    $fallback = Join-Path $Env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $fallback) { $Env:ANDROID_SDK_ROOT = $fallback }
}

if (-not $Env:ANDROID_SDK_ROOT -or -not (Test-Path $Env:ANDROID_SDK_ROOT)) {
    Write-Error "Android SDK not found. Set ANDROID_SDK_ROOT or install SDK and ensure path exists."; exit 1
}

$SdkRoot = $Env:ANDROID_SDK_ROOT
$PlatformTools = Join-Path $SdkRoot 'platform-tools'
$EmulatorBin = Join-Path $SdkRoot 'emulator'

if (-not (Test-Path $PlatformTools)) { Write-Error "platform-tools not found at $PlatformTools"; exit 1 }
if (-not (Test-Path $EmulatorBin)) { Write-Error "emulator not found at $EmulatorBin"; exit 1 }

Write-Log "Using SDK: $SdkRoot"

# List AVDs
$avds = & "$EmulatorBin\emulator.exe" -list-avds 2>$null
if (-not $avds) { Write-Error "No AVDs found. Create one in Android Studio AVD Manager."; exit 1 }

Write-Log "Available AVDs:`n$avds"

if (-not $AvdName) {
    $AvdName = Read-Host "Enter AVD name to start (case-sensitive, copy from list above)"
    if (-not $AvdName) { Write-Error "No AVD specified"; exit 1 }
}

# Start emulator if not already running
function Is-Emulator-Running($name) {
    $devices = & "$PlatformTools\adb.exe" devices 2>$null | Select-Object -Skip 1
    foreach ($d in $devices) {
        if ($d -match "emulator-") { return $true }
    }
    return $false
}

if (-not (Is-Emulator-Running $AvdName)) {
    Write-Log "Starting emulator: $AvdName"
    Start-Process -FilePath "$EmulatorBin\emulator.exe" -ArgumentList "-avd`, `"$AvdName`" -no-snapshot-load -no-boot-anim" -WindowStyle Normal
} else {
    Write-Log "Emulator appears to be already running"
}

# Wait for adb device
Write-Log "Waiting for adb device..."
& "$PlatformTools\adb.exe" wait-for-device

# Wait for boot completed property
$timeoutSec = 300
$elapsed = 0
Write-Log "Waiting for emulator to finish booting (timeout ${timeoutSec}s)"
while ($elapsed -lt $timeoutSec) {
    $prop = & "$PlatformTools\adb.exe" shell getprop sys.boot_completed 2>$null | ForEach-Object { $_.Trim() }
    if ($prop -eq '1') { break }
    Start-Sleep -Seconds 1
    $elapsed += 1
}

if ($elapsed -ge $timeoutSec) {
    Write-Warning "Emulator did not signal boot completed within ${timeoutSec}s. Continuing anyway."
} else {
    Write-Log "Emulator booted in ${elapsed}s"
}

# Build the app
Write-Log "Building debug APK with Gradle"
Push-Location -Path $ProjectRoot
try {
    $gradle = Join-Path $ProjectRoot 'gradlew.bat'
    if (-not (Test-Path $gradle)) { Write-Error "gradlew.bat not found in project root: $ProjectRoot"; Pop-Location; exit 1 }
    & $gradle :app:assembleDebug
} catch {
    Write-Error "Gradle build failed: $_"; Pop-Location; exit 1
}
Pop-Location

# Install APK
$apkPath = Join-Path $ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apkPath)) {
    Write-Warning "APK not found at $apkPath; attempting Gradle install instead"
    Push-Location -Path $ProjectRoot
    & $gradle :app:installDebug
    Pop-Location
} else {
    Write-Log "Installing APK: $apkPath"
    & "$PlatformTools\adb.exe" install -r "$apkPath"
}

# Detect package from AndroidManifest.xml
$manifest = Join-Path $ProjectRoot 'app\src\main\AndroidManifest.xml'
$pkg = 'com.beast.app'
if (Test-Path $manifest) {
    $raw = Get-Content $manifest -Raw
    if ($raw -match 'package="([^"]+)"') { $pkg = $matches[1] }
}

Write-Log "Launching app package: $pkg"
& "$PlatformTools\adb.exe" shell monkey -p $pkg -c android.intent.category.LAUNCHER 1

Write-Log "Done. If the app did not start, check 'adb logcat' for errors."
