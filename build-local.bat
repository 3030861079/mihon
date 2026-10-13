@echo off
REM ============================================================
REM  Mihon local build script for Windows
REM
REM  Size policy (keep builds lean):
REM    - Prefer release over debug  (~26-35MB vs ~70MB)
REM    - Install only the arm64-v8a APK on real devices
REM    - Do NOT install the universal APK (~65MB+ / debug ~137MB)
REM
REM  NOTE: this machine's default JVM is JDK 17, but Mihon requires
REM  JDK 21 (the metro Gradle plugin refuses to run on 17).
REM  So JAVA_HOME is overridden below.
REM ============================================================
setlocal

set "JAVA_HOME=C://Users//%USERNAME%//.workbuddy//binaries//jdk//21//jdk-21.0.12.1+1"
if not exist "%JAVA_HOME%\bin\java.exe" goto nojdk

REM Make dead connections fail fast instead of stalling the whole build.
set "GRADLE_OPTS=-Dorg.gradle.internal.http.connectionTimeout=30000 -Dorg.gradle.internal.http.socketTimeout=30000"

if "%~1"=="" goto default
if /I "%~1"=="help" goto usage
if /I "%~1"=="-h" goto usage
if /I "%~1"=="--help" goto usage
if /I "%~1"=="install" goto install

echo Using JDK: %JAVA_HOME%
call "%~dp0gradlew.bat" %* --console=plain
exit /b %ERRORLEVEL%

:default
echo Using JDK: %JAVA_HOME%
echo.
echo [default] assembleRelease + install arm64 APK
echo.
call "%~dp0gradlew.bat" :app:assembleRelease --console=plain
if errorlevel 1 exit /b %ERRORLEVEL%
goto install

:install
set "APK=%~dp0app\build\outputs\apk\release\app-arm64-v8a-release.apk"
if not exist "%APK%" (
    echo [ERROR] APK not found: %APK%
    exit /b 1
)
set "ADB="
if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
if not defined ADB if defined ANDROID_SDK_ROOT if exist "%ANDROID_SDK_ROOT%\platform-tools\adb.exe" set "ADB=%ANDROID_SDK_ROOT%\platform-tools\adb.exe"
if not defined ADB if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not defined ADB (
    where adb >nul 2>&1 && set "ADB=adb"
)
if not defined ADB (
    echo [ERROR] adb not found. Connect a device and ensure Android SDK platform-tools is installed.
    exit /b 1
)
echo Installing: %APK%
"%ADB%" devices
"%ADB%" install -r "%APK%"
exit /b %ERRORLEVEL%

:usage
echo Usage: build-local.bat [gradle task...]
echo.
echo Default (no args):  :app:assembleRelease
echo   Then install:     app\build\outputs\apk\release\app-arm64-v8a-release.apk
echo.
echo Size notes:
echo   release arm64     ~26-35 MB   ^<-- use this on phones
echo   release universal ~65 MB      skip unless you need all ABIs
echo   debug arm64       ~70 MB      only for debugging
echo.
echo Other tasks:
echo   :app:assembleDebug       debug APK (.dev suffix)
echo   :app:assembleFoss        FOSS release variant
echo   :app:installRelease      build+install (may pick wrong ABI split)
echo   :app:testDebugUnitTest   unit tests
echo   clean                    clean outputs
echo.
echo Prefer: build-local.bat
echo     or: build-local.bat :app:assembleRelease
echo Then:   adb install -r app\build\outputs\apk\release\app-arm64-v8a-release.apk
exit /b 0

:nojdk
echo [ERROR] JDK 21 not found at: %JAVA_HOME%
echo         Edit JAVA_HOME in this script.
exit /b 1
