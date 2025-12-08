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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel for adding new vehicles to the simulation.
 * Allows selection of vehicle type and route building by selecting start/end edges.
 * SUMO automatically finds a valid path between the selected edges.
 */
public class VehicleAddPanel extends VBox {

    // Vehicle type options
    private static final String[] VEHICLE_TYPES = { "car", "truck", "bus", "motorcycle", "emergency" };
    private static final String[] VEHICLE_CLASSES = { "passenger", "truck", "bus", "motorcycle", "emergency" };

    // UI components
    private ComboBox<String> vehicleTypeCombo;
    private TextField vehicleIdField;
    private ListView<String> routeListView;
    private Label statusLabel;
    private Label instructionLabel;
    private Label startEdgeLabel;
    private Label endEdgeLabel;
    private Button addEdgeBtn;
    private Button clearRouteBtn;
    private Button confirmBtn;
    private Button cancelBtn;

    // Route data
    private List<String> selectedRoute;
    private List<String> userSelectedEdges;
    private String startEdge = null;
    private String endEdge = null;

    // Dependencies
    private SimulationRunner runner;
    private TrafficManager trafficManager; // Do not delete, even if marked as unused, it's used in callbacks
    private Runnable onCancel;
    private Runnable onStartRouteSelection;
    private Consumer<Boolean> onRouteSelectionModeChange;
    private Runnable onVehicleAdded;

    // State
    private boolean routeSelectionMode = false;
    private static int vehicleCounter = 1;

    public VehicleAddPanel(SimulationRunner runner, TrafficManager trafficManager,
            Runnable onCancel, Runnable onStartRouteSelection,
            Consumer<Boolean> onRouteSelectionModeChange, Runnable onVehicleAdded) {
        super(12);
        this.runner = runner;
        this.trafficManager = trafficManager;
        this.onCancel = onCancel;
        this.onStartRouteSelection = onStartRouteSelection;
        this.onRouteSelectionModeChange = onRouteSelectionModeChange;
        this.onVehicleAdded = onVehicleAdded;
        this.selectedRoute = new ArrayList<>();
        this.userSelectedEdges = new ArrayList<>();

        createUI();
    }

    private void createUI() {
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(15));
        setStyle("-fx-background-color: #0D1B2A;");
        setSpacing(10);

        // Back button
        Button backBtn = createButton("← Back");
        backBtn.setOnAction(e -> {
            exitRouteSelectionMode();
            onCancel.run();
        });

        // Title
        Label titleLabel = new Label("ADD NEW VEHICLE");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16;");

        // Vehicle Type Selection
        Label typeLabel = new Label("Vehicle Type:");
        typeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        vehicleTypeCombo = new ComboBox<>();
        vehicleTypeCombo.getItems().addAll(VEHICLE_TYPES);
        vehicleTypeCombo.setValue(VEHICLE_TYPES[0]);
        vehicleTypeCombo.setPrefWidth(250);
        vehicleTypeCombo.setStyle("-fx-background-color: #1B263B; -fx-text-fill: white;");
        vehicleTypeCombo.setOnAction(e -> updateVehicleId());

        // Vehicle ID
        Label idLabel = new Label("Vehicle ID (auto-generated):");
        idLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        vehicleIdField = new TextField();
        vehicleIdField.setPrefWidth(250);
        vehicleIdField.setStyle("-fx-background-color: #1B263B; -fx-text-fill: white; -fx-font-family: monospace;");
        updateVehicleId();

        // Route Section
        Label routeLabel = new Label("Route Selection:");
        routeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");

        instructionLabel = new Label("Click 'Select Route' then pick START and END edges");
        instructionLabel.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 11;");
        instructionLabel.setWrapText(true);

        // Start/End edge labels
        startEdgeLabel = new Label("Start: (not selected)");
        startEdgeLabel.setStyle("-fx-text-fill: #778DA9; -fx-font-size: 11;");

        endEdgeLabel = new Label("End: (not selected)");
        endEdgeLabel.setStyle("-fx-text-fill: #778DA9; -fx-font-size: 11;");

        // Route list (shows computed path)
        Label computedRouteLabel = new Label("Computed Route:");
        computedRouteLabel.setStyle("-fx-text-fill: #778DA9; -fx-font-size: 11;");

        routeListView = new ListView<>();
        routeListView.setPrefHeight(100);
        routeListView.setStyle("-fx-background-color: #1B263B; -fx-control-inner-background: #1B263B;");

        // Route action buttons
        HBox routeButtons = new HBox(8);
        routeButtons.setAlignment(Pos.CENTER);

        addEdgeBtn = createButton("Select Route");
        addEdgeBtn.setPrefWidth(120);
        addEdgeBtn.setStyle("-fx-background-color: #415A77; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 8;");
        addEdgeBtn.setOnAction(e -> toggleRouteSelectionMode());

        clearRouteBtn = createButton("🗑 Clear");
        clearRouteBtn.setPrefWidth(120);
        clearRouteBtn.setStyle("-fx-background-color: #1B263B; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 8;");
        clearRouteBtn.setOnAction(e -> clearRoute());

        routeButtons.getChildren().addAll(addEdgeBtn, clearRouteBtn);

        // Status label
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11;");
        statusLabel.setWrapText(true);

        // Confirm/Cancel buttons
        confirmBtn = createButton("✓ Add Vehicle");
        confirmBtn.setStyle("-fx-background-color: #415A77; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;");
        confirmBtn.setOnAction(e -> addVehicle());

        cancelBtn = createButton("✕ Cancel");
        cancelBtn.setStyle("-fx-background-color: #1B263B; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;");
        cancelBtn.setOnAction(e -> {
            exitRouteSelectionMode();
            onCancel.run();
        });

        // Info box with instructions
        VBox infoBox = createInfoBox();

        // Add all components
        getChildren().addAll(
                backBtn, titleLabel, new Separator(),
                typeLabel, vehicleTypeCombo,
                idLabel, vehicleIdField, new Separator(),
                routeLabel, instructionLabel,
                startEdgeLabel, endEdgeLabel,
                computedRouteLabel, routeListView,
                routeButtons, statusLabel, new Separator(),
                confirmBtn, cancelBtn, infoBox);
    }

    private VBox createInfoBox() {
        VBox infoBox = new VBox(5);
        infoBox.setStyle("-fx-background-color: #1B263B; -fx-padding: 10; -fx-background-radius: 5;");

        Label infoLabel = new Label("ℹ How to add a vehicle:");
        infoLabel.setStyle("-fx-text-fill: #778DA9; -fx-font-weight: bold; -fx-font-size: 11;");

        String stepStyle = "-fx-text-fill: #778DA9; -fx-font-size: 10;";
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

    // Update vehicle ID field based on selected type and counter
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

    private void toggleRouteSelectionMode() {
        routeSelectionMode = !routeSelectionMode;

        if (routeSelectionMode) {
            enterRouteSelectionMode();
        } else {
            exitRouteSelectionModeUI();
        }
    }

    private void enterRouteSelectionMode() {
        // Pause simulation when entering route selection mode
        if (runner != null && !runner.isPaused()) {
            runner.pause();
            statusLabel.setText("Simulation paused for route selection");
            statusLabel.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 11;");
        }

        addEdgeBtn.setText("✓ Done Selecting");
        addEdgeBtn.setStyle("-fx-background-color: #415A77; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 8;");

        if (startEdge == null) {
            instructionLabel.setText("Click on the START edge (where vehicle spawns)");
            instructionLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11;");
        } else if (endEdge == null) {
            instructionLabel.setText("Click on the END edge (destination)");
            instructionLabel.setStyle("-fx-text-fill: #EF5350; -fx-font-size: 11;");
        }

        if (onRouteSelectionModeChange != null) {
            onRouteSelectionModeChange.accept(true);
        }
        if (onStartRouteSelection != null) {
            onStartRouteSelection.run();
        }
    }

    private void exitRouteSelectionModeUI() {
        addEdgeBtn.setText("📍 Select Route");
        addEdgeBtn.setStyle("-fx-background-color: #415A77; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 8;");

        if (selectedRoute.isEmpty() && startEdge != null && endEdge != null) {
            instructionLabel.setText("No valid route found. Try different edges.");
            instructionLabel.setStyle("-fx-text-fill: #EF5350; -fx-font-size: 11;");
        } else if (!selectedRoute.isEmpty()) {
            instructionLabel.setText("Route computed: " + selectedRoute.size() + " edges");
            instructionLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11;");
        } else {
            instructionLabel.setText("Select start and end edges");
            instructionLabel.setStyle("-fx-text-fill: #778DA9; -fx-font-size: 11;");
        }

        if (onRouteSelectionModeChange != null) {
            onRouteSelectionModeChange.accept(false);
        }
    }

    private void exitRouteSelectionMode() {
        if (routeSelectionMode) {
            routeSelectionMode = false;
            if (onRouteSelectionModeChange != null) {
                onRouteSelectionModeChange.accept(false);
            }
        }
    }

    // Called when user clicks on an edge during route selection
    public void addEdgeToRoute(String edgeId) {
        if (!routeSelectionMode)
            return;

        // First click = start edge
        if (startEdge == null) {
            startEdge = edgeId;
            userSelectedEdges.clear();
            userSelectedEdges.add(edgeId);
            startEdgeLabel.setText("Start: " + edgeId);
            startEdgeLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11; -fx-font-weight: bold;");
            statusLabel.setText("Start edge selected. Now click END edge.");
            statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11;");
            instructionLabel.setText("Click on the END edge (destination)");
            instructionLabel.setStyle("-fx-text-fill: #EF5350; -fx-font-size: 11;");
            return;
        }

        // Second click = end edge (compute route)
        if (endEdge == null && !edgeId.equals(startEdge)) {
            endEdge = edgeId;
            userSelectedEdges.add(edgeId);
            endEdgeLabel.setText("End: " + edgeId);
            endEdgeLabel.setStyle("-fx-text-fill: #EF5350; -fx-font-size: 11; -fx-font-weight: bold;");
            computeRoute();
            return;
        }

        // If clicking again, allow changing the end edge
        if (endEdge != null && !edgeId.equals(startEdge)) {
            endEdge = edgeId;
            userSelectedEdges.set(1, edgeId);
            endEdgeLabel.setText("End: " + edgeId);
            endEdgeLabel.setStyle("-fx-text-fill: #EF5350; -fx-font-size: 11; -fx-font-weight: bold;");
            computeRoute();
        }
    }

    // Compute route using SUMO between selected start and end edges
    private void computeRoute() {
        if (startEdge == null || endEdge == null)
            return;

        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null) {
                showError("SUMO not connected!"); //This might happened if trying to add vehicle before starting simulation
                return;
            }

            statusLabel.setText("Computing route...");
            statusLabel.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 11;");

            // Use SUMO to find valid route
            List<String> route = adapter.findRoute(startEdge, endEdge);

            if (route != null && !route.isEmpty()) {
                selectedRoute.clear();
                selectedRoute.addAll(route);

                // Update UI
                routeListView.getItems().clear();
                routeListView.getItems().addAll(route);

                statusLabel.setText("✓ Route found: " + route.size() + " edges");
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 11;");
                instructionLabel.setText("Route ready! Click 'Add Vehicle' or change edges.");
                instructionLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11;");
            } else {
                selectedRoute.clear();
                routeListView.getItems().clear();
                showError("No valid route between these edges!");
            }

        } catch (Exception e) {
            showError("Route computation failed: " + e.getMessage()); // Only happen if SUMO connection issue or invalid edges
            e.printStackTrace();
        }
    }

    private void clearRoute() {
        startEdge = null;
        endEdge = null;
        selectedRoute.clear();
        userSelectedEdges.clear();
        routeListView.getItems().clear();

        startEdgeLabel.setText("Start: (not selected)");
        startEdgeLabel.setStyle("-fx-text-fill: #778DA9; -fx-font-size: 11;");
        endEdgeLabel.setText("End: (not selected)");
        endEdgeLabel.setStyle("-fx-text-fill: #778DA9; -fx-font-size: 11;");
        statusLabel.setText("Route cleared");
        statusLabel.setStyle("-fx-text-fill: #778DA9; -fx-font-size: 11;");
        instructionLabel.setText("Click 'Select Route' then pick START and END edges");
        instructionLabel.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 11;");
    }

    private void addVehicle() {
        // Validation
        if (selectedRoute.size() < 2) {
            showError("Route must have at least 2 edges!");
            return;
        }

        String vehicleId = vehicleIdField.getText().trim();
        if (vehicleId.isEmpty()) {
            showError("Vehicle ID cannot be empty!");
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

            // Create route ID and add to SUMO
            String routeId = "route_" + vehicleId;
            adapter.addRoute(routeId, selectedRoute);
            adapter.addVehicle(vehicleId, routeId, vehicleClass);

            statusLabel.setText("✓ Vehicle '" + vehicleId + "' added successfully!");
            statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 11;");

            // Increment counter for next vehicle
            vehicleCounter++;

            // Resume simulation
            if (runner != null && runner.isPaused()) {
                runner.resume();
            }

            // Clear and reset for next vehicle
            clearRoute();
            updateVehicleId();

            // Notify that vehicle was added
            if (onVehicleAdded != null) {
                onVehicleAdded.run();
            }

            System.out.println("Added vehicle: " + vehicleId + " with route: " + selectedRoute);

        } catch (Exception e) {
            showError("Failed to add vehicle: " + e.getMessage()); // This might happne if having conflicting vehicle IDs or route selection issues
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        statusLabel.setText("❌ " + message);
        statusLabel.setStyle("-fx-text-fill: #EF5350; -fx-font-size: 11;");
    }

    private Button createButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(250);

        String buttonStyle = "-fx-background-color: #1B263B; -fx-text-fill: white; " +
                "-fx-font-size: 12; -fx-padding: 8;";
        String buttonHoverStyle = buttonStyle + "-fx-background-color: #415A77;";

        btn.setStyle(buttonStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(buttonHoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(buttonStyle));

        return btn;
    }

    // Getters
    public boolean isRouteSelectionMode() {
        return routeSelectionMode;
    }

    public List<String> getSelectedRoute() {
        return new ArrayList<>(selectedRoute);
    }

    public String getStartEdge() {
        return startEdge;
    }

    public String getEndEdge() {
        return endEdge;
    }
}
