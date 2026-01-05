import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

import javafx.stage.FileChooser;
import java.io.File;

/**
 * Main control panel for the simulation interface.
 * Provides buttons for simulation control (play, pause, stop), view manipulation (zoom, pan),
 * and access to specialized panels (vehicle addition, traffic light control).
 * 
 * <p>The panel is scrollable and maintains callbacks for route selection interactions.
 * The control panel includes:
 * <ul>
 *   <li>Simulation control buttons: Play, Pause, Stop, Add Vehicle</li>
 *   <li>View control buttons: Zoom In, Zoom Out, Reset View</li>
 *   <li>Integration with DashBoard for real-time metrics display</li>
 *   <li>Ability to switch to VehicleAddPanel and TrafficLightControlPanel</li>
 * </ul>
 * 
 * <p>Callbacks can be set for route selection mode to notify when route selection starts,
 * when the mode changes, and when a vehicle is added.</p>
 * 
 * @author M A T^2 H Team
 * @version 2.0
 * @see SimulationRunner
 * @see ViewManager
 * @see DashBoard
 * @see VehicleAddPanel
 * @see TrafficLightControlPanel
 * @see TrafficLight
 */
public class ControlPanel {
    private VBox controlPanel;
    private ScrollPane scrollPane;
    private SimulationRunner runner;
    private ViewManager viewManager;
    private TrafficManager trafficManager;
    private VehicleAddPanel vehicleAddPanel;
    private CongestionMonitorPanel congestionMonitorPanel;

    // Callbacks for route selection mode
    private Runnable onStartRouteSelection;
    private Consumer<Boolean> onRouteSelectionModeChange;
    private Runnable onVehicleAdded;
    private StatisticsWindow statsWindow;
    /**
     * Constructs a new control panel with simulation and view controls.
     * 
     * @param runner The simulation runner for play/pause/stop control
     * @param viewManager The view manager for zoom and pan operations
     * @param dashboard The dashboard to display in the panel
     * @param trafficManager The traffic manager for accessing traffic light data
     */
    public ControlPanel(SimulationRunner runner, ViewManager viewManager, DashBoard dashboard,
            TrafficManager trafficManager) {
        this.runner = runner;
        this.viewManager = viewManager;
        this.trafficManager = trafficManager;
        createPanel(dashboard);
    }

    /**
     * Creates the main panel UI with simulation controls, view controls, and dashboard.
     * 
     * @param dashboard The dashboard component to include
     */
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
        
        // Add congestion monitor panel
        congestionMonitorPanel = new CongestionMonitorPanel();
        congestionMonitorPanel.setOnToggleOverlay(() -> {
            trafficManager.setShowCongestionOverlay(!trafficManager.isShowCongestionOverlay());
        });
        controlPanel.getChildren().add(congestionMonitorPanel);

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
        scrollPane.setStyle("-fx-background: " + UIStyles.BG_PRIMARY + "; -fx-background-color: " + UIStyles.BG_PRIMARY + ";");
    }
    /**
     * Adds simulation control buttons (play, pause, stop, add vehicle) to the panel.
     */
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
                System.out.println("Simulation resumed");
            }
        });

        pauseBtn.setOnAction(e -> {
            if (runner != null) {
                runner.pause();
                System.out.println("Simulation paused");
            }
        });

        stopBtn.setOnAction(e -> {
            if (runner != null) {
                runner.stop();
            }
            System.out.println("Simulation stopped - Exiting application");
            Platform.exit();
            System.exit(0);
        });

        addVehicleBtn.setOnAction(e -> showVehicleAddPanel());

        controlPanel.getChildren().addAll(simLabel, playBtn, pauseBtn, stopBtn, addVehicleBtn);
    }

    /**
     * Adds view control buttons (zoom in, zoom out, reset) to the panel.
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
     * Adds statistics control buttons (view stats, export csv, export pdf)
     * @return The View Statistics section
     */
    private void addStatisticsControl() {
        Label statsLabel = new Label ("---Statistics---");
        statsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14");

        Button viewStatsButton = UIStyles.createAccentButton("View Live Statistics");
        Button exportCSV = UIStyles.createStyledButton("Export to CSV");
        Button exportPDF = UIStyles.createStyledButton("Export to PDF");
        exportPDF.setDisable(true);

        viewStatsButton.setOnAction(e -> showStatisticsWindow());
        exportCSV.setOnAction(e -> exportTrafficData());

        controlPanel.getChildren().addAll(statsLabel, viewStatsButton, exportCSV, exportPDF);
    }
    private void exportTrafficData() {
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
                TrafficDataExporter.exportToCSV(filePath, runner, trafficManager);
                System.out.println("Simulation data exported to: " + filePath);
            } catch (java.io.IOException ex) {
                System.err.println("Error exporting CSV: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
    private void showStatisticsWindow() {
        if (statsWindow == null || !statsWindow.isShowing()) {
            statsWindow = new StatisticsWindow(runner, trafficManager);
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
     * @param onStartRouteSelection Called when route selection begins
     * @param onRouteSelectionModeChange Called when route selection mode changes (true = enabled)
     * @param onVehicleAdded Called when a vehicle is successfully added
     */
    public void setRouteSelectionCallbacks(Runnable onStartRouteSelection, Consumer<Boolean> onRouteSelectionModeChange, Runnable onVehicleAdded) {
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
     * Returns the current vehicle add panel for edge selection during route creation.
     * 
     * @return The active vehicle add panel, or null if not showing
     */
    public VehicleAddPanel getVehicleAddPanel() {
        return vehicleAddPanel;
    }

    /**
     * Displays the traffic light control panel for the selected traffic light.
     * 
     * @param tl The traffic light to control
     */
    public void showTrafficLightControl(TrafficLight tl) {
        TrafficLightControlPanel tlPanel = new TrafficLightControlPanel(tl, runner, trafficManager, this::showNormalControls);
        scrollPane.setContent(tlPanel.getPanel());
    }

    /**
     * Restores the normal control panel view, hiding any specialized panels.
     */
    public void showNormalControls() {
        vehicleAddPanel = null; // Clear reference
        scrollPane.setContent(controlPanel);
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
