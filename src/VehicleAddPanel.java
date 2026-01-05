import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Panel for adding vehicles to the simulation interactively.
 * Supports route selection, auto-generated IDs, and stress testing.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see SimulationRunner
 * @see TraaSAdapter
 */
public class VehicleAddPanel extends VBox {
    private static final Logger logger = Logger.getLogger(VehicleAddPanel.class.getName());

    // Vehicle type options
    private static final String[] VEHICLE_TYPES = { "car", "truck", "bus", "motorcycle", "emergency" };
    private static final String[] VEHICLE_CLASSES = { "passenger", "truck", "bus", "motorcycle", "emergency" };

    // UI components
    private ComboBox<String> vehicleTypeCombo;
    private TextField vehicleIdField;
    private TextField vehicleBatchField;
    private TextField vehicleSpeedField;
    private TextField vehicleColorField;
    private ListView<String> routeListView;
    private Label statusLabel;
    private Label instructionLabel;
    private Label startEdgeLabel;
    private Label endEdgeLabel;
    private Button addEdgeBtn;
    private Button clearRouteBtn;
    private Button confirmBtn;
    private Button cancelBtn;

    // Stress test components
    /**
     * Adjusting the value RAND_ITERATIONS and RAND_MAX_DISTANCE in createMap.sh
     * to recommended values is advised for better stress test.
     * Rerun the createMap.sh script after changing those values.
     */
    private Button stressTestBtn;
    private TextField stressIntervalField;
    private TextField stressBatchField;
    private Label stressStatusLabel;
    private Label stressStatsLabel;
    private Label fpsLabel;
    private Timer stressTestTimer;
    private boolean stressTestRunning = false;
    private int stressVehiclesAdded = 0;
    private int stressVehiclesFailed = 0;
    private long stressTestStartTime = 0;
    private Random random = new Random();
    private List<String> availableEdges = null;

    // FPS counter
    private AnimationTimer fpsTimer;
    private long[] frameTimes = new long[100];
    private int frameTimeIndex = 0;
    private boolean frameTimesArrayFull = false;

    // Route data
    private List<String> selectedRoute;
    private List<String> userSelectedEdges;
    private String startEdge = null;
    private String endEdge = null;

    // Dependencies
    private SimulationRunner runner;
    private Runnable onCancel;
    private Runnable onStartRouteSelection;
    private java.util.function.Consumer<Boolean> onRouteSelectionModeChange;
    private Runnable onVehicleAdded;

    // State
    private boolean routeSelectionMode = false;
    private static int vehicleCounter = 1;

    /**
     * Constructs a new vehicle addition panel.
     * 
     * @param runner                     The simulation runner
     * @param onCancel                   Callback invoked on cancel
     * @param onStartRouteSelection      Callback invoked when route selection
     *                                   starts
     * @param onRouteSelectionModeChange Callback for route selection mode changes
     * @param onVehicleAdded             Callback invoked when vehicle is added
     */
    public VehicleAddPanel(SimulationRunner runner, Runnable onCancel,
            Runnable onStartRouteSelection, java.util.function.Consumer<Boolean> onRouteSelectionModeChange,
            Runnable onVehicleAdded) {
        super(12);
        this.runner = runner;
        this.onCancel = onCancel;
        this.onStartRouteSelection = onStartRouteSelection;
        this.onRouteSelectionModeChange = onRouteSelectionModeChange;
        this.onVehicleAdded = onVehicleAdded;
        this.selectedRoute = new ArrayList<>();
        this.userSelectedEdges = new ArrayList<>();

        createUI();
    }

    /**
     * Creates the complete UI for the vehicle addition panel.
     */
    private void createUI() {
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(15));
        setStyle("-fx-background-color: " + UIStyles.BG_PRIMARY + ";");
        setSpacing(10);

        // Back button
        Button backBtn = UIStyles.createStyledButton("← Back");
        backBtn.setOnAction(e -> {
            exitRouteSelectionMode();
            onCancel.run();
        });

        // Title
        Label titleLabel = new Label("ADD NEW VEHICLE");
        titleLabel.setStyle(UIStyles.TITLE_STYLE);

        // Vehicle Type Selection
        Label typeLabel = new Label("Vehicle Type:");
        typeLabel.setStyle(UIStyles.LABEL_STYLE);

        vehicleTypeCombo = new ComboBox<>();
        vehicleTypeCombo.getItems().addAll(VEHICLE_TYPES);
        vehicleTypeCombo.setValue(VEHICLE_TYPES[0]);
        vehicleTypeCombo.setPrefWidth(250);
        vehicleTypeCombo.setStyle(UIStyles.COMBO_BOX_STYLE);
        vehicleTypeCombo.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white;");
                }
            }
        });
        vehicleTypeCombo.setOnAction(e -> updateVehicleId());

        // Vehicle ID
        Label idLabel = new Label("Vehicle ID (auto-generated):");
        idLabel.setStyle(UIStyles.LABEL_STYLE);

        vehicleIdField = new TextField();
        vehicleIdField.setPrefWidth(250);
        vehicleIdField.setStyle(UIStyles.INPUT_FIELD_STYLE + " -fx-font-family: monospace;");
        updateVehicleId();

        // Vehicle Parameters
        Label batchLabel = new Label("Batch count:");
        batchLabel.setStyle(UIStyles.LABEL_STYLE);
        vehicleBatchField = new TextField("1");
        vehicleBatchField.setPrefWidth(80);
        vehicleBatchField.setStyle(UIStyles.INPUT_FIELD_STYLE + " -fx-font-family: monospace;");
        vehicleBatchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                vehicleBatchField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        Label speedLabel = new Label("Depart speed (m/s):");
        speedLabel.setStyle(UIStyles.LABEL_STYLE);
        vehicleSpeedField = new TextField("0");
        vehicleSpeedField.setPrefWidth(80);
        vehicleSpeedField.setStyle(UIStyles.INPUT_FIELD_STYLE + " -fx-font-family: monospace;");
        vehicleSpeedField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[0-9]*([.]?[0-9]*)?")) {
                vehicleSpeedField.setText(oldVal);
            }
        });

        HBox paramsRow = new HBox(10);
        paramsRow.setAlignment(Pos.CENTER_LEFT);
        paramsRow.getChildren().addAll(batchLabel, vehicleBatchField, speedLabel, vehicleSpeedField);

        Label colorLabel = new Label("Color (hex #RRGGBB, optional):");
        colorLabel.setStyle(UIStyles.LABEL_STYLE);
        vehicleColorField = new TextField("");
        vehicleColorField.setPrefWidth(250);
        vehicleColorField.setStyle(UIStyles.INPUT_FIELD_STYLE + " -fx-font-family: monospace;");

        // Route Section
        Label routeLabel = new Label("Route Selection:");
        routeLabel.setStyle(UIStyles.LABEL_STYLE + " -fx-font-weight: bold;");

        instructionLabel = new Label("Click 'Select Route' then pick START and END edges");
        instructionLabel.setStyle("-fx-text-fill: " + UIStyles.TEXT_WARNING + "; -fx-font-size: 11;");
        instructionLabel.setWrapText(true);

        // Start/End edge labels
        startEdgeLabel = new Label("Start: (not selected)");
        startEdgeLabel.setStyle(UIStyles.LABEL_SECONDARY_STYLE);

        endEdgeLabel = new Label("End: (not selected)");
        endEdgeLabel.setStyle(UIStyles.LABEL_SECONDARY_STYLE);

        // Route list (shows computed path)
        Label computedRouteLabel = new Label("Computed Route:");
        computedRouteLabel.setStyle(UIStyles.LABEL_SECONDARY_STYLE);

        routeListView = new ListView<>();
        routeListView.setPrefHeight(100);
        routeListView.setStyle("-fx-background-color: " + UIStyles.BG_SECONDARY + "; -fx-control-inner-background: "
                + UIStyles.BG_SECONDARY + ";");

        // Route action buttons
        HBox routeButtons = new HBox(8);
        routeButtons.setAlignment(Pos.CENTER);

        addEdgeBtn = UIStyles.createStyledButton("Select Route");
        addEdgeBtn.setPrefWidth(120);
        UIStyles.applyAccentButtonStyle(addEdgeBtn);
        addEdgeBtn.setOnAction(e -> toggleRouteSelectionMode());

        clearRouteBtn = UIStyles.createStyledButton("🗑 Clear");
        clearRouteBtn.setPrefWidth(120);
        clearRouteBtn.setOnAction(e -> clearRoute());

        routeButtons.getChildren().addAll(addEdgeBtn, clearRouteBtn);

        // Status label
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11;");
        statusLabel.setWrapText(true);

        // Confirm/Cancel buttons
        confirmBtn = UIStyles.createAccentButton("✓ Add Vehicle");
        confirmBtn.setOnAction(e -> addVehicle());

        cancelBtn = UIStyles.createStyledButton("✕ Cancel");
        cancelBtn.setOnAction(e -> {
            stopStressTest();
            exitRouteSelectionMode();
            onCancel.run();
        });

        // Stress Test Section
        Separator stressSeparator = new Separator();
        stressSeparator.setStyle("-fx-background-color: #415A77;");

        Label stressTestTitle = new Label("STRESS TEST");
        stressTestTitle.setStyle(UIStyles.TITLE_STYLE + " -fx-font-size: 14;");

        Label stressDescLabel = new Label("Continuously inject vehicles with random routes");
        stressDescLabel.setStyle(UIStyles.LABEL_SECONDARY_STYLE);
        stressDescLabel.setWrapText(true);

        Label intervalLabel = new Label("Interval (ms):");
        intervalLabel.setStyle(UIStyles.LABEL_STYLE);

        stressIntervalField = new TextField("500");
        stressIntervalField.setPrefWidth(80);
        stressIntervalField.setStyle(UIStyles.INPUT_FIELD_STYLE + " -fx-font-family: monospace;");
        stressIntervalField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                stressIntervalField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        HBox intervalBox = new HBox(10);
        intervalBox.setAlignment(Pos.CENTER_LEFT);
        intervalBox.getChildren().addAll(intervalLabel, stressIntervalField);

        Label stressBatchLabel = new Label("Batch (veh/interval):");
        stressBatchLabel.setStyle(UIStyles.LABEL_STYLE);
        stressBatchField = new TextField("1");
        stressBatchField.setPrefWidth(80);
        stressBatchField.setStyle(UIStyles.INPUT_FIELD_STYLE + " -fx-font-family: monospace;");
        stressBatchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                stressBatchField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
        HBox stressBatchBox = new HBox(10);
        stressBatchBox.setAlignment(Pos.CENTER_LEFT);
        stressBatchBox.getChildren().addAll(stressBatchLabel, stressBatchField);

        stressTestBtn = UIStyles.createStyledButton("Start Stress Test");
        stressTestBtn.setPrefWidth(200);
        stressTestBtn.setStyle(
                "-fx-background-color: #E53935; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10;");
        stressTestBtn.setOnMouseEntered(e -> {
            if (!stressTestRunning)
                stressTestBtn.setStyle(
                        "-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10;");
        });
        stressTestBtn.setOnMouseExited(e -> {
            if (!stressTestRunning)
                stressTestBtn.setStyle(
                        "-fx-background-color: #E53935; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10;");
        });
        stressTestBtn.setOnAction(e -> toggleStressTest());

        stressStatusLabel = new Label("Ready to start");
        stressStatusLabel.setStyle("-fx-text-fill: " + UIStyles.TEXT_SECONDARY + "; -fx-font-size: 11;");

        stressStatsLabel = new Label("");
        stressStatsLabel.setStyle(
                "-fx-text-fill: " + UIStyles.TEXT_WARNING + "; -fx-font-size: 11; -fx-font-family: monospace;");
        stressStatsLabel.setWrapText(true);

        fpsLabel = new Label("");
        fpsLabel.setStyle(
                "-fx-text-fill: #00E676; -fx-font-size: 14; -fx-font-weight: bold; -fx-font-family: monospace;");

        // Info box with instructions
        VBox infoBox = createInfoBox();

        // Add all components
        getChildren().addAll(
                backBtn, titleLabel, new Separator(),
                typeLabel, vehicleTypeCombo,
                idLabel, vehicleIdField,
                paramsRow,
                colorLabel, vehicleColorField,
                new Separator(),
                routeLabel, instructionLabel,
                startEdgeLabel, endEdgeLabel,
                computedRouteLabel, routeListView,
                routeButtons, statusLabel, new Separator(),
                confirmBtn, cancelBtn,
                stressSeparator, stressTestTitle, stressDescLabel,
                intervalBox, stressBatchBox, stressTestBtn, stressStatusLabel, stressStatsLabel, fpsLabel,
                infoBox);
    }

    private Color parseHexColor(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (s.isEmpty()) return null;
        if (!s.startsWith("#") || s.length() != 7) {
            throw new IllegalArgumentException("Color must be in format #RRGGBB");
        }
        int r = Integer.parseInt(s.substring(1, 3), 16);
        int g = Integer.parseInt(s.substring(3, 5), 16);
        int b = Integer.parseInt(s.substring(5, 7), 16);
        return Color.rgb(r, g, b);
    }

    /**
     * Creates an informational box with step-by-step instructions.
     * 
     * @return A VBox containing the help instructions
     */
    private VBox createInfoBox() {
        VBox infoBox = new VBox(5);
        infoBox.setStyle(UIStyles.INFO_BOX_STYLE);

        Label infoLabel = new Label("ℹ How to add a vehicle:");
        infoLabel.setStyle(UIStyles.INFO_LABEL_STYLE);

        String stepStyle = "-fx-text-fill: " + UIStyles.TEXT_SECONDARY + "; -fx-font-size: 10;";
        Label step1 = new Label("1. Select vehicle type");
        Label step2 = new Label("2. Click 'Select Route'");
        Label step3 = new Label("3. Click START edge (green)");
        Label step4 = new Label("4. Click END edge (red)");
        Label step5 = new Label("5. Route auto-computed!");
        Label step6 = new Label("6. Click 'Add Vehicle'");

        step1.setStyle(stepStyle);
        step2.setStyle(stepStyle);
        step3.setStyle(stepStyle);
        step4.setStyle(stepStyle);
        step5.setStyle(stepStyle);
        step6.setStyle(stepStyle);

        infoBox.getChildren().addAll(infoLabel, step1, step2, step3, step4, step5, step6);
        return infoBox;
    }

    /**
     * Updates the vehicle ID field based on selected type.
     */
    private void updateVehicleId() {
        String type = vehicleTypeCombo.getValue();
        String prefix = switch (type) {
            case "car" -> "car_new_";
            case "truck" -> "truck_new_";
            case "bus" -> "bus_new_";
            case "motorcycle" -> "moto_new_";
            case "emergency" -> "ambu_new_";
            default -> "vehicle_";
        };
        vehicleIdField.setText(prefix + vehicleCounter);
    }

    /**
     * Toggles between route selection mode and normal mode.
     */
    private void toggleRouteSelectionMode() {
        routeSelectionMode = !routeSelectionMode;

        if (routeSelectionMode) {
            enterRouteSelectionMode();
        } else {
            exitRouteSelectionModeUI();
        }
    }

    /**
     * Enters route selection mode and updates UI.
     */
    private void enterRouteSelectionMode() {
        // Pause simulation when entering route selection mode
        if (runner != null && !runner.isPaused()) {
            runner.pause();
            updateStatusLabel("Simulation paused for route selection", UIStyles.TEXT_WARNING);
        }

        addEdgeBtn.setText("✓ Done Selecting");
        addEdgeBtn.setStyle("-fx-background-color: #415A77; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 8;");

        if (startEdge == null) {
            updateInstructionLabel("Click on the START edge (where vehicle spawns)", UIStyles.TEXT_SUCCESS);
        } else if (endEdge == null) {
            updateInstructionLabel("Click on the END edge (destination)", UIStyles.TEXT_ERROR);
        }

        if (onRouteSelectionModeChange != null)
            onRouteSelectionModeChange.accept(true);
        if (onStartRouteSelection != null)
            onStartRouteSelection.run();
    }

    /**
     * Exits route selection mode and updates UI.
     */
    private void exitRouteSelectionModeUI() {
        addEdgeBtn.setText("📍 Select Route");
        addEdgeBtn.setStyle("-fx-background-color: #415A77; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 8;");

        if (selectedRoute.isEmpty() && startEdge != null && endEdge != null) {
            updateInstructionLabel("No valid route found. Try different edges.", UIStyles.TEXT_ERROR);
        } else if (!selectedRoute.isEmpty()) {
            updateInstructionLabel("Route computed: " + selectedRoute.size() + " edges", UIStyles.TEXT_SUCCESS);
        } else {
            updateInstructionLabel("Select start and end edges", UIStyles.TEXT_SECONDARY);
        }

        if (onRouteSelectionModeChange != null)
            onRouteSelectionModeChange.accept(false);
    }

    /**
     * Cleanly exits route selection mode.
     */
    private void exitRouteSelectionMode() {
        if (routeSelectionMode) {
            routeSelectionMode = false;
            if (onRouteSelectionModeChange != null)
                onRouteSelectionModeChange.accept(false);
        }
    }

    /**
     * Adds an edge to the route during route selection mode.
     * 
     * @param edgeId The ID of the clicked edge
     */
    public void addEdgeToRoute(String edgeId) {
        if (!routeSelectionMode)
            return;

        // First click = start edge
        if (startEdge == null) {
            startEdge = edgeId;
            userSelectedEdges.clear();
            userSelectedEdges.add(edgeId);
            updateEdgeLabelStyle(startEdgeLabel, "Start: " + edgeId, UIStyles.TEXT_SUCCESS, true);
            updateStatusLabel("Start edge selected. Now click END edge.", UIStyles.TEXT_SUCCESS);
            updateInstructionLabel("Click on the END edge (destination)", UIStyles.TEXT_ERROR);
            return;
        }

        // Second click = end edge (compute route)
        if (endEdge == null && !edgeId.equals(startEdge)) {
            endEdge = edgeId;
            userSelectedEdges.add(edgeId);
            updateEdgeLabelStyle(endEdgeLabel, "End: " + edgeId, UIStyles.TEXT_ERROR, true);
            computeRoute();
            return;
        }

        // If clicking again, allow changing the end edge
        if (endEdge != null && !edgeId.equals(startEdge)) {
            endEdge = edgeId;
            userSelectedEdges.set(1, edgeId);
            updateEdgeLabelStyle(endEdgeLabel, "End: " + edgeId, UIStyles.TEXT_ERROR, true);
            computeRoute();
        }
    }

    /**
     * Computes a route using SUMO's routing algorithm.
     */
    private void computeRoute() {
        if (startEdge == null || endEdge == null)
            return;

        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null) {
                showError("SUMO not connected!"); // This might happened if trying to add vehicle before starting
                                                  // simulation
                return;
            }

            updateStatusLabel("Computing route...", UIStyles.TEXT_WARNING);

            // Use SUMO to find valid route
            List<String> route = adapter.findRoute(startEdge, endEdge);

            if (route != null && !route.isEmpty()) {
                selectedRoute.clear();
                selectedRoute.addAll(route);

                // Update UI
                routeListView.getItems().clear();
                routeListView.getItems().addAll(route);

                statusLabel.setText("✓ Route found: " + route.size() + " edges");
                statusLabel.setStyle(
                        "-fx-text-fill: " + UIStyles.TEXT_SUCCESS + "; -fx-font-weight: bold; -fx-font-size: 11;");
                updateInstructionLabel("Route ready! Click 'Add Vehicle' or change edges.", UIStyles.TEXT_SUCCESS);
            } else {
                selectedRoute.clear();
                routeListView.getItems().clear();
                showError("No valid route between these edges!");
            }

        } catch (Exception e) {
            showError("Route computation failed: " + e.getMessage()); // Only happen if SUMO connection issue or invalid
                                                                      // edges
            e.printStackTrace();
        }
    }

    /**
     * An exaplaination of how the program's FPS works:
     * 1 Frame = 1 complete redraw of the canvas (not 1 step of the simulation)
     * Basically the FPS here is how many times per second the canvas is redrawn
     * The JavaFX UI rendering usually runs at 60FPS on thread 1
     * The SUMO Simulation runs at a fixed tick rate on thread 2
     * The stress test adds vehicles continuously will applying load on both threads
     * 1. More vehicles = more rendering required per frame
     * 5000 vehicle means 5000 drawings per frame
     * Making the draw() method take longer to complete 1 total redraw
     * Lower FPS as the draw() takes longer
     * 2. More vehicles = more processing per simulation step
     * Hard to notice unless extreme stress test
     * Making each simulation step take longer to complete
     * The simulation thread cannot complete a tick in desired time.
     * The simulation lags behind real time.
     * 3. Desynchronization between rendering and simulation:
     * + Simulation faster than render
     * Rarely happens, require high vehicle count and low interval
     * The vehicles teleport of skip positions between frames
     * + Render faster than simulation
     * Common when stress testing
     * The redraws show same vehicle positions multiple times
     * The FPS counter shows high FPS but simulation lags behind real time
     */

    /**
     * Toggles the stress test on or off.
     */
    private void toggleStressTest() {
        if (stressTestRunning) {
            stopStressTest();
        } else {
            startStressTest();
        }
    }

    /**
     * Starts the continuous stress test with random vehicle injection.
     */
    private void startStressTest() {
        TraaSAdapter adapter = runner.getAdapter();
        if (adapter == null) {
            updateStressStatus("SUMO not connected! Start simulation first.", UIStyles.TEXT_ERROR);
            return;
        }

        // Get interval
        String intervalText = stressIntervalField.getText().trim();
        if (intervalText.isEmpty())
            intervalText = "500";
        int interval;
        try {
            interval = Integer.parseInt(intervalText);
        } catch (NumberFormatException e) {
            updateStressStatus("Invalid interval!", UIStyles.TEXT_ERROR);
            return;
        }

        // Get batch size
        String batchText = stressBatchField.getText().trim();
        if (batchText.isEmpty()) batchText = "1";
        int batch;
        try {
            batch = Integer.parseInt(batchText);
        } catch (NumberFormatException e) {
            updateStressStatus("Invalid batch size!", UIStyles.TEXT_ERROR);
            return;
        }
        if (batch < 1) {
            updateStressStatus("Batch size must be >= 1", UIStyles.TEXT_ERROR);
            return;
        }
        if (batch > 1000) {
            updateStressStatus("Batch size too large (max 1000)", UIStyles.TEXT_ERROR);
            return;
        }

        // Load available edges for random routing
        try {
            availableEdges = adapter.getEdgeIds();
            // Filter out internal edges (those starting with ':')
            availableEdges = availableEdges.stream()
                    .filter(edge -> !edge.startsWith(":"))
                    .collect(java.util.stream.Collectors.toList());

            if (availableEdges.size() < 2) {
                updateStressStatus("Not enough edges for routing!", UIStyles.TEXT_ERROR);
                return;
            }
        } catch (Exception e) {
            updateStressStatus("Failed to get edges: " + e.getMessage(), UIStyles.TEXT_ERROR);
            return;
        }

        // Resume simulation if paused
        if (runner.isPaused()) {
            runner.resume();
        }

        // Initialize stress test state
        stressTestRunning = true;
        stressVehiclesAdded = 0;
        stressVehiclesFailed = 0;
        stressTestStartTime = System.currentTimeMillis();

        // Update UI
        stressTestBtn.setText("Stop Stress Test");
        stressTestBtn.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10;");
        stressTestBtn.setOnMouseEntered(e -> stressTestBtn.setStyle(
                "-fx-background-color: #66BB6A; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10;"));
        stressTestBtn.setOnMouseExited(e -> stressTestBtn.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10;"));
        updateStressStatus("Stress test running...", UIStyles.TEXT_SUCCESS);

        // Disable other controls during stress test
        confirmBtn.setDisable(true);
        addEdgeBtn.setDisable(true);
        stressIntervalField.setDisable(true);
        stressBatchField.setDisable(true);

        // Start FPS counter
        startFpsCounter();

        // Start timer for continuous injection
        stressTestTimer = new Timer("StressTestTimer", true);
        stressTestTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (stressTestRunning) {
                    for (int i = 0; i < batch && stressTestRunning; i++) {
                        injectRandomVehicle();
                    }
                }
            }
        }, 0, interval);

        logger.info("Stress test started with interval: " + interval + "ms");
    }

    /**
     * Stops the stress test and displays statistics.
     */
    private void stopStressTest() {
        if (!stressTestRunning)
            return;

        stressTestRunning = false;

        // Stop timer
        if (stressTestTimer != null) {
            stressTestTimer.cancel();
            stressTestTimer = null;
        }

        // Stop FPS counter
        stopFpsCounter();

        // Calculate statistics
        long duration = System.currentTimeMillis() - stressTestStartTime;
        double durationSec = duration / 1000.0;
        double rate = durationSec > 0 ? stressVehiclesAdded / durationSec : 0;

        // Update UI on JavaFX thread
        Platform.runLater(() -> {
            stressTestBtn.setText("Start Stress Test");
            stressTestBtn.setStyle(
                    "-fx-background-color: #E53935; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10;");
            stressTestBtn.setOnMouseEntered(e -> stressTestBtn.setStyle(
                    "-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10;"));
            stressTestBtn.setOnMouseExited(e -> stressTestBtn.setStyle(
                    "-fx-background-color: #E53935; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10;"));

            updateStressStatus("Stress test stopped", UIStyles.TEXT_WARNING);
            stressStatsLabel.setText(String.format(
                    "Results: %d added, %d failed\nDuration: %.1fs | Rate: %.1f veh/s",
                    stressVehiclesAdded, stressVehiclesFailed, durationSec, rate));
            stressStatsLabel.setStyle(
                    "-fx-text-fill: " + UIStyles.TEXT_SUCCESS + "; -fx-font-size: 11; -fx-font-family: monospace;");

            // Re-enable controls
            confirmBtn.setDisable(false);
            addEdgeBtn.setDisable(false);
            stressIntervalField.setDisable(false);
            stressBatchField.setDisable(false);
        });

        System.out
                .println(String.format("Stress test stopped. Added: %d, Failed: %d, Duration: %.1fs, Rate: %.1f veh/s",
                        stressVehiclesAdded, stressVehiclesFailed, durationSec, rate));
    }

    /**
     * Injects a single vehicle with a random route.
     */
    private void injectRandomVehicle() {
        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null || availableEdges == null || availableEdges.size() < 2) {
                Platform.runLater(() -> stopStressTest());
                return;
            }

            // Select random start and end edges
            String fromEdge = availableEdges.get(random.nextInt(availableEdges.size()));
            String toEdge;
            do {
                toEdge = availableEdges.get(random.nextInt(availableEdges.size()));
            } while (toEdge.equals(fromEdge));

            // Find route between edges
            List<String> route = adapter.findRoute(fromEdge, toEdge);
            if (route == null || route.isEmpty()) {
                stressVehiclesFailed++;
                updateStressStats();
                return;
            }

            // Generate unique vehicle ID
            String vehicleType = VEHICLE_TYPES[random.nextInt(VEHICLE_TYPES.length)];
            String vehicleClass = VEHICLE_CLASSES[java.util.Arrays.asList(VEHICLE_TYPES).indexOf(vehicleType)];
            String vehicleId = "stress_" + vehicleType + "_" + System.currentTimeMillis() + "_" + random.nextInt(10000);
            String routeId = "route_" + vehicleId;

            // Add route and vehicle
            adapter.addRoute(routeId, route);
            adapter.addVehicle(vehicleId, routeId, vehicleClass);

            stressVehiclesAdded++;
            updateStressStats();

        } catch (Exception e) {
            stressVehiclesFailed++;
            updateStressStats();
            // Don't spam console with errors
            if (stressVehiclesFailed <= 5) {
                System.err.println("Stress test injection error: " + e.getMessage());
            }
        }
    }

    /**
     * Updates the stress test statistics display.
     */
    private void updateStressStats() {
        if (!stressTestRunning)
            return;

        long elapsed = System.currentTimeMillis() - stressTestStartTime;
        double elapsedSec = elapsed / 1000.0;
        double rate = elapsedSec > 0 ? stressVehiclesAdded / elapsedSec : 0;

        Platform.runLater(() -> {
            stressStatsLabel.setText(String.format(
                    "Added: %d | Failed: %d | Rate: %.1f veh/s",
                    stressVehiclesAdded, stressVehiclesFailed, rate));
        });
    }

    /**
     * Updates the stress test status label.
     */
    private void updateStressStatus(String text, String color) {
        Platform.runLater(() -> {
            stressStatusLabel.setText(text);
            stressStatusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11;");
        });
    }

    /**
     * Starts the FPS counter using AnimationTimer.
     */
    private void startFpsCounter() {
        frameTimeIndex = 0;
        frameTimesArrayFull = false;

        fpsTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long oldFrameTime = frameTimes[frameTimeIndex];
                frameTimes[frameTimeIndex] = now;
                frameTimeIndex = (frameTimeIndex + 1) % frameTimes.length;

                if (frameTimeIndex == 0) {
                    frameTimesArrayFull = true;
                }

                if (frameTimesArrayFull) {
                    long elapsedNanos = now - oldFrameTime;
                    long elapsedNanosPerFrame = elapsedNanos / frameTimes.length;
                    double fps = 1_000_000_000.0 / elapsedNanosPerFrame;

                    // Color code FPS: green > 30, yellow 15-30, red < 15
                    String color;
                    if (fps >= 30) {
                        color = "#00E676"; // Green
                    } else if (fps >= 15) {
                        color = "#FFA726"; // Orange
                    } else {
                        color = "#EF5350"; // Red
                    }

                    fpsLabel.setText(String.format("FPS: %.1f", fps));
                    fpsLabel.setStyle("-fx-text-fill: " + color
                            + "; -fx-font-size: 14; -fx-font-weight: bold; -fx-font-family: monospace;");
                }
            }
        };
        fpsTimer.start();
        fpsLabel.setText("FPS: measuring...");
    }

    /**
     * Stops the FPS counter.
     */
    private void stopFpsCounter() {
        if (fpsTimer != null) {
            fpsTimer.stop();
            fpsTimer = null;
        }
        // Keep the last FPS value visible
    }

    /**
     * Clears the current route selection.
     */
    private void clearRoute() {
        startEdge = null;
        endEdge = null;
        selectedRoute.clear();
        userSelectedEdges.clear();
        routeListView.getItems().clear();

        updateEdgeLabelStyle(startEdgeLabel, "Start: (not selected)", UIStyles.TEXT_SECONDARY, false);
        updateEdgeLabelStyle(endEdgeLabel, "End: (not selected)", UIStyles.TEXT_SECONDARY, false);
        updateStatusLabel("Route cleared", UIStyles.TEXT_SECONDARY);
        updateInstructionLabel("Click 'Select Route' then pick START and END edges", UIStyles.TEXT_WARNING);
    }

    /**
     * Adds the configured vehicle to the SUMO simulation.
     */
    private void addVehicle() {
        // Validation
        if (selectedRoute.size() < 2) {
            showError("Route must have at least 2 edges!");
            return;
        }

        String baseVehicleId = vehicleIdField.getText().trim();
        if (baseVehicleId.isEmpty()) {
            showError("Vehicle ID cannot be empty!");
            return;
        }

        int batchCount = 1;
        String batchText = vehicleBatchField.getText().trim();
        if (!batchText.isEmpty()) {
            try {
                batchCount = Integer.parseInt(batchText);
            } catch (NumberFormatException e) {
                showError("Invalid batch count!");
                return;
            }
        }
        if (batchCount < 1) {
            showError("Batch count must be >= 1");
            return;
        }
        if (batchCount > 1000) {
            showError("Batch count too large (max 1000)");
            return;
        }

        double departSpeed = 0.0;
        String speedText = vehicleSpeedField.getText().trim();
        if (!speedText.isEmpty()) {
            try {
                departSpeed = Double.parseDouble(speedText);
            } catch (NumberFormatException e) {
                showError("Invalid speed value!");
                return;
            }
        }
        if (!Double.isFinite(departSpeed) || departSpeed < 0.0) {
            showError("Speed must be a non-negative number");
            return;
        }

        Color overrideColor = null;
        try {
            overrideColor = parseHexColor(vehicleColorField.getText());
        } catch (Exception e) {
            showError(e.getMessage());
            return;
        }

        String vehicleType = vehicleTypeCombo.getValue();
        int typeIndex = java.util.Arrays.asList(VEHICLE_TYPES).indexOf(vehicleType);
        String vehicleClass = VEHICLE_CLASSES[typeIndex];

        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null) {
                showError("SUMO not connected!");
                return;
            }

            for (int i = 0; i < batchCount; i++) {
                String vehicleId = (i == 0) ? baseVehicleId : (baseVehicleId + "_" + (i + 1));
                String routeId = "route_" + vehicleId;
                adapter.addRoute(routeId, selectedRoute);
                adapter.addVehicle(vehicleId, routeId, vehicleClass, departSpeed);

                if (overrideColor != null) {
                    Vehicle.setColorOverride(vehicleId, overrideColor);
                }
            }

            statusLabel.setText(batchCount == 1
                    ? ("✓ Vehicle '" + baseVehicleId + "' added successfully!")
                    : ("✓ Added " + batchCount + " vehicles starting with '" + baseVehicleId + "'"));
            statusLabel.setStyle("-fx-text-fill: " + UIStyles.TEXT_SUCCESS + "; -fx-font-weight: bold; -fx-font-size: 11;");

            // Increment counter for next vehicle
            vehicleCounter += batchCount;

            // Resume simulation
            if (runner != null && runner.isPaused()) {
                runner.resume();
            }

            // Clear and reset for next vehicle
            clearRoute();
            updateVehicleId();

            // Notify that vehicle was added
            if (onVehicleAdded != null)
                onVehicleAdded.run();

            // System.out.println("Added vehicle: " + vehicleId + " with route: " +
            // selectedRoute);

        } catch (Exception e) {
            showError("Failed to add vehicle: " + e.getMessage()); // This might happne if having conflicting vehicle
                                                                   // IDs or route selection issues
            e.printStackTrace();
        }
    }

    /**
     * Displays an error message in the status label.
     * 
     * @param message The error message to display
     */
    private void showError(String message) {
        updateStatusLabel("X " + message, UIStyles.TEXT_ERROR);
    }

    /**
     * Updates an edge label with consistent styling.
     * 
     * @param label The label to update
     * @param text  The new text
     * @param color The text color
     * @param bold  Whether to apply bold styling
     */
    private void updateEdgeLabelStyle(Label label, String text, String color, boolean bold) {
        label.setText(text);
        String style = "-fx-text-fill: " + color + "; -fx-font-size: 11;";
        if (bold)
            style += " -fx-font-weight: bold;";
        label.setStyle(style);
    }

    /**
     * Updates the status label with colored text.
     * 
     * @param text  The status message
     * @param color The text color
     */
    private void updateStatusLabel(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11;");
    }

    /**
     * Updates the instruction label with colored text.
     * 
     * @param text  The instruction message
     * @param color The text color
     */
    private void updateInstructionLabel(String text, String color) {
        instructionLabel.setText(text);
        instructionLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11;");
    }

    /**
     * Checks if the panel is currently in route selection mode.
     * 
     * @return true if route selection is active
     */
    public boolean isRouteSelectionMode() {
        return routeSelectionMode;
    }

    /**
     * Returns a copy of the currently selected route.
     * 
     * @return List of edge IDs in the route
     */
    public List<String> getSelectedRoute() {
        return new ArrayList<>(selectedRoute);
    }

    /**
     * Returns the start edge ID.
     * 
     * @return The start edge ID, or null if not selected
     */
    public String getStartEdge() {
        return startEdge;
    }

    /**
     * Returns the end edge ID.
     * 
     * @return The end edge ID, or null if not selected
     */
    public String getEndEdge() {
        return endEdge;
    }
}
