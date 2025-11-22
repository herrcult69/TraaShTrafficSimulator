@echo off
REM Compile and Run in one command
echo.
echo Traffic Simulator - Build and Run
echo.
echo [PHASE 1/3] Cleaning old compiled files...
echo.
call clean.bat
echo.

echo.
echo [PHASE 2/3]Compiling source files...
echo.
call compile.bat
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR]Build failed! Stopping execution.
    echo.
    pause
    exit /b 1
)

echo.
echo [PHASE 3/3]Launching application...
echo.
call run.bat
echo.
echo [INFO] Application closed.
echo.
