@echo off
REM ============================================================
REM  Mihon local build script for Windows
REM
REM  NOTE: this machine's default JVM is JDK 17, but Mihon requires
REM  JDK 21 (the metro 1.4.5 Gradle plugin refuses to run on 17).
REM  So JAVA_HOME is overridden below.
REM
REM  This file is NOT tracked by git. Edit or delete freely.
REM ============================================================
setlocal

set "JAVA_HOME=C://Users//%USERNAME%//.workbuddy//binaries//jdk//21//jdk-21.0.12.1+1"
if not exist "%JAVA_HOME%\bin\java.exe" goto nojdk

REM Make dead connections fail fast instead of stalling the whole build.
set "GRADLE_OPTS=-Dorg.gradle.internal.http.connectionTimeout=30000 -Dorg.gradle.internal.http.socketTimeout=30000"

if "%~1"=="" goto usage

echo Using JDK: %JAVA_HOME%
call "%~dp0gradlew.bat" %* --console=plain
exit /b %ERRORLEVEL%

:usage
echo Usage: build-local.bat ^<gradle task^>
echo.
echo Common tasks:
echo   :app:assembleDebug     build debug APK, installs alongside official Mihon
echo   :app:assembleFoss      build FOSS variant
echo   :app:installDebug      build and install to connected device
echo   :app:testDebugUnitTest run unit tests
echo   clean                  clean build outputs
echo.
echo Example: build-local.bat :app:assembleDebug
exit /b 0

:nojdk
echo [ERROR] JDK 21 not found at: %JAVA_HOME%
echo         Edit JAVA_HOME in this script.
exit /b 1
