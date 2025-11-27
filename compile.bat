@echo off
REM Compile all Java source files
echo   Compiling Java Source Files...

REM If compilation fails, replace "lib\javafx\*" with your JavaFX path:
REM javac -cp "lib\*;C:\path\to\javafx\lib\*" -d bin src\*.java

echo.
echo [INFO] Compiling source files from src/ to bin/...
javac -cp "lib\*;lib\javafx\*" -d bin src\*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Compilation completed successfully!
    exit /b 0
) else (
    echo.
    echo [ERROR] Compilation failed! Check errors above.
    exit /b 1
)
