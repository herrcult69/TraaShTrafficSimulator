import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javafx.stage.FileChooser;
import java.io.File;

/**
 * Main control panel for simulation control, view manipulation, and dashboard
 * display.
 * Provides buttons for play/pause/stop, zoom, and access to specialized panels.
 * 
 * @author M A T^2 H Team
 * @version 2.0
 * @see SimulationRunner
 * @see ViewManager
 */
public class ControlPanel {
    private static final Logger logger = Logger.getLogger(ControlPanel.class.getName());
    
    private VBox controlPanel;
    private ScrollPane scrollPane;
    private SimulationRunner runner;
    private ViewManager viewManager;
    private TrafficManager trafficManager;
    private VehicleAddPanel vehicleAddPanel;
    private CongestionMonitorPanel congestionMonitorPanel;
    private VehicleFilterPanel vehicleFilterPanel;
    private TrafficLight currentTrafficLight;

    // Callbacks for route selection mode
    private Runnable onStartRouteSelection;
    private Consumer<Boolean> onRouteSelectionModeChange;
    private Runnable onVehicleAdded;
    private StatisticsWindow statsWindow;
    /**
     * Constructs the control panel.
     * 
     * @param runner Simulation runner
     * @param viewManager View manager
     * @param dashboard Dashboard component
     * @param trafficManager Traffic manager
     */
    public ControlPanel(SimulationRunner runner, ViewManager viewManager, DashBoard dashboard,
            TrafficManager trafficManager) {
        this.runner = runner;
        this.viewManager = viewManager;
        this.trafficManager = trafficManager;
        createPanel(dashboard);
    }

    private void createPanel(DashBoard dashboard) {
        controlPanel = new VBox(10);
        controlPanel.setAlignment(Pos.TOP_CENTER);

        // Add simulation controls
        addSimulationControls();

        // Add separator
        controlPanel.getChildren().add(new Separator());

        // Add view controls
        addViewControls();

        // Add statistics controls
        addStatisticsControl();
        // Add separator
        controlPanel.getChildren().add(new Separator());

        // Add dashboard
        controlPanel.getChildren().add(dashboard);
        
        // Add separator
        controlPanel.getChildren().add(new Separator());
        
        // Add filter and congestion buttons
        addFilterAndCongestionControls();
        
        // Initialize panels
        congestionMonitorPanel = new CongestionMonitorPanel();
        congestionMonitorPanel.setOnToggleOverlay(() -> {
            trafficManager.setShowCongestionOverlay(!trafficManager.isShowCongestionOverlay());
        });
        congestionMonitorPanel.setOnBackPressed(this::showNormalControls);
        vehicleFilterPanel = new VehicleFilterPanel(this::showNormalControls);

        // Style the panel
        controlPanel.setPadding(new Insets(10));
        controlPanel.setSpacing(8);
        controlPanel.setStyle("-fx-background-color: " + UIStyles.BG_PRIMARY + ";");
        controlPanel.setMinWidth(300);
        controlPanel.setMaxWidth(300);

        // Wrap in ScrollPane
        scrollPane = new ScrollPane(controlPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMinWidth(300);
        scrollPane.setMaxWidth(300);
        scrollPane.setStyle(
                "-fx-background: " + UIStyles.BG_PRIMARY + "; -fx-background-color: " + UIStyles.BG_PRIMARY + ";");
    }

    private void addSimulationControls() {
        Label simLabel = new Label("―――SIMULATION―――");
        simLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");

        Button playBtn = UIStyles.createStyledButton("[>] Play");
        Button pauseBtn = UIStyles.createStyledButton("[=] Pause");
        Button stopBtn = UIStyles.createStyledButton("[#] Stop");
        Button addVehicleBtn = UIStyles.createAccentButton("[@] Add Vehicle");

        playBtn.setOnAction(e -> {
            if (runner != null) {
                runner.resume();
                logger.info("Simulation resumed");
            }
        });

        pauseBtn.setOnAction(e -> {
            if (runner != null) {
                runner.pause();
                logger.info("Simulation paused");
            }
        });

        stopBtn.setOnAction(e -> {
            if (runner != null) {
                runner.stop();
            }
            logger.info("Simulation stopped - Exiting application");
            Platform.exit();
            System.exit(0);
        });

        addVehicleBtn.setOnAction(e -> showVehicleAddPanel());

        controlPanel.getChildren().addAll(simLabel, playBtn, pauseBtn, stopBtn, addVehicleBtn);
    }

    /**
     * Adds filter and congestion monitoring buttons.
     */
    private void addFilterAndCongestionControls() {
        Label filterLabel = new Label("―――FILTERS & MONITORING―――");
        filterLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        
        Button vehicleFilterBtn = UIStyles.createStyledButton("Vehicle Filters");
        Button congestionBtn = UIStyles.createStyledButton("Congestion Monitor");
        
        vehicleFilterBtn.setOnAction(e -> showVehicleFilterPanel());
        congestionBtn.setOnAction(e -> showCongestionMonitorPanel());
        
        controlPanel.getChildren().addAll(filterLabel, vehicleFilterBtn, congestionBtn);
    }
    
    /**
     * Adds view control buttons to the panel.
     */
    private void addViewControls() {
        Label viewLabel = new Label("―――VIEW―――");
        viewLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");

        Button zoomIn = UIStyles.createStyledButton("+ Zoom In");
        Button zoomOut = UIStyles.createStyledButton("- Zoom Out");
        Button reset = UIStyles.createStyledButton("⟲ Reset View");

        zoomIn.setOnAction(e -> viewManager.zoomToCenter(1.2));
        zoomOut.setOnAction(e -> viewManager.zoomToCenter(0.8));
        reset.setOnAction(e -> viewManager.resetView());

        controlPanel.getChildren().addAll(viewLabel, zoomIn, zoomOut, reset);
    }

    /**
     * Adds statistics control buttons (view stats, export csv)
     * @return The View Statistics section
     */
    private void addStatisticsControl() {
        Label statsLabel = new Label ("---Statistics---");
        statsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14");

        Button viewStatsButton = UIStyles.createAccentButton("View Live Statistics");
        Button exportCSV = UIStyles.createStyledButton("Export to CSV");

        viewStatsButton.setOnAction(e -> showStatisticsWindow());
        exportCSV.setOnAction(e -> exportCSVData());

        controlPanel.getChildren().addAll(statsLabel, viewStatsButton, exportCSV);
    }
    private void exportCSVData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Simulation Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("Simulation_data.csv");
        
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                String filePath = file.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".csv")) {
                    filePath += ".csv";
                }
                TrafficDataExporter.exportToCSV(filePath, runner, trafficManager, vehicleFilterPanel);
                System.out.println("Simulation data exported to: " + filePath);
                
                // Show success alert
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("CSV exported successfully to:\n" + file.getName());
                alert.showAndWait();
            } catch (java.io.IOException ex) {
                System.err.println("Error exporting CSV: " + ex.getMessage());
                ex.printStackTrace();
                
                // Show error alert
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText(null);
                alert.setContentText("Failed to export CSV:\n" + ex.getMessage());
                alert.showAndWait();
            }
        }
    }
    private void showStatisticsWindow() {
        if (statsWindow == null || !statsWindow.isShowing()) {
            statsWindow = new StatisticsWindow(runner, trafficManager, vehicleFilterPanel);
            statsWindow.show();
        } else {
            statsWindow.toFront();  // Bring existing window to front
        }
        System.out.println("Statistics window opened");
    }
    /**
     * Returns the scroll pane containing the control panel.
     * 
     * @return The scrollable control panel
     */
    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    /**
     * Sets callbacks for route selection mode interactions.
     * 
     * @param onStartRouteSelection      Called when route selection begins
     * @param onRouteSelectionModeChange Called when route selection mode changes
     * @param onVehicleAdded             Called when a vehicle is added
     */
    public void setRouteSelectionCallbacks(Runnable onStartRouteSelection, Consumer<Boolean> onRouteSelectionModeChange,
            Runnable onVehicleAdded) {
        this.onStartRouteSelection = onStartRouteSelection;
        this.onRouteSelectionModeChange = onRouteSelectionModeChange;
        this.onVehicleAdded = onVehicleAdded;
    }

    /**
     * Displays the vehicle addition panel, replacing the normal control panel.
     */
    public void showVehicleAddPanel() {
        vehicleAddPanel = new VehicleAddPanel(runner, this::showNormalControls,
                onStartRouteSelection, onRouteSelectionModeChange, onVehicleAdded);
        scrollPane.setContent(vehicleAddPanel);
    }

    /**
     * Returns the current vehicle add panel.
     * 
     * @return The active vehicle add panel, or null
     */
    public VehicleAddPanel getVehicleAddPanel() {
        return vehicleAddPanel;
    }

    /**
     * Displays the traffic light control panel.
     * If already showing a traffic light panel, just update to the new light.
     * 
     * @param tl The traffic light to control
     */
    public void showTrafficLightControl(TrafficLight tl) {
        currentTrafficLight = tl;
        TrafficLightControlPanel tlPanel = new TrafficLightControlPanel(tl, runner, trafficManager,
                this::showNormalControls);
        scrollPane.setContent(tlPanel.getPanel());
    }
    
    /**
     * Shows the vehicle filter panel.
     */
    public void showVehicleFilterPanel() {
        scrollPane.setContent(vehicleFilterPanel.getPanel());
    }
    
    /**
     * Shows the congestion monitor panel.
     */
    public void showCongestionMonitorPanel() {
        scrollPane.setContent(congestionMonitorPanel);
    }

    /**
     * Restores the normal control panel view.
     */
    public void showNormalControls() {
        vehicleAddPanel = null; // Clear reference
        currentTrafficLight = null; // Clear traffic light reference
        scrollPane.setContent(controlPanel);
    }
    
    /**
     * Returns the vehicle filter panel.
     * 
     * @return The vehicle filter panel
     */
    public VehicleFilterPanel getVehicleFilterPanel() {
        return vehicleFilterPanel;
    }
    
    /**
     * Checks if currently showing a traffic light control panel.
     * 
     * @return true if traffic light panel is active
     */
    public boolean isShowingTrafficLightPanel() {
        return currentTrafficLight != null;
    }
    
    /**
     * Returns the congestion monitor panel for updating hotspot data.
     * 
     * @return The congestion monitor panel
     */
    public CongestionMonitorPanel getCongestionMonitorPanel() {
        return congestionMonitorPanel;
    }
}
