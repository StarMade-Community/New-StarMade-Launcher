@echo off
:: StarMade Dedicated Server Launch Script for Windows
:: This script launches the StarMade Launcher JAR in server mode

setlocal enabledelayedexpansion

:: Get the directory where this script is located
set SCRIPT_DIR=%~dp0
cd "%SCRIPT_DIR%"

:: Check if StarMade directory exists
if not exist "StarMade" (
    echo StarMade directory not found.
    echo Please run the StarMade Launcher first to download the game.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)

:: Check if launcher JAR exists
if not exist "StarMade-Launcher.jar" (
    echo StarMade-Launcher.jar not found.
    echo Please ensure the launcher is properly installed.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)

:: Check if Java 23 exists
set JAVA_PATH=%SCRIPT_DIR%jre23\bin\java.exe
if not exist "%JAVA_PATH%" (
    echo Java 23 runtime not found at %JAVA_PATH%
    echo Please run the StarMade Launcher first to install the required Java runtime.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)

:: Determine port (default 4242)
set PORT=4242
if not "%~1"=="" (
    set PORT=%~1
)

echo Starting StarMade Server on port %PORT%...
echo.

:: Launch the launcher JAR in server mode
"%JAVA_PATH%" -jar StarMade-Launcher.jar -server -port: %PORT%

echo.
echo Server has stopped.
echo Press any key to exit...
pause >nul
exit /b 0