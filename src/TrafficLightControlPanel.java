import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

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

    // Button styles
    private final String greenStyle = "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private final String greenHover = "-fx-background-color: #388E3C; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private final String greenActive = "-fx-background-color: #1B5E20; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10; -fx-border-color: #4CAF50; -fx-border-width: 3;";

    private final String redStyle = "-fx-background-color: #C62828; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private final String redHover = "-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10;";
    private final String redActive = "-fx-background-color: #B71C1C; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10; -fx-border-color: #EF5350; -fx-border-width: 3;";

    public TrafficLightControlPanel(TrafficLight light, SimulationRunner runner, TrafficManager trafficManager,
            Runnable onBackPressed) {
        this.selectedLight = light;
        this.runner = runner;
        this.trafficManager = trafficManager;
        this.onBackPressed = onBackPressed;
        createUI();

        // Update UI to reflect current manual mode state
        if (light.isManualMode()) {
            statusLabel.setText("Mode: MANUAL");
            statusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-size: 14;");
            autoBtn.setDisable(false);
        }

        // Update button highlighting to show current state
        updateButtonStates();
    }

    private void createUI() {
        panel = new VBox(12);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #2b2b2b;");

        // Back button
        Button backBtn = createButton("← Back");
        backBtn.setOnAction(e -> onBackPressed.run());

        // Title
        Label titleLabel = new Label("TRAFFIC LIGHT CONTROL");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16;");

        // Info section
        VBox infoBox = new VBox(8);
        infoBox.setStyle("-fx-background-color: #3c3f41; -fx-padding: 10; -fx-background-radius: 5;");

        Label junctionLabel = new Label("Junction: " + selectedLight.getJunctionId());
        junctionLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12;");

        Label edgeLabel = new Label("Approach: " + selectedLight.getApproachEdgeId());
        edgeLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12;");

        Label linksLabel = new Label("Controls Links: " + selectedLight.getLinkIndices().toString());
        linksLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12;");
        linksLabel.setWrapText(true);

        statusLabel = new Label("Mode: AUTO");
        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 14;");

        infoBox.getChildren().addAll(junctionLabel, edgeLabel, linksLabel, statusLabel);

        // Current state display
        Label currentStateLabel = new Label("Current State:");
        currentStateLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");

        TextField currentStateField = new TextField();
        currentStateField.setEditable(false);
        currentStateField.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: #00ff00; " +
                "-fx-font-family: monospace; -fx-font-size: 12;");

        // Update current state periodically
        new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                String state = selectedLight.getCurrentState();
                if (state != null && !state.isEmpty()) {
                    currentStateField.setText(state);
                    // Highlight controlled indices
                    currentStateField.setTooltip(new javafx.scene.control.Tooltip(
                            "Link indices " + selectedLight.getLinkIndices() + " are highlighted"));
                }
            }
        }.start();

        // Quick control buttons
        Label quickControlLabel = new Label("Quick Controls:");
        quickControlLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");

        forceGreenBtn = createButton("Force GREEN");
        forceGreenBtn.setStyle(greenStyle);
        forceGreenBtn.setOnMouseEntered(e -> {
            if (!isCurrentlyGreen())
                forceGreenBtn.setStyle(greenHover);
        });
        forceGreenBtn.setOnMouseExited(e -> updateButtonStates());
        forceGreenBtn.setOnAction(e -> forceGreen());

        forceRedBtn = createButton("Force RED");
        forceRedBtn.setStyle(redStyle);
        forceRedBtn.setOnMouseEntered(e -> {
            if (!isCurrentlyRed())
                forceRedBtn.setStyle(redHover);
        });
        forceRedBtn.setOnMouseExited(e -> updateButtonStates());
        forceRedBtn.setOnAction(e -> forceRed());

        autoBtn = createButton("⟲ Return to AUTO");
        String blueStyle = "-fx-background-color: #1976D2; -fx-text-fill: white; " +
                "-fx-font-size: 12; -fx-padding: 10;";
        String blueHover = "-fx-background-color: #2196F3; -fx-text-fill: white; " +
                "-fx-font-size: 12; -fx-padding: 10;";
        autoBtn.setStyle(blueStyle);
        autoBtn.setOnMouseEntered(e -> autoBtn.setStyle(blueHover));
        autoBtn.setOnMouseExited(e -> autoBtn.setStyle(blueStyle));
        autoBtn.setOnAction(e -> returnToAuto());
        autoBtn.setDisable(true); // Initially disabled

        // Warning label
        Label warningLabel = new Label("⚠ Manual control overrides SUMO's automatic timing");
        warningLabel.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 10;");
        warningLabel.setWrapText(true);

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
                forceGreenBtn,
                forceRedBtn,
                autoBtn,
                warningLabel);
    }

    private void forceGreen() {
        String currentState = selectedLight.getCurrentState();
        if (currentState == null || currentState.isEmpty()) {
            showError("Cannot get current state from SUMO");
            return;
        }

        // Set all controlled links to 'G' (green with priority)
        char[] state = currentState.toCharArray();
        for (int idx : selectedLight.getLinkIndices()) {
            if (idx < state.length) {
                state[idx] = 'G';
            }
        }

        applyState(new String(state));
        enterManualMode();
        updateButtonStates();
    }

    private void forceRed() {
        String currentState = selectedLight.getCurrentState();
        if (currentState == null || currentState.isEmpty()) {
            showError("Cannot get current state from SUMO");
            return;
        }

        // Set all controlled links to 'r' (red)
        char[] state = currentState.toCharArray();
        for (int idx : selectedLight.getLinkIndices()) {
            if (idx < state.length) {
                state[idx] = 'r';
            }
        }

        applyState(new String(state));
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

            adapter.setTrafficLightState(selectedLight.getJunctionId(), newState);

            // Update ALL traffic lights at this junction
            trafficManager.setJunctionManualMode(selectedLight.getJunctionId(), true, newState);

            System.out.println("Applied state to junction " + selectedLight.getJunctionId() + ": " + newState);

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

            // Return SUMO to automatic control by setting default program
            adapter.setTrafficLightProgram(selectedLight.getJunctionId(), "0");

            // Update ALL traffic lights at this junction
            trafficManager.setJunctionManualMode(selectedLight.getJunctionId(), false, null);

            statusLabel.setText("Mode: AUTO");
            statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 14;");

            forceGreenBtn.setDisable(false);
            forceRedBtn.setDisable(false);
            autoBtn.setDisable(true);

            // Reset button styles
            updateButtonStates();

            System.out.println("Returned to AUTO mode for " + selectedLight.getJunctionId());

        } catch (Exception e) {
            showError("Failed to return to auto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateButtonStates() {
        if (isCurrentlyGreen()) {
            forceGreenBtn.setStyle(greenActive);
            forceRedBtn.setStyle(redStyle);
        } else if (isCurrentlyRed()) {
            forceGreenBtn.setStyle(greenStyle);
            forceRedBtn.setStyle(redActive);
        } else {
            forceGreenBtn.setStyle(greenStyle);
            forceRedBtn.setStyle(redStyle);
        }
    }

    private boolean isCurrentlyGreen() {
        String state = selectedLight.getCurrentState();
        if (state == null || state.isEmpty())
            return false;

        for (int idx : selectedLight.getLinkIndices()) {
            if (idx < state.length() && (state.charAt(idx) == 'G' || state.charAt(idx) == 'g')) {
                return true;
            }
        }
        return false;
    }

    private boolean isCurrentlyRed() {
        String state = selectedLight.getCurrentState();
        if (state == null || state.isEmpty())
            return false;

        for (int idx : selectedLight.getLinkIndices()) {
            if (idx < state.length() && (state.charAt(idx) == 'r' || state.charAt(idx) == 'R')) {
                return true;
            }
        }
        return false;
    }

    private void enterManualMode() {
        statusLabel.setText("Mode: MANUAL");
        statusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-size: 14;");

        // Keep force buttons enabled so you can keep changing states in manual mode
        autoBtn.setDisable(false);
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Button createButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(250);
        return btn;
    }

    public VBox getPanel() {
        return panel;
    }
}
