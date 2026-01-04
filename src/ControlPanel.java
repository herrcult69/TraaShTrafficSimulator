import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

/**
 * Main control panel for simulation control, view manipulation, and dashboard display.
 * Provides buttons for play/pause/stop, zoom, and access to specialized panels.
 * 
 * @author M A T^2 H Team
 * @version 2.0
 * @see SimulationRunner
 * @see ViewManager
 */
public class ControlPanel {
    private VBox controlPanel;
    private ScrollPane scrollPane;
    private SimulationRunner runner;
    private ViewManager viewManager;
    private TrafficManager trafficManager;
    private VehicleAddPanel vehicleAddPanel;

    // Callbacks for route selection mode
    private Runnable onStartRouteSelection;
    private Consumer<Boolean> onRouteSelectionModeChange;
    private Runnable onVehicleAdded;

    /**
     * Constructs a new control panel.
     * 
     * @param runner The simulation runner
     * @param viewManager The view manager
     * @param dashboard The dashboard component
     * @param trafficManager The traffic manager
     */
    public ControlPanel(SimulationRunner runner, ViewManager viewManager, DashBoard dashboard,
            TrafficManager trafficManager) {
        this.runner = runner;
        this.viewManager = viewManager;
        this.trafficManager = trafficManager;
        createPanel(dashboard);
    }

    /**
     * Creates the main panel UI.
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

        // Add separator
        controlPanel.getChildren().add(new Separator());

        // Add dashboard
        controlPanel.getChildren().add(dashboard);

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
     * Adds simulation control buttons to the panel.
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
     * @param onRouteSelectionModeChange Called when route selection mode changes
     * @param onVehicleAdded Called when a vehicle is added
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
     * Returns the current vehicle add panel.
     * 
     * @return The active vehicle add panel, or null
     */
    public VehicleAddPanel getVehicleAddPanel() {
        return vehicleAddPanel;
    }

    /**
     * Displays the traffic light control panel.
     * 
     * @param tl The traffic light to control
     */
    public void showTrafficLightControl(TrafficLight tl) {
        TrafficLightControlPanel tlPanel = new TrafficLightControlPanel(tl, runner, trafficManager, this::showNormalControls);
        scrollPane.setContent(tlPanel.getPanel());
    }

    /**
     * Restores the normal control panel view.
     */
    public void showNormalControls() {
        vehicleAddPanel = null; // Clear reference
        scrollPane.setContent(controlPanel);
    }
}
