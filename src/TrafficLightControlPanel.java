import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.util.converter.DoubleStringConverter;
import java.util.List;
import java.util.ArrayList;

/**
 * Control panel for managing a selected traffic light manually.
 * Provides selective control over individual traffic light signals while maintaining junction synchronization.
 * Supports forcing green/red states for specific signals and returning to automatic SUMO control.
 * Manual mode is synchronized across all traffic lights at the same junction to prevent conflicts.
 *
 * @author M A T^2 H Team
 * @version 2.0 
 * @see TrafficLight
 * @see TrafficManager
 * @see SimulationRunner
 * @see ControlPanel
 */
public class TrafficLightControlPanel {
    private VBox panel;
    private TrafficLight selectedLight;
    private SimulationRunner runner;
    private TrafficManager trafficManager;
    private Runnable onBackPressed;

    // Track manual phase duration set by users
    private Double customPhaseDuration = null;
    private int customPhaseIndex = -1;

    // UI elements
    private Label statusLabel;
    private Button forceGreenBtn;
    private Button forceRedBtn;
    private Button autoBtn;
    private TextField currentStateField;
    
    // Phase display elements
    private Label currentPhaseLabel;
    private Label phaseDurationLabel;
    private Label remainingTimeLabel;

    // Phase timing editor
    private TextField phaseDurationField;
    private Button editTimingBtn;

    // Button styles - static final for efficiency and consistency
    private static final String GREEN_STYLE = "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String GREEN_HOVER = "-fx-background-color: #388E3C; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String GREEN_ACTIVE = "-fx-background-color: #1B5E20; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10; -fx-border-color: #4CAF50; -fx-border-width: 3;";

    private static final String RED_STYLE = "-fx-background-color: #C62828; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String RED_HOVER = "-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String RED_ACTIVE = "-fx-background-color: #B71C1C; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10; -fx-border-color: #EF5350; -fx-border-width: 3;";

    private static final String AUTO_STYLE = "-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String AUTO_HOVER = "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";

    /**
     * Constructs a traffic light control panel for the specified light.
     * 
     * @param light The traffic light to control
     * @param runner The simulation runner for SUMO communication
     * @param trafficManager The traffic manager for junction-wide mode synchronization
     * @param onBackPressed Callback invoked when back button is pressed
     */
    public TrafficLightControlPanel(TrafficLight light, SimulationRunner runner, TrafficManager trafficManager,
            Runnable onBackPressed) {
        this.selectedLight = light;
        this.runner = runner;
        this.trafficManager = trafficManager;
        this.onBackPressed = onBackPressed;
        createUI();
        
        // Update UI to reflect current mode state
        updateModeDisplay();
        updateButtonStates();
    }

    /**
     * Creates the control panel UI with traffic light info, state display, and control buttons.
     */
    private void createUI() {
        panel = new VBox(12);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: " + UIStyles.BG_PRIMARY + ";");

        // Back button
        Button backBtn = UIStyles.createStyledButton("← Back");
        backBtn.setOnAction(e -> onBackPressed.run());

        // Title
        Label titleLabel = new Label("TRAFFIC LIGHT CONTROL");
        titleLabel.setStyle(UIStyles.TITLE_STYLE);

        // Info section
        VBox infoBox = new VBox(8);
        infoBox.setStyle(UIStyles.INFO_BOX_STYLE);

        Label junctionLabel = new Label("Junction: " + selectedLight.getJunctionId());
        junctionLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12;");

        // Get signal count and link indices (sorted)
        int signalCount = selectedLight.getSignals().size();
        List<Integer> linkIndices = selectedLight.getLinkIndices();

        Label signalLabel = new Label("Signals: " + signalCount + " connections");
        signalLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12;");

        Label linksLabel = new Label("Controls Links: " + linkIndices.toString());
        linksLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12;");
        linksLabel.setWrapText(true);
        statusLabel = new Label("Mode: AUTO");
        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 14;");

        infoBox.getChildren().addAll(junctionLabel, signalLabel, linksLabel, statusLabel);

        // Current state display
        Label currentStateLabel = new Label("Current State:");
        currentStateLabel.setStyle(UIStyles.LABEL_STYLE + " -fx-font-weight: bold;");

        currentStateField = new TextField();
        currentStateField.setEditable(false);
        currentStateField.setStyle(UIStyles.MONOSPACE_FIELD_STYLE);

        // Update current state periodically
        new javafx.animation.AnimationTimer() {
            private int lastPhase = -1;
            private long lastUpdate = 0;
            private static final long UPDATE_INTERVAL = 500_000_000;    // 0.5 real-life seconds in nanoseconds

            @Override
            public void handle(long now) {
                updateStateDisplay();

                // update remaining time every 0.5 seconds
                if (now - lastUpdate >= UPDATE_INTERVAL) {
                    lastUpdate = now;
                    updateRemainingTime();
                }

                // check if phase has changed
                try{
                    TraaSAdapter adapter = runner.getAdapter();
                    if (adapter != null ) {
                        int currentPhase = adapter.getCurrentPhase(selectedLight.getJunctionId());
                        if (currentPhase != lastPhase) {
                            lastPhase = currentPhase;
                            updatePhaseDisplay();
                        }
                    }
                } catch (Exception e){
                    // ignore
                }
            }
        }.start();

        // Quick control buttons
        Label quickControlLabel = new Label("Selective Control:");
        quickControlLabel.setStyle(UIStyles.LABEL_STYLE + " -fx-font-weight: bold;");

        // Count traffic lights at this junction
        int junctionTLCount = (int) trafficManager.getTrafficLights().stream()
            .filter(tl -> tl.getJunctionId().equals(selectedLight.getJunctionId()))
            .count();

        Label infoText = new Label("⚠ Changes only this traffic light's signals (links " + linkIndices + ")\n" +
                                   "⚠ Puts entire junction (" + junctionTLCount + " traffic lights) in MANUAL mode");
        infoText.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 10;");
        infoText.setWrapText(true);

        forceGreenBtn = new Button("Force GREEN (This Light Only)");
        forceGreenBtn.setPrefWidth(250);
        forceGreenBtn.setStyle(GREEN_STYLE);
        forceGreenBtn.setOnMouseEntered(e -> {
            if (!isCurrentlyGreen())
                forceGreenBtn.setStyle(GREEN_HOVER);
        });
        forceGreenBtn.setOnMouseExited(e -> updateButtonStates());
        forceGreenBtn.setOnAction(e -> forceGreen());

        forceRedBtn = new Button("Force RED (This Light Only)");
        forceRedBtn.setPrefWidth(250);
        forceRedBtn.setStyle(RED_STYLE);
        forceRedBtn.setOnMouseEntered(e -> {
            if (!isCurrentlyRed())
                forceRedBtn.setStyle(RED_HOVER);
        });
        forceRedBtn.setOnMouseExited(e -> updateButtonStates());
        forceRedBtn.setOnAction(e -> forceRed());

        autoBtn = new Button("⟲ Return Junction to AUTO");
        autoBtn.setPrefWidth(250);
        autoBtn.setStyle(AUTO_STYLE);
        autoBtn.setOnMouseEntered(e -> autoBtn.setStyle(AUTO_HOVER));
        autoBtn.setOnMouseExited(e -> autoBtn.setStyle(AUTO_STYLE));
        autoBtn.setOnAction(e -> returnToAuto());
        autoBtn.setDisable(true);

        // Phase information section 
        Label phaseInfoTitle = new Label ("Phase Information:");
        phaseInfoTitle.setStyle(UIStyles.LABEL_STYLE + " -fx-font-weight: bold;");

        currentPhaseLabel = new Label ("Current Phase: -");
        currentPhaseLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12;");

        phaseDurationLabel = new Label ("Phase Duration: -");
        phaseDurationLabel.setStyle("-fx-text-fill: #778DA9; -fx-font-size: 12;");

        remainingTimeLabel = new Label ("Remaining Time: -");
        remainingTimeLabel.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 12;");

        // Update phase display when panel opens
        updatePhaseDisplay();

        // Add all elements
        panel.getChildren().addAll(
                backBtn,
                titleLabel,
                infoBox,
                new javafx.scene.control.Separator(),
                currentStateLabel,
                currentStateField,
                new javafx.scene.control.Separator(),
                phaseInfoTitle,
                currentPhaseLabel,
                phaseDurationLabel,
                remainingTimeLabel,
                new javafx.scene.control.Separator(),
                createPhaseTimingButton(),
                new javafx.scene.control.Separator(),
                quickControlLabel,
                infoText,
                forceGreenBtn,
                forceRedBtn,
                autoBtn
                );
    }

    /**
     * Updates the current state display field with the traffic light's state.
     * Highlights controlled links in square brackets for visual clarity.
     */
    private void updateStateDisplay() {
        String state = selectedLight.getCurrentState();
        if (state != null && !state.isEmpty()) {
            // Highlight this traffic light's controlled links
            List<Integer> linkIndices = selectedLight.getLinkIndices();
            StringBuilder display = new StringBuilder();
            for (int i = 0; i < state.length(); i++) {
                if (linkIndices.contains(i)) {
                    display.append("[").append(state.charAt(i)).append("]");
                } else {
                    display.append(state.charAt(i));
                }
            }
            currentStateField.setText(display.toString());

            // Tooltip
            StringBuilder tooltip = new StringBuilder("Full junction state: " + state + "\n\n");
            tooltip.append("This traffic light controls:\n");
            for (TrafficLight.Signal signal : selectedLight.getSignals()) {
                tooltip.append("  Link ").append(signal.linkIndex).append(": ");
                if (signal.linkIndex < state.length()) {
                    tooltip.append(state.charAt(signal.linkIndex));
                }
                tooltip.append(" (").append(signal.fromEdge).append(" → ").append(signal.toEdge).append(")\n");
            }
            currentStateField.setTooltip(new javafx.scene.control.Tooltip(tooltip.toString()));
        }
    }

    /**
     * Forces all signals of this traffic light to green state.
     */
    private void forceGreen() {
        forceSignalState('G', "GREEN");
    }

    /**
     * Forces all signals of this traffic light to red state.
     */
    private void forceRed() {
        forceSignalState('r', "RED");
    }

    /**
     * Forces all signals of this traffic light to a specific state.
     * Modifies only the controlled link indices while preserving other junction signals.
     * 
     * @param stateChar The character to set ('G' for green, 'r' for red)
     * @param stateName Human-readable name for logging
     */
    private void forceSignalState(char stateChar, String stateName) {
        String currentState = selectedLight.getCurrentState();
        if (currentState == null || currentState.isEmpty()) {
            showError("Cannot get current state from SUMO");
            return;
        }

        List<Integer> linkIndices = selectedLight.getLinkIndices();
        char[] state = currentState.toCharArray();
        
        for (int linkIndex : linkIndices) {
            if (linkIndex < state.length) {
                state[linkIndex] = stateChar;
            }
        }

        String newState = new String(state);
        System.out.println("=== SELECTIVE CONTROL: Force " + stateName + " ===");
        System.out.println("Traffic Light: " + selectedLight.getJunctionId());
        System.out.println("Controlled Links: " + linkIndices);
        System.out.println("Old State: " + currentState);
        System.out.println("New State: " + newState);
        
        applyState(newState);
        enterManualMode();
        updateButtonStates();
    }

    /**
     * Applies a new traffic light state to SUMO and enters manual mode.
     * 
     * @param newState The complete junction state string to apply
     */
    private void applyState(String newState) {
        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null) {
                showError("SUMO not connected");
                return;
            }

            // Apply the modified state to the junction
            adapter.setTrafficLightState(selectedLight.getJunctionId(), newState);
            
            // Mark this traffic light as in manual mode
            selectedLight.setManualMode(true);
            selectedLight.setState(newState);
            
            // Synchronize manual mode across ALL traffic lights at this junction
            setJunctionManualMode(selectedLight.getJunctionId(), true);

            System.out.println("Applied selective state change to junction " + 
                             selectedLight.getJunctionId() + " (manual mode synced across junction)");

        } catch (Exception e) {
            showError("Failed to set state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Returns the traffic light to automatic SUMO control.
     * Synchronizes auto mode across all traffic lights at the junction.
     */
    private void returnToAuto() {
        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null) {
                showError("SUMO not connected");
                return;
            }

            // Return SUMO to automatic control
            adapter.setTrafficLightProgram(selectedLight.getJunctionId(), "0");

            // Clear manual mode for this traffic light
            selectedLight.setManualMode(false);
            
            // Synchronize auto mode across ALL traffic lights at this junction
            setJunctionManualMode(selectedLight.getJunctionId(), false);

            statusLabel.setText("Mode: AUTO");
            statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 14;");

            forceGreenBtn.setDisable(false);
            forceRedBtn.setDisable(false);
            autoBtn.setDisable(true);

            updateButtonStates();

            System.out.println("Returned to AUTO mode for junction " + selectedLight.getJunctionId() + " (auto mode synced across junction)");

        } catch (Exception e) {
            showError("Failed to return to auto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates button visual states based on current traffic light signal states.
     * Highlights active state (green or red) with bordered style.
     */
    private void updateButtonStates() {
        if (isCurrentlyGreen()) {
            forceGreenBtn.setStyle(GREEN_ACTIVE);
            forceRedBtn.setStyle(RED_STYLE);
        } else if (isCurrentlyRed()) {
            forceGreenBtn.setStyle(GREEN_STYLE);
            forceRedBtn.setStyle(RED_ACTIVE);
        } else {
            forceGreenBtn.setStyle(GREEN_STYLE);
            forceRedBtn.setStyle(RED_STYLE);
        }
    }

    /**
     * Checks if all controlled signals are currently in green state.
     * 
     * @return true if all signals are green ('G' or 'g')
     */
    private boolean isCurrentlyGreen() {
        return isCurrentlyInState('G', 'g');
    }

    /**
     * Checks if all controlled signals are currently in red state.
     * 
     * @return true if all signals are red ('r' or 'R')
     */
    private boolean isCurrentlyRed() {
        return isCurrentlyInState('r', 'R');
    }

    /**
     * Checks if all traffic light signals match any of the given states.
     * 
     * @param validStates Valid state characters (case-sensitive)
     * @return true if all links match one of the valid states
     */
    private boolean isCurrentlyInState(char... validStates) {
        String state = selectedLight.getCurrentState();
        if (state == null || state.isEmpty()) return false;

        List<Integer> linkIndices = selectedLight.getLinkIndices();
        for (int linkIndex : linkIndices) {
            if (linkIndex < state.length()) {
                char c = state.charAt(linkIndex);
                boolean matches = false;
                for (char validState : validStates) {
                    if (c == validState) {
                        matches = true;
                        break;
                    }
                }
                if (!matches) return false;
            }
        }
        return true;
    }

    /**
     * Enters manual control mode, updating UI to show manual status.
     */
    private void enterManualMode() {
        statusLabel.setText("Mode: MANUAL");
        statusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-size: 14;");
        autoBtn.setDisable(false);
    }
    
    /**
     * Updates the UI to reflect the current mode state (manual or auto).
     * Called when opening the panel to show the remembered mode from previous interactions.
     */
    private void updateModeDisplay() {
        if (selectedLight.isManualMode()) {
            statusLabel.setText("Mode: MANUAL");
            statusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-size: 14;");
            autoBtn.setDisable(false);
        } else {
            statusLabel.setText("Mode: AUTO");
            statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 14;");
            autoBtn.setDisable(true);
        }
    }
    
    /**
     * Sets manual or auto mode for ALL traffic lights at the same junction.
     * This keeps mode synchronized across the junction while allowing individual signal control.
     * 
     * @param junctionId The junction ID
     * @param manualMode true for manual mode, false for auto mode
     */
    private void setJunctionManualMode(String junctionId, boolean manualMode) {
        List<TrafficLight> allLights = trafficManager.getTrafficLights();
        int count = 0;
        for (TrafficLight light : allLights) {
            if (light.getJunctionId().equals(junctionId)) {
                light.setManualMode(manualMode);
                count++;
            }
        }
        System.out.println("Synchronized " + (manualMode ? "MANUAL" : "AUTO") + 
                         " mode across " + count + " traffic lights at junction " + junctionId);
    }

    /**
     * Creates a button to open the phase timming editor
     * 
     * @return Button to edit phase timing
     */
    private Button createPhaseTimingButton(){
        editTimingBtn = new Button("Edit Phase Timing");
        editTimingBtn.setPrefWidth(250);
        editTimingBtn.setStyle("-fx-background-color: #7B1FA2; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;");
        editTimingBtn.setOnMouseEntered(e -> editTimingBtn.setStyle("-fx-background-color: #8E24AA; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;"));
        editTimingBtn.setOnMouseExited(e -> editTimingBtn.setStyle("-fx-background-color: #7B1FA2; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;"));
        editTimingBtn.setOnAction(e -> openPhaseTimingEditor());
        return editTimingBtn;
    }

    /**
     * Opens the phase timing editor 
     * Pauses simulation, allows adjusting current phase duration
     */
    private void openPhaseTimingEditor(){
        // Pause simulation
        boolean wasRunning = !runner.isPaused();
        if (wasRunning){
            runner.pause();
        }

        try{
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null) {
                showError("Sumo not connected");
                if (wasRunning)runner.resume();
                return;
            }

            String junctionId = selectedLight.getJunctionId();

            // Get current phase information
            int currentPhase = adapter.getCurrentPhase(junctionId);
            double currentDuration = adapter.getCurrentPhaseDuration(junctionId);

            // Create dialog
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Phase Timing Editor - " + junctionId);
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            VBox dialogContent = new VBox(15);
            dialogContent.setPadding(new Insets(20));
            dialogContent.setStyle("-fx-background-color: " + UIStyles.BG_PRIMARY + ";");

            // Title
            Label titleLabel = new Label("EDIT PHASE TIMING");
            titleLabel.setStyle(UIStyles.TITLE_STYLE);

            // Info
            Label infoLabel = new Label("Simulation is paused. Adjust the current phase duration.");
            infoLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11;");
            infoLabel.setWrapText(true);

            Label noteLabel = new Label("Note: This adjusts the duration of the current active phase.");
            noteLabel.setStyle("-fx-text-fill: #778DA9; -fx-font-size: 10;");
            noteLabel.setWrapText(true);

            // Current phase info box
            VBox phaseBox = new VBox(10);
            phaseBox.setStyle("-fx-background-color: #1e1e1e; -fx-padding: 20; -fx-background-radius: 5;");
            phaseBox.setAlignment(Pos.CENTER);

            Label currentPhaseTitle = new Label ("Current Phase: " + currentPhase);
            currentPhaseTitle.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14; -fx-font-weight: bold;");

            Label currentDurationLabel = new Label ("Current Duration: " + String.format("%.0f", currentDuration) + " seconds");
            currentDurationLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12;");

            Label newDurationLabel = new Label ("New Duration (seconds)");
            newDurationLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12; -fx-font-weight: bold;");

            TextField durationField = new TextField(String.format("%.0f", currentDuration));
            durationField.setPrefWidth(200);
            durationField.setMaxWidth(200);
            durationField.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #FFA726; " + "-fx-font-size: 14; -fx-font-weight: bold; -fx-alignment: center;");

            // Only allow numeric input
            durationField.setTextFormatter(new TextFormatter<> (new DoubleStringConverter(),
                currentDuration,
                change -> {
                    String newText = change.getControlNewText();
                    if (newText.matches("\\d*\\.?\\d*")) {
                        return change;
                    }
                    return null;
                }
            ));

            phaseDurationField = durationField;

            phaseBox.getChildren().addAll(currentPhaseTitle, currentDurationLabel, new javafx.scene.control.Separator(), newDurationLabel, durationField);

            // Buttons
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);
            
            Button applyBtn = new Button("✓ Apply & Resume");
            applyBtn.setPrefWidth(150);
            applyBtn.setStyle(AUTO_STYLE);
            applyBtn.setOnMouseEntered(e -> applyBtn.setStyle(AUTO_HOVER));
            applyBtn.setOnMouseExited(e -> applyBtn.setStyle(AUTO_STYLE));
            applyBtn.setOnAction(e -> {
                if (applyCurrentPhaseTiming(adapter, junctionId)){
                    dialog.close();
                    if(wasRunning) runner.resume();
                }
            });

            Button cancelBtn = new Button("x Cancel");
            cancelBtn.setPrefWidth(150);
            cancelBtn.setStyle(RED_STYLE);
            cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(RED_HOVER));
            cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(RED_STYLE));
            cancelBtn.setOnAction(e -> {
                    dialog.close();
                    if(wasRunning) runner.resume();
            });

            buttonBox.getChildren().addAll(applyBtn, cancelBtn);

            dialogContent.getChildren().addAll(titleLabel, infoLabel, noteLabel, phaseBox, buttonBox);

            javafx.scene.Scene dialogScene = new javafx.scene.Scene(dialogContent, 500, 400);
            dialog.setScene(dialogScene);
            dialog.setOnCloseRequest(e -> {if (wasRunning) runner.resume();});
            dialog.show();
        } catch (Exception e){
            showError("Failed to open editor: " + e.getMessage());
            e.printStackTrace();
            if (wasRunning) runner.resume();
        }
    }

    /**
     * Applies the new duration to the current active phase
     * 
     * @param adapter The TraCi adapter
     * @param junctionId The junction ID
     * @return true if successful, false otherwise
     */
    private boolean applyCurrentPhaseTiming(TraaSAdapter adapter, String junctionId) {
        try{
            // Validate input
            if (phaseDurationField == null) {
                showError("No duration field found");
                return false;
            }
            
            String text = phaseDurationField.getText().trim();
            if (text.isEmpty()){
                showError("Duration cannot be empty");
                return false;
            }

            double duration = Double.parseDouble(text);
            if (duration < 1){
                showError("Duration must be at least 1 second");
                return false;
            }

            if (duration > 300){
                showError("Duration cannot exceed 300 seconds");
                return false;
            }

            // Store custom duration for display
            customPhaseDuration = duration;
            customPhaseIndex = adapter.getCurrentPhase(junctionId);

            // Apply to SUMO - sets duration for current phase
            adapter.setPhaseDuration(junctionId, duration);

            // Show success
            showSuccess("Phase timing updated successfully!\nCurrent phase now has " + String.format("%.0f", duration) + " second duration.");

            System.out.println("===PHASE TIMING UPDATED===");
            System.out.println("Juntion: " + junctionId);
            System.out.println("Current phase duration set to : " + String.format("%.0f", duration) + "s");

            // Update phase display
            updatePhaseDisplay();

            return true;
        } catch (NumberFormatException e){
            showError("Invalid duration value. Please enter numbers only!");
            return false;
        } catch (Exception e){
            showError("Failed to apply timing: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Displays a success dialog with the specified message
     * 
     * @param message The success message to display
     */
    private void showSuccess(String message){
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Displays an error dialog with the specified message.
     * 
     * @param message The error message to display
     */
    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Update phase display with current phase index and phase duration
     * Shows total phase's duration and remaining time before next phase switching
     */
    private void updatePhaseDisplay() {
        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null) return;

            String junctionId = selectedLight.getJunctionId();

            // Get current phase index
            int currentPhase = adapter.getCurrentPhase(junctionId);

            // Check if phase changed - reset custom duration
            if (customPhaseIndex != -1 && currentPhase != customPhaseIndex) {
                customPhaseDuration = null;
                customPhaseIndex = -1;
            }

            // Use custom duration if set, otherwise get from SUMO
            double phaseDuration;
            if (customPhaseDuration != null && currentPhase == customPhaseIndex){
                phaseDuration = customPhaseDuration;
            } else {
                phaseDuration = adapter.getCurrentPhaseDuration(junctionId);
            }

            // Get next phase switching time and computer remaining time before next switching
            double nextSwitch = adapter.getNextSwitch(junctionId);
            double currentTime = adapter.getSimulationTime();
            double remainingTime = nextSwitch - currentTime;

            // Update labels
            currentPhaseLabel.setText("Current Phase: " + currentPhase);
            phaseDurationLabel.setText(String.format("Phase Duration: %.1f seconds", phaseDuration));
            remainingTimeLabel.setText(String.format("Remaining Time: %.1f seconds", remainingTime));

        } catch (Exception e) {
            currentPhaseLabel.setText("Current Phase: -");
            phaseDurationLabel.setText("Phase Duration: -");
            remainingTimeLabel.setText("Remaining Time: -");
        }
    }

    /**
     * Update remaining time display for smooth countdown
     * Called frequently (every 0.5 second) for real-time countdown
     */
    private void updateRemainingTime() {
        try{
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null) return;

            String junctionId = selectedLight.getJunctionId();

            // Compute remaining time
            double nextSwitch = adapter.getNextSwitch(junctionId);
            double currentTime = adapter.getSimulationTime();
            double remainingTime = nextSwitch - currentTime;

            // Update only the remaining time label
            remainingTimeLabel.setText(String.format("Remaining Time: %.1f seconds", remainingTime));
        } catch (Exception e){
            // Ignore
        }
    }

    /**
     * Returns the panel's VBox container.
     * 
     * @return The panel VBox
     */
    public VBox getPanel() {
        return panel;
    }

}
