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
 * Control panel for simulation and view controls
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

    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    /**
     * Set callbacks for route selection mode
     */
    public void setRouteSelectionCallbacks(Runnable onStartRouteSelection, Consumer<Boolean> onRouteSelectionModeChange, Runnable onVehicleAdded) {
        this.onStartRouteSelection = onStartRouteSelection;
        this.onRouteSelectionModeChange = onRouteSelectionModeChange;
        this.onVehicleAdded = onVehicleAdded;
    }

    /**
     * Show the vehicle add panel
     */
    public void showVehicleAddPanel() {
        vehicleAddPanel = new VehicleAddPanel(runner, this::showNormalControls,
                onStartRouteSelection, onRouteSelectionModeChange, onVehicleAdded);
        scrollPane.setContent(vehicleAddPanel);
    }

    /**
     * Get the current vehicle add panel (for adding edges to route)
     */
    public VehicleAddPanel getVehicleAddPanel() {
        return vehicleAddPanel;
    }

    public void showTrafficLightControl(TrafficLight tl) {
        TrafficLightControlPanel tlPanel = new TrafficLightControlPanel(tl, runner, trafficManager, this::showNormalControls);
        scrollPane.setContent(tlPanel.getPanel());
    }

    public void showNormalControls() {
        vehicleAddPanel = null; // Clear reference
        scrollPane.setContent(controlPanel);
    }
}
