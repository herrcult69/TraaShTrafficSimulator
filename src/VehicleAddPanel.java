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

/**
 * Panel for adding new vehicles to the simulation interactively.
 * Allows selection of vehicle type and route building by clicking start and end edges on the map.
 * SUMO automatically computes a valid path between the selected edges using its routing algorithm.
 * Supports auto-generated vehicle IDs and provides visual feedback during route selection.
 *
 * @author M A T^2 H Team
 * @version 2.0 
 * @see SimulationRunner
 * @see TraaSAdapter
 * @see Edge
 * @see ControlPanel
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
    private Runnable onCancel;
    private Runnable onStartRouteSelection;
    private java.util.function.Consumer<Boolean> onRouteSelectionModeChange;
    private Runnable onVehicleAdded;

    // State
    private boolean routeSelectionMode = false;
    private static int vehicleCounter = 1;

    /**
     * Constructs a new vehicle addition panel with necessary callbacks.
     * 
     * @param runner The simulation runner for SUMO communication
     * @param onCancel Callback invoked when user cancels vehicle addition
     * @param onStartRouteSelection Callback invoked when route selection starts
     * @param onRouteSelectionModeChange Callback for route selection mode changes (true = active)
     * @param onVehicleAdded Callback invoked when vehicle is successfully added
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
     * Includes vehicle type selector, ID field, route selection controls, and action buttons.
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
        routeListView.setStyle("-fx-background-color: " + UIStyles.BG_SECONDARY + "; -fx-control-inner-background: " + UIStyles.BG_SECONDARY + ";");

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

    /**
     * Creates an informational box with step-by-step instructions for adding a vehicle.
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
     * Updates the vehicle ID field based on the selected vehicle type and counter.
     * Generates IDs like "car_new_1", "truck_new_2", etc.
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
     * Enters route selection mode, pauses simulation, and updates UI for edge selection.
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

        if (onRouteSelectionModeChange != null) onRouteSelectionModeChange.accept(true);
        if (onStartRouteSelection != null) onStartRouteSelection.run();
    }

    /**
     * Exits route selection mode and updates UI to show route status or instructions.
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

        if (onRouteSelectionModeChange != null) onRouteSelectionModeChange.accept(false);
    }

    /**
     * Cleanly exits route selection mode by disabling it and notifying callbacks.
     */
    private void exitRouteSelectionMode() {
        if (routeSelectionMode) {
            routeSelectionMode = false;
            if (onRouteSelectionModeChange != null) onRouteSelectionModeChange.accept(false);
        }
    }

    /**
     * Adds an edge to the route during route selection mode.
     * First click sets start edge (green), second click sets end edge (red) and computes route.
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
     * Computes a route using SUMO's routing algorithm between the selected start and end edges.
     * Displays the computed route in the route list view.
     */
    private void computeRoute() {
        if (startEdge == null || endEdge == null)
            return;

        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null) {
                showError("SUMO not connected!"); //This might happened if trying to add vehicle before starting simulation
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
                statusLabel.setStyle("-fx-text-fill: " + UIStyles.TEXT_SUCCESS + "; -fx-font-weight: bold; -fx-font-size: 11;");
                updateInstructionLabel("Route ready! Click 'Add Vehicle' or change edges.", UIStyles.TEXT_SUCCESS);
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

    /**
     * Clears the current route selection, resetting start/end edges and route list.
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
     * Validates inputs, creates route in SUMO, adds vehicle, and resets the form.
     */
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
            statusLabel.setStyle("-fx-text-fill: " + UIStyles.TEXT_SUCCESS + "; -fx-font-weight: bold; -fx-font-size: 11;");

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
            if (onVehicleAdded != null) onVehicleAdded.run();

            System.out.println("Added vehicle: " + vehicleId + " with route: " + selectedRoute);

        } catch (Exception e) {
            showError("Failed to add vehicle: " + e.getMessage()); // This might happne if having conflicting vehicle IDs or route selection issues
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
     * @param text The new text
     * @param color The text color
     * @param bold Whether to apply bold styling
     */
    private void updateEdgeLabelStyle(Label label, String text, String color, boolean bold) {
        label.setText(text);
        String style = "-fx-text-fill: " + color + "; -fx-font-size: 11;";
        if (bold) style += " -fx-font-weight: bold;";
        label.setStyle(style);
    }

    /**
     * Updates the status label with colored text.
     * 
     * @param text The status message
     * @param color The text color
     */
    private void updateStatusLabel(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11;");
    }

    /**
     * Updates the instruction label with colored text.
     * 
     * @param text The instruction message
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
