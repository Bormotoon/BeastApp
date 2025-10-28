Windows scripts:

1) PowerShell (recommended):
   - Run with:  Right-click this file's folder > Open in Terminal (PowerShell) and execute:
       pwsh -File .\setup-and-build.ps1
     Optional: specify custom Android SDK path (if not default):
       pwsh -File .\setup-and-build.ps1 -AndroidSdk "C:/Android/Sdk"

2) CMD (fallback):
   - Double-click setup-and-build.bat or run from cmd:
       setup-and-build.bat

Both scripts will:
- Set JAVA_HOME and GRADLE_HOME for the current session
- Generate Gradle Wrapper 9.1 (if missing)
- Create local.properties with your default SDK path if missing
- Build Debug APK via gradlew.bat

