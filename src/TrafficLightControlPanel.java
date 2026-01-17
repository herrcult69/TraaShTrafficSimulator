import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Control panel for managing a selected traffic light manually.
 * Provides selective control over individual traffic light signals while
 * maintaining junction synchronization.
 * Supports forcing green/red states for specific signals and returning to
 * automatic SUMO control.
 * Manual mode is synchronized across all traffic lights at the same junction to
 * prevent conflicts.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see TrafficLight
 * @see TrafficManager
 * @see SimulationRunner
 * @see ControlPanel
 */
public class TrafficLightControlPanel {
    private static final Logger logger = Logger.getLogger(TrafficLightControlPanel.class.getName());
    
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

    // ========== Performance Metrics Tracking ==========
    private MetricsSnapshot beforeSnapshot;
    private MetricsSnapshot afterSnapshot;
    private long afterMeasurementStart = -1;
    private Button metricsBtn;
    private static final int MEASUREMENT_DURATION_MS = 15000; // 15 seconds observation period
    private static final double MEASUREMENT_RADIUS = 100.0; // meters around junction
    private static final double STOPPED_SPEED_THRESHOLD = 2.0; // m/s

    // Button styles - static final for efficiency and consistency
    private static final String GREEN_STYLE = "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String GREEN_HOVER = "-fx-background-color: #388E3C; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String GREEN_ACTIVE = "-fx-background-color: #1B5E20; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10; -fx-border-color: #4CAF50; -fx-border-width: 3;";

    private static final String RED_STYLE = "-fx-background-color: #C62828; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String RED_HOVER = "-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String RED_ACTIVE = "-fx-background-color: #B71C1C; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10; -fx-border-color: #EF5350; -fx-border-width: 3;";

    private static final String AUTO_STYLE = "-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String AUTO_HOVER = "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";

    private static final String PURPLE_STYLE = "-fx-background-color: #7B1FA2; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String PURPLE_HOVER = "-fx-background-color: #8E24AA; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";

    private static final String TEAL_STYLE = "-fx-background-color: #00897B; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String TEAL_HOVER = "-fx-background-color: #00ACC1; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";

    private static final double BUTTON_WIDTH = 250.0;

    /**
     * Constructs a traffic light control panel for the specified light.
     * 
     * @param light          The traffic light to control
     * @param runner         The simulation runner for SUMO communication
     * @param trafficManager The traffic manager for junction-wide mode
     *                       synchronization
     * @param onBackPressed  Callback invoked when back button is pressed
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
     * Creates the control panel UI with traffic light info, state display, and
     * control buttons.
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

        String infoStyle = "-fx-text-fill: #aaaaaa; -fx-font-size: 12;";

        Label junctionLabel = new Label("Junction: " + selectedLight.getJunctionId());
        junctionLabel.setStyle(infoStyle);

        List<Integer> linkIndices = selectedLight.getLinkIndices();
        Label signalLabel = new Label("Signals: " + selectedLight.getSignals().size() + " connections");
        signalLabel.setStyle(infoStyle);

        Label linksLabel = new Label("Controls Links: " + linkIndices);
        linksLabel.setStyle(infoStyle);
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
            private static final long UPDATE_INTERVAL = 500_000_000; // 0.5 real-life seconds in nanoseconds

            @Override
            public void handle(long now) {
                updateStateDisplay();

                // update remaining time every 0.5 seconds
                if (now - lastUpdate >= UPDATE_INTERVAL) {
                    lastUpdate = now;
                    updateRemainingTime();
                }

                // check if phase has changed
                try {
                    TraaSAdapter adapter = runner.getAdapter();
                    if (adapter != null) {
                        int currentPhase = adapter.getCurrentPhase(selectedLight.getJunctionId());
                        if (currentPhase != lastPhase) {
                            lastPhase = currentPhase;
                            updatePhaseDisplay();
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }

                // Check if AFTER measurement period is complete
                if (afterMeasurementStart > 0) {
                    long elapsed = System.currentTimeMillis() - afterMeasurementStart;
                    if (elapsed >= MEASUREMENT_DURATION_MS) {
                        afterSnapshot = captureCurrentMetrics();
                        afterMeasurementStart = -1; // Stop checking
                        logMetrics("AFTER METRICS CAPTURED", afterSnapshot);

                        // Show dialog on JavaFx thread
                        if (afterSnapshot != null) {
                            javafx.application.Platform.runLater(() -> {
                                try {
                                    logger.fine("Showing performance metrics completion dialog");
                                    showSuccess(
                                            "Observation complete! Click 'View Performance Metrics' to see comparison.");
                                    logger.fine("Performance dialog shown successfully");
                                } catch (Exception ex) {
                                    System.err.println("Failed to show dialog: " + ex.getMessage());
                                    ex.printStackTrace();
                                }
                            });

                        } else {
                            System.err.println("afterSnapshot is null - dialog not shown");
                        }
                    }
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
        forceGreenBtn.setPrefWidth(BUTTON_WIDTH);
        forceGreenBtn.setStyle(GREEN_STYLE);
        forceGreenBtn.setOnMouseEntered(e -> {
            if (!isCurrentlyGreen())
                forceGreenBtn.setStyle(GREEN_HOVER);
        });
        forceGreenBtn.setOnMouseExited(e -> updateButtonStates());
        forceGreenBtn.setOnAction(e -> forceGreen());

        forceRedBtn = new Button("Force RED (This Light Only)");
        forceRedBtn.setPrefWidth(BUTTON_WIDTH);
        forceRedBtn.setStyle(RED_STYLE);
        forceRedBtn.setOnMouseEntered(e -> {
            if (!isCurrentlyRed())
                forceRedBtn.setStyle(RED_HOVER);
        });
        forceRedBtn.setOnMouseExited(e -> updateButtonStates());
        forceRedBtn.setOnAction(e -> forceRed());

        autoBtn = new Button("⟲ Return Junction to AUTO");
        autoBtn.setPrefWidth(BUTTON_WIDTH);
        autoBtn.setStyle(AUTO_STYLE);
        autoBtn.setOnMouseEntered(e -> autoBtn.setStyle(AUTO_HOVER));
        autoBtn.setOnMouseExited(e -> autoBtn.setStyle(AUTO_STYLE));
        autoBtn.setOnAction(e -> returnToAuto());
        autoBtn.setDisable(true);

        // Phase information section
        Label phaseInfoTitle = new Label("Phase Information:");
        phaseInfoTitle.setStyle(UIStyles.LABEL_STYLE + " -fx-font-weight: bold;");

        currentPhaseLabel = new Label("Current Phase: -");
        currentPhaseLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12;");

        phaseDurationLabel = new Label("Phase Duration: -");
        phaseDurationLabel.setStyle("-fx-text-fill: #778DA9; -fx-font-size: 12;");

        remainingTimeLabel = new Label("Remaining Time: -");
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
                createMetricsButton(),
                new javafx.scene.control.Separator(),
                quickControlLabel,
                infoText,
                forceGreenBtn,
                forceRedBtn,
                autoBtn);
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
     * Modifies only the controlled link indices while preserving other junction
     * signals.
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
        logger.info(String.format("Traffic Light Control: Force %s - Junction: %s, Links: %s, Old: %s, New: %s",
                stateName, selectedLight.getJunctionId(), linkIndices, currentState, newState));

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

            logger.info("Applied manual state to junction " + selectedLight.getJunctionId() +
                    " (manual mode synced across junction)");

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
            // Refresh phase display to show AUTO mode values
            updatePhaseDisplay();

            logger.info("Returned to AUTO mode for junction " + selectedLight.getJunctionId() +
                    " (auto mode synced across junction)");

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
        if (state == null || state.isEmpty())
            return false;

        for (int linkIndex : selectedLight.getLinkIndices()) {
            if (linkIndex >= state.length())
                continue;
            char c = state.charAt(linkIndex);
            boolean matches = false;
            for (char validState : validStates) {
                if (c == validState) {
                    matches = true;
                    break;
                }
            }
            if (!matches)
                return false;
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

        // Set phase information to N/A in manual mode
        currentPhaseLabel.setText("Current Phase: N/A (manual mode)");
        phaseDurationLabel.setText("Phase Duration: N/A (manual mode)");
        remainingTimeLabel.setText("Remaining Time: N/A (manual mode)");
    }

    /**
     * Updates the UI to reflect the current mode state (manual or auto).
     * Called when opening the panel to show the remembered mode from previous
     * interactions.
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
     * This keeps mode synchronized across the junction while allowing individual
     * signal control.
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
        logger.info("Synchronized " + (manualMode ? "MANUAL" : "AUTO") +
                " mode across " + count + " traffic lights at junction " + junctionId);
    }

    /**
     * Creates a button to open the phase timing editor
     * 
     * @return Button to edit phase timing
     */
    private Button createPhaseTimingButton() {
        editTimingBtn = new Button("Edit Phase Timing");
        editTimingBtn.setPrefWidth(BUTTON_WIDTH);
        editTimingBtn.setStyle(PURPLE_STYLE);
        editTimingBtn.setOnMouseEntered(e -> editTimingBtn.setStyle(PURPLE_HOVER));
        editTimingBtn.setOnMouseExited(e -> editTimingBtn.setStyle(PURPLE_STYLE));
        editTimingBtn.setOnAction(e -> openPhaseTimingEditor());
        return editTimingBtn;
    }

    /**
     * Opens the phase timing editor
     * Pauses simulation, allows adjusting current phase duration
     */
    private void openPhaseTimingEditor() {
        // Pause simulation
        boolean wasRunning = !runner.isPaused();
        if (wasRunning) {
            runner.pause();
        }

        // Capture BEFORE metrics when pausing
        beforeSnapshot = captureCurrentMetrics();
        logMetrics("BEFORE METRICS CAPTURED", beforeSnapshot);

        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null) {
                showError("Sumo not connected");
                if (wasRunning)
                    runner.resume();
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

            Label currentPhaseTitle = new Label("Current Phase: " + currentPhase);
            currentPhaseTitle.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14; -fx-font-weight: bold;");

            Label currentDurationLabel = new Label(
                    "Current Duration: " + String.format("%.0f", currentDuration) + " seconds");
            currentDurationLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12;");

            Label newDurationLabel = new Label("New Duration (seconds)");
            newDurationLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12; -fx-font-weight: bold;");

            TextField durationField = new TextField(String.format("%.0f", currentDuration));
            durationField.setPrefWidth(200);
            durationField.setMaxWidth(200);
            durationField.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #FFA726; "
                    + "-fx-font-size: 14; -fx-font-weight: bold; -fx-alignment: center;");

            // Only allow numeric input
            durationField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter(),
                    currentDuration,
                    change -> {
                        String newText = change.getControlNewText();
                        if (newText.matches("\\d*\\.?\\d*")) {
                            return change;
                        }
                        return null;
                    }));

            phaseDurationField = durationField;

            phaseBox.getChildren().addAll(currentPhaseTitle, currentDurationLabel, new javafx.scene.control.Separator(),
                    newDurationLabel, durationField);

            // Buttons
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);

            Button applyBtn = new Button("✓ Apply & Resume");
            applyBtn.setPrefWidth(150);
            applyBtn.setStyle(AUTO_STYLE);
            applyBtn.setOnMouseEntered(e -> applyBtn.setStyle(AUTO_HOVER));
            applyBtn.setOnMouseExited(e -> applyBtn.setStyle(AUTO_STYLE));
            applyBtn.setOnAction(e -> {
                if (applyCurrentPhaseTiming(adapter, junctionId)) {
                    dialog.close();
                    if (wasRunning)
                        runner.resume();
                }
            });

            Button cancelBtn = new Button("x Cancel");
            cancelBtn.setPrefWidth(150);
            cancelBtn.setStyle(RED_STYLE);
            cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(RED_HOVER));
            cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(RED_STYLE));
            cancelBtn.setOnAction(e -> {
                dialog.close();
                if (wasRunning)
                    runner.resume();
            });

            buttonBox.getChildren().addAll(applyBtn, cancelBtn);

            dialogContent.getChildren().addAll(titleLabel, infoLabel, noteLabel, phaseBox, buttonBox);

            javafx.scene.Scene dialogScene = new javafx.scene.Scene(dialogContent, 500, 400);
            dialog.setScene(dialogScene);
            dialog.setOnCloseRequest(e -> {
                if (wasRunning)
                    runner.resume();
            });
            dialog.show();
        } catch (Exception e) {
            showError("Failed to open editor: " + e.getMessage());
            e.printStackTrace();
            if (wasRunning)
                runner.resume();
        }
    }

    /**
     * Applies the new duration to the current active phase
     * 
     * @param adapter    The TraCi adapter
     * @param junctionId The junction ID
     * @return true if successful, false otherwise
     */
    private boolean applyCurrentPhaseTiming(TraaSAdapter adapter, String junctionId) {
        try {
            // Validate input
            if (phaseDurationField == null) {
                showError("No duration field found");
                return false;
            }

            String text = phaseDurationField.getText().trim();
            if (text.isEmpty()) {
                showError("Duration cannot be empty");
                return false;
            }

            double duration = Double.parseDouble(text);
            if (duration < 1) {
                showError("Duration must be at least 1 second");
                return false;
            }

            if (duration > 300) {
                showError("Duration cannot exceed 300 seconds");
                return false;
            }

            // Store custom duration for display
            customPhaseDuration = duration;
            customPhaseIndex = adapter.getCurrentPhase(junctionId);

            // Apply to SUMO - sets duration for current phase
            adapter.setPhaseDuration(junctionId, duration);

            // Start AFTER measurement period
            afterMeasurementStart = System.currentTimeMillis();
            showInfo("Phase timing updated!\nObserving for 15 seconds to capture performance metrics...");

            logger.info(String.format("Phase timing updated - Junction: %s, Duration: %.0fs",
                    junctionId, duration));

            // Update phase display
            updatePhaseDisplay();

            return true;
        } catch (NumberFormatException e) {
            showError("Invalid duration value. Please enter numbers only!");
            return false;
        } catch (Exception e) {
            showError("Failed to apply timing: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Displays an alert dialog with configurable blocking behavior.
     * 
     * @param type     Alert type (ERROR or INFORMATION)
     * @param title    Dialog window title
     * @param message  Message content to display
     * @param blocking If true, blocks execution until user closes (showAndWait);
     *                 if false, shows non-blocking dialog (show)
     */
    private void showAlert(javafx.scene.control.Alert.AlertType type, String title, String message, boolean blocking) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (blocking) {
            alert.showAndWait();
        } else {
            alert.show();
        }
    }

    /**
     * Logs metrics snapshot data to console for debugging purposes.
     * Outputs average speed, queue length, and wait time, or a warning if snapshot
     * is null.
     * 
     * @param label    Descriptive label for the log entry (e.g., "BEFORE METRICS
     *                 CAPTURED")
     * @param snapshot The metrics snapshot to log, or null if capture failed
     */
    private void logMetrics(String label, MetricsSnapshot snapshot) {
        if (snapshot != null) {
            logger.info(String.format("%s: Avg Speed: %.2f m/s, Queue: %d, Wait Time: %.1fs",
                    label, snapshot.avgSpeed, snapshot.queueLength, snapshot.waitTime));
        } else {
            logger.warning("Failed to capture metrics: " + label);
        }
    }

    /**
     * Displays a blocking success dialog with the specified message.
     * User must click OK before execution continues.
     * 
     * @param message The success message to display
     */
    private void showSuccess(String message) {
        showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Success", message, true);
    }

    /**
     * Displays a non-blocking information dialog with the specified message.
     * Execution continues immediately without waiting for user interaction.
     * 
     * @param message The information message to display
     */
    private void showInfo(String message) {
        showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Information", message, false);
    }

    /**
     * Displays a blocking error dialog with the specified message.
     * User must acknowledge the error before execution continues.
     * 
     * @param message The error message to display
     */
    private void showError(String message) {
        showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", message, true);
    }

    /**
     * Update phase display with current phase index and phase duration
     * Shows total phase's duration and remaining time before next phase switching
     */
    private void updatePhaseDisplay() {
        // Check if in manual mode
        if (selectedLight.isManualMode()) {
            currentPhaseLabel.setText("Current Phase: N/A (manual mode)");
            phaseDurationLabel.setText("Phase Duration: N/A (manual mode)");
            remainingTimeLabel.setText("Remaining Time: N/A (manual mode)");
            return;
        }
        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null)
                return;

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
            if (customPhaseDuration != null && currentPhase == customPhaseIndex) {
                phaseDuration = customPhaseDuration;
            } else {
                phaseDuration = adapter.getCurrentPhaseDuration(junctionId);
            }

            // Get next phase switching time and computer remaining time before next
            // switching
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
        // Skip updates in manual mode
        if (selectedLight.isManualMode()) {
            return;
        }
        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null)
                return;

            String junctionId = selectedLight.getJunctionId();

            // Compute remaining time
            double nextSwitch = adapter.getNextSwitch(junctionId);
            double currentTime = adapter.getSimulationTime();
            double remainingTime = nextSwitch - currentTime;

            // Update only the remaining time label
            remainingTimeLabel.setText(String.format("Remaining Time: %.1f seconds", remainingTime));
        } catch (Exception e) {
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

    /**
     * Creates a button to view performance metrics comparison.
     * Opens the metrics dialog showing before/after traffic performance data
     * when phase timing changes are made.
     * 
     * @return Button configured with teal styling that opens the metrics dialog
     */
    private Button createMetricsButton() {
        metricsBtn = new Button("View Performance Metrics");
        metricsBtn.setPrefWidth(BUTTON_WIDTH);
        metricsBtn.setStyle(TEAL_STYLE);
        metricsBtn.setOnMouseEntered(e -> metricsBtn.setStyle(TEAL_HOVER));
        metricsBtn.setOnMouseExited(e -> metricsBtn.setStyle(TEAL_STYLE));
        metricsBtn.setOnAction(e -> showMetricsDialog());
        return metricsBtn;
    }

    /**
     * Captures current traffic metrics for vehicles near the selected junction.
     * Collects average speed, queue length, and waiting time for all vehicles
     * within MEASUREMENT_RADIUS (100m) of the junction center.
     * 
     * @return MetricsSnapshot containing averaged traffic metrics, or null if
     *         capture fails
     *         (e.g., SUMO disconnected, junction not found, no vehicles nearby)
     */
    private MetricsSnapshot captureCurrentMetrics() {
        try {
            TraaSAdapter adapter = runner.getAdapter();
            if (adapter == null)
                return null;

            Junction junction = findJunctionByIdInternal(selectedLight.getJunctionId());
            if (junction == null)
                return null;

            double junctionX = junction.getX();
            double junctionY = junction.getY();

            // Get all vehicles from traffic manager
            Map<String, Vehicle> allVehicles = trafficManager.getVehicles();
            Map<String, Double> vehicleSpeeds = runner.getVehicleSpeeds();

            // Calculate metrics
            List<Double> speeds = new ArrayList<>();
            int queueCount = 0;
            List<Double> waitTimes = new ArrayList<>();

            for (Vehicle v : allVehicles.values()) {
                double dist = Math.sqrt(
                        Math.pow(v.getWorldX() - junctionX, 2) +
                                Math.pow(v.getWorldY() - junctionY, 2));

                if (dist <= MEASUREMENT_RADIUS) {
                    // Get speed
                    Double speed = vehicleSpeeds.get(v.getId());
                    if (speed != null) {
                        speeds.add(speed);

                        // Count as queue if stopped/slow
                        if (speed < STOPPED_SPEED_THRESHOLD) {
                            queueCount++;
                        }
                    }

                    // Get actual waiting time from SUMO
                    try {
                        double waitTime = adapter.getVehicleWaitingTime(v.getId());
                        waitTimes.add(waitTime);
                    } catch (Exception e) {
                        // Skip if waiting time unavailable for this vehicle
                    }
                }
            }

            // Calculate averages
            double avgSpeed = speeds.isEmpty() ? 0.0
                    : speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            // Use average actual waiting time from SUMO instead of simplified estimation
            double avgWaitTime = waitTimes.isEmpty() ? 0.0
                    : waitTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            return new MetricsSnapshot(
                    selectedLight.getJunctionId(),
                    avgSpeed,
                    queueCount,
                    avgWaitTime,
                    adapter.getSimulationTime());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Finds and returns the Junction object matching the specified junction ID.
     * Searches through all junctions managed by the traffic manager.
     * 
     * @param junctionId The identifier of the junction to find
     * @return The Junction object with matching ID, or null if not found
     */
    private Junction findJunctionByIdInternal(String junctionId) {
        for (Junction j : trafficManager.getJunctions()) {
            if (j.getId().equals(junctionId)) {
                return j;
            }
        }
        return null;
    }

    /**
     * Displays a modal dialog showing performance metrics comparison.
     * Shows before/after traffic metrics if both snapshots are available,
     * observation progress if measurement is in progress, or instructions
     * for capturing metrics if no data is available yet.
     * 
     * The dialog displays:
     * - Before/After comparison table with speed, queue length, and wait time
     * - Recommendation based on performance changes
     * - Real-time countdown during observation period
     * - Step-by-step instructions when no metrics are captured
     */
    private void showMetricsDialog() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("Performance Metrics - " + selectedLight.getJunctionId());
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: " + UIStyles.BG_PRIMARY + ";");

        Label title = new Label("PERFORMANCE COMPARISON");
        title.setStyle(UIStyles.TITLE_STYLE);
        content.getChildren().add(title);

        // Show Before/After comparison if available
        if (beforeSnapshot != null && afterSnapshot != null) {
            Label compareTitle = new Label("Before vs After Phase Duration Change:");
            compareTitle.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 16; -fx-font-weight: bold;");
            compareTitle.setAlignment(Pos.CENTER);
            compareTitle.setMaxWidth(Double.MAX_VALUE);

            Label comparison = new Label(formatComparison(beforeSnapshot, afterSnapshot));
            comparison.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16; -fx-font-family: monospace;");
            comparison.setAlignment(Pos.CENTER);
            comparison.setMaxWidth(Double.MAX_VALUE);

            content.getChildren().addAll(compareTitle, comparison);
        } else {
            // Show status/instructions
            Label statusLabel = new Label();

            if (beforeSnapshot == null && afterSnapshot == null) {
                statusLabel.setText("To measure performance impact:\n\n" +
                        "1. Click 'Edit Phase Timing'\n" +
                        "2. Change the duration and click 'Apply & Resume'\n" +
                        "3. Wait " + (MEASUREMENT_DURATION_MS / 1000) + " seconds for observation\n" +
                        "4. Return here to see before/after comparison\n\n" +
                        "Current real-time metrics are shown in the Dashboard.");
                statusLabel.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 16;");
            } else if (beforeSnapshot != null && afterSnapshot == null) {
                if (afterMeasurementStart > 0) {
                    long elapsed = System.currentTimeMillis() - afterMeasurementStart;
                    long remaining = (MEASUREMENT_DURATION_MS - elapsed) / 1000;
                    statusLabel.setText("⏱ Observation in progress...\n\n" +
                            "Time remaining: " + Math.max(0, remaining) + " seconds\n\n" +
                            "Come back after observation completes to see comparison.\n" +
                            "You can close this dialog and continue using the application.");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 16;");
                } else {
                    statusLabel.setText("✓ BEFORE metrics captured\n\n" +
                            "Ready to apply timing change.\n" +
                            "Edit phase duration and the system will automatically\n" +
                            "measure the AFTER performance.");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 16;");
                }
            }

            statusLabel.setWrapText(true);
            statusLabel.setAlignment(Pos.CENTER);
            statusLabel.setMaxWidth(Double.MAX_VALUE);
            content.getChildren().add(statusLabel);
        }

        Button closeBtn = UIStyles.createStyledButton("Close");
        closeBtn.setOnAction(e -> dialog.close());
        content.getChildren().add(closeBtn);

        javafx.scene.Scene scene = new javafx.scene.Scene(content, 750, 550);
        dialog.setScene(scene);
        dialog.show();
    }

    /**
     * Formats before/after metrics comparison into a readable text table.
     * 
     * @param before The metrics snapshot captured before phase timing change
     * @param after  The metrics snapshot captured after phase timing change
     * @return Formatted string showing BEFORE, AFTER, and CHANGE columns with
     *         recommendation
     */
    private String formatComparison(MetricsSnapshot before, MetricsSnapshot after) {
        double speedDiff = after.avgSpeed - before.avgSpeed;
        int queueDiff = after.queueLength - before.queueLength;
        double waitDiff = after.waitTime - before.waitTime;

        return String.format(
                "                    BEFORE    AFTER     CHANGE\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "Avg Speed:          %.2f     %.2f      %s%.2f m/s\n" +
                        "Queue Length:       %d       %d        %s%d veh\n" +
                        "Est. Wait Time:     %.1f     %.1f      %s%.1f s\n\n" +
                        "%s",
                before.avgSpeed, after.avgSpeed,
                (Math.abs(speedDiff) < 0.01 ? "±" : (speedDiff < 0 ? "-" : "+")), Math.abs(speedDiff),
                before.queueLength, after.queueLength,
                (queueDiff == 0 ? "±" : (queueDiff > 0 ? "+" : "-")), Math.abs(queueDiff),
                before.waitTime, after.waitTime,
                (Math.abs(waitDiff) < 0.01 ? "±" : (waitDiff > 0 ? "+" : "-")), Math.abs(waitDiff),
                getRecommendation(speedDiff, queueDiff, waitDiff));
    }

    /**
     * Generates a recommendation message based on traffic metrics changes.
     * 
     * @param speedDiff Change in average speed (after - before) in m/s
     * @param queueDiff Change in queue length (after - before) in vehicles
     * @param waitDiff  Change in wait time (after - before) in seconds
     * @return Recommendation string: IMPROVED, WORSENED, or NEUTRAL with
     *         explanation
     */
    private String getRecommendation(double speedDiff, int queueDiff, double waitDiff) {
        if (speedDiff > 0.5 && queueDiff < -1 && waitDiff < -2) {
            return "✓ IMPROVED: Timing changes reduced congestion significantly!";
        } else if (speedDiff > 0.2 && (queueDiff <= 0 || waitDiff < 0)) {
            return "✓ IMPROVED: Traffic flow slightly better with new timing.";
        } else if (speedDiff < -0.5 || queueDiff > 2 || waitDiff > 5) {
            return "✗ WORSENED: Consider reverting timing changes.";
        } else {
            return "~ NEUTRAL: Minor impact. Try longer observation period.";
        }
    }

    /**
     * A data class storing a snapshot of traffic metrics at a specific moment.
     * Used to capture before/after performance data for comparison when phase
     * timing changes.
     * 
     * @param junctionId  The junction ID where metrics were captured
     * @param avgSpeed    Average vehicle speed in m/s within measurement radius
     * @param queueLength Number of stopped/slow vehicles (speed < 2.0 m/s)
     * @param waitTime    Average waiting time in seconds
     * @param timestamp   Simulation time when snapshot was captured
     */
    @SuppressWarnings("unused")
    private static class MetricsSnapshot {
        final String junctionId;
        final double avgSpeed;
        final int queueLength;
        final double waitTime;
        final double timestamp;

        MetricsSnapshot(String junctionId, double avgSpeed, int queueLength,
                double waitTime, double timestamp) {
            this.junctionId = junctionId;
            this.avgSpeed = avgSpeed;
            this.queueLength = queueLength;
            this.waitTime = waitTime;
            this.timestamp = timestamp;
        }
    }
}


