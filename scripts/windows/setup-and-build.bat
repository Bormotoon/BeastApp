@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot"
set "GRADLE_HOME=C:\Program Files\gradle-9.1.0"
set "PATH=%JAVA_HOME%\bin;%GRADLE_HOME%\bin;%PATH%"

"%JAVA_HOME%\bin\java.exe" -version
"%GRADLE_HOME%\bin\gradle.bat" -v

pushd "%~dp0\..\..\"

if not exist "local.properties" (
  set "SDK_DIR=%LOCALAPPDATA%\Android\Sdk"
  rem Use forward slashes to avoid escaping issues
  set "SDK_DIR_FWD=%SDK_DIR:\=/%"
  > local.properties echo sdk.dir=%SDK_DIR_FWD%
)

"%GRADLE_HOME%\bin\gradle.bat" wrapper --gradle-version 9.1

call .\gradlew.bat --version
call .\gradlew.bat assembleDebug

popd

echo Build finished. APK: app\build\outputs\apk\debug\app-debug.apk
endlocal

