@echo off
REM Clean compiled class files
echo.
echo Cleaning Compiled Files...
echo.
if exist bin (
    echo Removing .class files from bin/...
    del /Q bin\*.class 2>nul
    echo Cleaned all .class files from bin directory.
) else (
    echo Bin directory does not exist.
)
echo.
