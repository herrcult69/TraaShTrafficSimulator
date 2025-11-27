@echo off
REM Run the Traffic Simulator Application

REM If JavaFX modules are not found, replace "lib\javafx" with your JavaFX path:
REM java --module-path "C:\path\to\javafx\lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "lib\TraaS.jar;bin" TrafficSimulatorApp

echo [INFO] Loading JavaFX modules...
echo [INFO] Initializing application...
echo.
java --module-path "lib\javafx" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "lib\TraaS.jar;bin" TrafficSimulatorApp
