import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;


/**
 * Main control panel for simulation control, view manipulation, and dashboard
 * display.
 * Provides buttons for play/pause/stop, zoom, and access to specialized panels.
 * 
 * @author M A T^2 H Team
 * @version 2.0
 * @see SimulationRunner
 * @see ViewManager
 */
public class ControlPanel {
    private static final Logger logger = Logger.getLogger(ControlPanel.class.getName());
    
    // Panel width constants
    private static final double EXPANDED_WIDTH = 300;
    private static final double MINIMIZED_WIDTH = 50;
    private static final double ANIMATION_DURATION_MS = 200;
    
    private VBox controlPanel;
    private VBox minimizedPanel;
    private ScrollPane scrollPane;
    private SimulationRunner runner;
    private ViewManager viewManager;
    private TrafficManager trafficManager;
    private VehicleAddPanel vehicleAddPanel;
    private CongestionMonitorPanel congestionMonitorPanel;
    private VehicleFilterPanel vehicleFilterPanel;
    private TrafficLight currentTrafficLight;
    
    // Expand/Minimize state
    private boolean isExpanded = true;
    private DoubleProperty panelWidth = new SimpleDoubleProperty(EXPANDED_WIDTH);
    private Button toggleButton;
    private VBox contentWrapper;

    // Callbacks for route selection mode
    private Runnable onStartRouteSelection;
    private Consumer<Boolean> onRouteSelectionModeChange;
    private Runnable onVehicleAdded;
    private StatisticsWindow statsWindow;
    /**
     * Constructs the control panel.
     * 
     * @param runner Simulation runner
     * @param viewManager View manager
     * @param dashboard Dashboard component
     * @param trafficManager Traffic manager
     */
    public ControlPanel(SimulationRunner runner, ViewManager viewManager, DashBoard dashboard,
            TrafficManager trafficManager) {
        this.runner = runner;
        this.viewManager = viewManager;
        this.trafficManager = trafficManager;
        createPanel(dashboard);
    }

    private void createPanel(DashBoard dashboard) {
        // Create main content panel
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
        
        // Add filter and congestion buttons
        addFilterAndCongestionControls();
        
        // Initialize panels
        congestionMonitorPanel = new CongestionMonitorPanel();
        congestionMonitorPanel.setOnToggleOverlay(() -> {
            trafficManager.setShowCongestionOverlay(!trafficManager.isShowCongestionOverlay());
        });
        congestionMonitorPanel.setOnBackPressed(this::showNormalControls);
        vehicleFilterPanel = new VehicleFilterPanel(this::showNormalControls);

        // Style the main control panel
        controlPanel.setPadding(new Insets(10));
        controlPanel.setSpacing(8);
        controlPanel.setStyle("-fx-background-color: " + UIStyles.BG_PRIMARY + ";");

        // Create toggle button for expand/minimize
        toggleButton = createToggleButton();
        
        // Create header with toggle button
        HBox header = createHeader();
        
        // Create content wrapper that holds header and scrollable content
        contentWrapper = new VBox();
        contentWrapper.setStyle("-fx-background-color: " + UIStyles.BG_PRIMARY + ";");
        
        // Create minimized panel (shown when collapsed)
        minimizedPanel = createMinimizedPanel();
        
        // Wrap main content in ScrollPane
        scrollPane = new ScrollPane(controlPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background: " + UIStyles.BG_PRIMARY + "; -fx-background-color: " + UIStyles.BG_PRIMARY + ";");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Add header and scroll pane to content wrapper
        contentWrapper.getChildren().addAll(header, scrollPane);
        
        // Bind width properties for smooth resizing
        contentWrapper.minWidthProperty().bind(panelWidth);
        contentWrapper.maxWidthProperty().bind(panelWidth);
        contentWrapper.prefWidthProperty().bind(panelWidth);
        scrollPane.minWidthProperty().bind(panelWidth);
        scrollPane.maxWidthProperty().bind(panelWidth);
    }
    
    /**
     * Creates the header section with the toggle button.
     * @return HBox containing the header elements
     */
    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5, 5, 5, 5));
        header.setStyle("-fx-background-color: " + UIStyles.BG_SECONDARY + ";");
        header.setMinHeight(35);
        header.setMaxHeight(35);
        
        Label titleLabel = new Label("Control Panel");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        header.getChildren().addAll(titleLabel, toggleButton);
        return header;
    }
    
    /**
     * Creates the toggle button for expand/minimize.
     * @return Button configured for toggling panel state
     */
    private Button createToggleButton() {
        Button btn = new Button("◀");
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip("Minimize Panel"));
        btn.setOnAction(e -> togglePanel());
        btn.setMinWidth(30);
        btn.setMaxWidth(30);
        
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + UIStyles.ACCENT_COLOR + "; -fx-text-fill: white; -fx-font-size: 14; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14; -fx-cursor: hand;"));
        
        return btn;
    }
    
    /**
     * Creates the minimized panel shown when control panel is collapsed.
     * @return VBox containing minimized view elements
     */
    private VBox createMinimizedPanel() {
        VBox panel = new VBox(8);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(10, 5, 10, 5));
        panel.setStyle("-fx-background-color: " + UIStyles.BG_PRIMARY + ";");
        
        // Expand button
        Button expandBtn = new Button("▶");
        expandBtn.setStyle("-fx-background-color: " + UIStyles.ACCENT_COLOR + "; -fx-text-fill: white; -fx-font-size: 14; -fx-cursor: hand;");
        expandBtn.setTooltip(new Tooltip("Expand Panel"));
        expandBtn.setOnAction(e -> togglePanel());
        expandBtn.setMinWidth(35);
        expandBtn.setMaxWidth(35);
        expandBtn.setMinHeight(35);
        
        // Quick action buttons (icons only)
        Button playBtn = createIconButton("▶", "Play", () -> { if (runner != null) runner.resume(); });
        Button pauseBtn = createIconButton("⏸", "Pause", () -> { if (runner != null) runner.pause(); });
        Button zoomInBtn = createIconButton("+", "Zoom In", () -> viewManager.zoomToCenter(1.2));
        Button zoomOutBtn = createIconButton("-", "Zoom Out", () -> viewManager.zoomToCenter(0.8));
        Button resetBtn = createIconButton("⟲", "Reset View", () -> viewManager.resetView());
        Button rotateLeftBtn = createIconButton("↺", "Rotate Left", () -> viewManager.rotate(-15));
        Button rotateRightBtn = createIconButton("↻", "Rotate Right", () -> viewManager.rotate(15));
        
        panel.getChildren().addAll(expandBtn, new Separator(), playBtn, pauseBtn, new Separator(), zoomInBtn, zoomOutBtn, resetBtn, new Separator(), rotateLeftBtn, rotateRightBtn);
        
        return panel;
    }
    
    /**
     * Creates a small icon button for minimized panel.
     * @param icon The icon text
     * @param tooltip Tooltip text
     * @param action Action to perform on click
     * @return Configured button
     */
    private Button createIconButton(String icon, String tooltip, Runnable action) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: " + UIStyles.BG_SECONDARY + "; -fx-text-fill: white; -fx-font-size: 12; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip(tooltip));
        btn.setOnAction(e -> action.run());
        btn.setMinWidth(35);
        btn.setMaxWidth(35);
        btn.setMinHeight(30);
        
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + UIStyles.ACCENT_COLOR + "; -fx-text-fill: white; -fx-font-size: 12; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + UIStyles.BG_SECONDARY + "; -fx-text-fill: white; -fx-font-size: 12; -fx-cursor: hand;"));
        
        return btn;
    }
    
    /**
     * Toggles the panel between expanded and minimized states with animation.
     */
    private void togglePanel() {
        isExpanded = !isExpanded;
        
        double targetWidth = isExpanded ? EXPANDED_WIDTH : MINIMIZED_WIDTH;
        
        // Animate width change
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(ANIMATION_DURATION_MS),
                new KeyValue(panelWidth, targetWidth)
            )
        );
        
        timeline.setOnFinished(e -> {
            if (isExpanded) {
                // Show full panel
                contentWrapper.getChildren().clear();
                HBox header = createHeader();
                contentWrapper.getChildren().addAll(header, scrollPane);
                toggleButton.setText("◀");
                toggleButton.setTooltip(new Tooltip("Minimize Panel"));
                logger.info("Control panel expanded");
            } else {
                // Show minimized panel
                contentWrapper.getChildren().clear();
                contentWrapper.getChildren().add(minimizedPanel);
                logger.info("Control panel minimized");
            }
        });
        
        // Start transitioning immediately for smoother experience
        if (!isExpanded) {
            toggleButton.setText("▶");
            toggleButton.setTooltip(new Tooltip("Expand Panel"));
        }
        
        timeline.play();
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
                logger.info("Simulation resumed");
            }
        });

        pauseBtn.setOnAction(e -> {
            if (runner != null) {
                runner.pause();
                logger.info("Simulation paused");
            }
        });

        stopBtn.setOnAction(e -> {
            if (runner != null) {
                runner.stop();
            }
            logger.info("Simulation stopped - Exiting application");
            Platform.exit();
            System.exit(0);
        });

        addVehicleBtn.setOnAction(e -> showVehicleAddPanel());

        controlPanel.getChildren().addAll(simLabel, playBtn, pauseBtn, stopBtn, addVehicleBtn);
    }

    /**
     * Adds filter and congestion monitoring buttons.
     */
    private void addFilterAndCongestionControls() {
        Label filterLabel = new Label("―――FILTERS & MONITORING―――");
        filterLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        
        Button vehicleFilterBtn = UIStyles.createStyledButton("Vehicle Filters");
        Button congestionBtn = UIStyles.createStyledButton("Congestion Monitor");
        
        vehicleFilterBtn.setOnAction(e -> showVehicleFilterPanel());
        congestionBtn.setOnAction(e -> showCongestionMonitorPanel());
        
        controlPanel.getChildren().addAll(filterLabel, vehicleFilterBtn, congestionBtn);
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
        
        // Rotation controls
        Button rotateLeft = UIStyles.createStyledButton("↺ Rotate Left");
        Button rotateRight = UIStyles.createStyledButton("↻ Rotate Right");
        Button resetRotation = UIStyles.createStyledButton("⊙ Reset Rotation");

        zoomIn.setOnAction(e -> viewManager.zoomToCenter(1.2));
        zoomOut.setOnAction(e -> viewManager.zoomToCenter(0.8));
        reset.setOnAction(e -> viewManager.resetView());
        
        // Rotation button actions (15 degrees per click)
        rotateLeft.setOnAction(e -> viewManager.rotate(-15));
        rotateRight.setOnAction(e -> viewManager.rotate(15));
        resetRotation.setOnAction(e -> viewManager.resetRotation());

        controlPanel.getChildren().addAll(viewLabel, zoomIn, zoomOut, reset, rotateLeft, rotateRight, resetRotation);
    }

    /**
     * Adds statistics control buttons (view stats, export csv)
     * @return The View Statistics section
     */
    private void addStatisticsControl() {
        Label statsLabel = new Label ("---Statistics---");
        statsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14");

        Button viewStatsButton = UIStyles.createAccentButton("View Live Statistics");
        Button exportCSV = UIStyles.createStyledButton("Export to CSV");

        viewStatsButton.setOnAction(e -> showStatisticsWindow());
        exportCSV.setOnAction(e -> TrafficDataExporter.exportToCSV(runner));

        controlPanel.getChildren().addAll(statsLabel, viewStatsButton, exportCSV);
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
     * Returns the content wrapper VBox that contains the entire control panel.
     * This should be used as the right component in BorderPane for proper sizing.
     * 
     * @return The content wrapper VBox
     */
    public VBox getContentWrapper() {
        return contentWrapper;
    }
    
    /**
     * Returns the panel width property for binding to canvas size.
     * 
     * @return The DoubleProperty representing current panel width
     */
    public DoubleProperty panelWidthProperty() {
        return panelWidth;
    }
    
    /**
     * Gets the current panel width.
     * 
     * @return Current width in pixels
     */
    public double getPanelWidth() {
        return panelWidth.get();
    }
    
    /**
     * Checks if the panel is currently expanded.
     * 
     * @return true if expanded, false if minimized
     */
    public boolean isExpanded() {
        return isExpanded;
    }
    
    /**
     * Expands the panel if it's currently minimized.
     */
    public void expand() {
        if (!isExpanded) {
            togglePanel();
        }
    }
    
    /**
     * Minimizes the panel if it's currently expanded.
     */
    public void minimize() {
        if (isExpanded) {
            togglePanel();
        }
    }

    /**
     * Sets callbacks for route selection mode interactions.
     * 
     * @param onStartRouteSelection      Called when route selection begins
     * @param onRouteSelectionModeChange Called when route selection mode changes
     * @param onVehicleAdded             Called when a vehicle is added
     */
    public void setRouteSelectionCallbacks(Runnable onStartRouteSelection, Consumer<Boolean> onRouteSelectionModeChange,
            Runnable onVehicleAdded) {
        this.onStartRouteSelection = onStartRouteSelection;
        this.onRouteSelectionModeChange = onRouteSelectionModeChange;
        this.onVehicleAdded = onVehicleAdded;
    }

    /**
     * Displays the vehicle addition panel, replacing the normal control panel.
     */
    public void showVehicleAddPanel() {
        // Ensure panel is expanded when showing vehicle add panel
        ensureExpanded();
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
     * If already showing a traffic light panel, just update to the new light.
     * 
     * @param tl The traffic light to control
     */
    public void showTrafficLightControl(TrafficLight tl) {
        // Ensure panel is expanded when showing traffic light control
        ensureExpanded();
        currentTrafficLight = tl;
        TrafficLightControlPanel tlPanel = new TrafficLightControlPanel(tl, runner, trafficManager,
                this::showNormalControls);
        scrollPane.setContent(tlPanel.getPanel());
    }
    
    /**
     * Shows the vehicle filter panel.
     */
    public void showVehicleFilterPanel() {
        // Ensure panel is expanded when showing vehicle filter panel
        ensureExpanded();
        scrollPane.setContent(vehicleFilterPanel.getPanel());
    }
    
    /**
     * Shows the congestion monitor panel.
     */
    public void showCongestionMonitorPanel() {
        // Ensure panel is expanded when showing congestion monitor
        ensureExpanded();
        scrollPane.setContent(congestionMonitorPanel);
    }
    
    /**
     * Ensures the panel is expanded (for when opening sub-panels).
     */
    private void ensureExpanded() {
        if (!isExpanded) {
            expand();
        }
    }

    /**
     * Restores the normal control panel view.
     */
    public void showNormalControls() {
        vehicleAddPanel = null; // Clear reference
        currentTrafficLight = null; // Clear traffic light reference
        // Ensure expanded state with correct content
        if (!isExpanded) {
            expand();
        }
        scrollPane.setContent(controlPanel);
        // Rebuild the content wrapper if it's been modified
        if (!contentWrapper.getChildren().contains(scrollPane)) {
            contentWrapper.getChildren().clear();
            HBox header = createHeader();
            contentWrapper.getChildren().addAll(header, scrollPane);
        }
    }
    
    /**
     * Returns the vehicle filter panel.
     * 
     * @return The vehicle filter panel
     */
    public VehicleFilterPanel getVehicleFilterPanel() {
        return vehicleFilterPanel;
    }
    
    /**
     * Checks if currently showing a traffic light control panel.
     * 
     * @return true if traffic light panel is active
     */
    public boolean isShowingTrafficLightPanel() {
        return currentTrafficLight != null;
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
