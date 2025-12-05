@echo off
REM Run the Traffic Simulator Application
echo.
echo [96m [96mStarting Traffic Simulator...[0m
echo.
echo [INFO] Loading JavaFX modules...
echo [INFO] Initializing application...
echo.
java --module-path "D:\Trac\SumoProject\javafx-sdk-17.0.17\lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "lib\TraaS.jar;bin" TrafficSimulatorApp
