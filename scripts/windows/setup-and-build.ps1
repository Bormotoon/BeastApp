Param(
  [string]$AndroidSdk = "$env:LOCALAPPDATA/Android/Sdk"
)

$ErrorActionPreference = 'Stop'

# Ensure paths use Windows separators where needed
$jdk = "C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot"
$gradle = "C:\Program Files\gradle-9.1.0"

Write-Host "Using JDK: $jdk"
Write-Host "Using Gradle: $gradle"

# Set env for current session only
$env:JAVA_HOME = $jdk
$env:GRADLE_HOME = $gradle
$env:Path = "$env:JAVA_HOME\bin;$env:GRADLE_HOME\bin;$env:Path"

# Verify tools
& "$env:JAVA_HOME\bin\java.exe" -version
& "$env:GRADLE_HOME\bin\gradle.bat" -v

# Go to repo root (script is placed under scripts/windows)
$repoRoot = (Resolve-Path "$PSScriptRoot\..\..\").Path
Set-Location $repoRoot
Write-Host "Repo root: $repoRoot"

# Create local.properties if missing
$localProps = Join-Path $repoRoot 'local.properties'
if (-not (Test-Path $localProps)) {
  $sdkNormalized = $AndroidSdk -replace '\\','/'
  $content = "sdk.dir=$sdkNormalized"
  $content | Out-File -FilePath $localProps -Encoding ascii -NoNewline
  Write-Host "Created local.properties with sdk.dir=$sdkNormalized"
} else {
  Write-Host "local.properties already exists, skipping."
}

# Generate Gradle wrapper 9.1 (creates gradle-wrapper.jar if missing)
& "$env:GRADLE_HOME\bin\gradle.bat" wrapper --gradle-version 9.1

# Build Debug APK via wrapper
& ".\gradlew.bat" --version
& ".\gradlew.bat" assembleDebug

Write-Host "Build finished. APK should be at app\\build\\outputs\\apk\\debug\\app-debug.apk"

