import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import java.util.List;

/**
 * Control panel for managing a selected traffic light
 */
public class TrafficLightControlPanel {
    private VBox panel;
    private TrafficLight selectedLight;
    private SimulationRunner runner;
    private TrafficManager trafficManager;
    private Runnable onBackPressed;

    // UI elements
    private Label statusLabel;
    private Button forceGreenBtn;
    private Button forceRedBtn;
    private Button autoBtn;
    private TextField currentStateField;

    // Button styles - static final for efficiency and consistency
    private static final String GREEN_STYLE = "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String GREEN_HOVER = "-fx-background-color: #388E3C; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String GREEN_ACTIVE = "-fx-background-color: #1B5E20; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10; -fx-border-color: #4CAF50; -fx-border-width: 3;";

    private static final String RED_STYLE = "-fx-background-color: #C62828; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String RED_HOVER = "-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String RED_ACTIVE = "-fx-background-color: #B71C1C; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10; -fx-border-color: #EF5350; -fx-border-width: 3;";

    private static final String AUTO_STYLE = "-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private static final String AUTO_HOVER = "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";

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
            @Override
            public void handle(long now) {
                updateStateDisplay();
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

        // Add all elements
        panel.getChildren().addAll(
                backBtn,
                titleLabel,
                infoBox,
                new javafx.scene.control.Separator(),
                currentStateLabel,
                currentStateField,
                new javafx.scene.control.Separator(),
                quickControlLabel,
                infoText,
                forceGreenBtn,
                forceRedBtn,
                autoBtn);
    }

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

    private void forceGreen() {
        forceSignalState('G', "GREEN");
    }

    private void forceRed() {
        forceSignalState('r', "RED");
    }

    /**
     * Force all signals of this traffic light to a specific state
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

    private boolean isCurrentlyGreen() {
        return isCurrentlyInState('G', 'g');
    }

    private boolean isCurrentlyRed() {
        return isCurrentlyInState('r', 'R');
    }

    /**
     * Check if all traffic light signals match any of the given states
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

    private void enterManualMode() {
        statusLabel.setText("Mode: MANUAL");
        statusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-size: 14;");
        autoBtn.setDisable(false);
    }
    
    /**
     * Update the UI to reflect the current mode state
     * Called when opening the panel to show remembered mode
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
     * Set manual/auto mode for ALL traffic lights at the same junction
     * This keeps mode synchronized while allowing individual force control
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

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public VBox getPanel() {
        return panel;
    }
}
