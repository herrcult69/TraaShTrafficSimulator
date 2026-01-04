import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Main JavaFX application for visualizing SUMO traffic simulations.
 * Handles coordinate transformation, rendering, user interaction, and real-time updates.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see TrafficManager
 * @see ViewManager
 */
public class TrafficSimulatorApp extends Application {
    private static final String NETWORK_FILE = "SumoConfig/network.net.xml";
    private static final String CONFIG_FILE = "SumoConfig/simulation.sumocfg";

    private NetworkParser.NetworkData network;
    private SimulationRunner runner;
    private ExecutorService exec;
    private Canvas canvas;
    private TrafficManager scene;
    private CoordinateTransform transform;
    private ViewManager viewManager;
    private Object selectedElement;
    private Object hoveredElement;
    private ControlPanel controlPanel;

    // Route selection mode
    private boolean routeSelectionMode = false;
    private List<String> selectedRouteEdges = new ArrayList<>();
    private Edge hoveredEdge = null;

    // Dashboard fields
    private DashBoard dashboard;
    private long lastDashboardUpdate = System.nanoTime();
    private static final double DASHBOARD_UPDATE_INTERVAL = 0.5; // Update every 0.5 seconds

    /**
     * Initializes and starts the JavaFX application.
     * 
     * @param stage The primary stage
     * @throws Exception if setup fails
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Initialize components
        network = NetworkParser.parse(NETWORK_FILE);
        canvas = new Canvas();
        scene = new TrafficManager();
        transform = new CoordinateTransform(900);

        // Initialize scene from network data
        scene.initializeFromNetwork(network);

        // Create view manager
        viewManager = new ViewManager(canvas, transform, network);

        // Create dashboard
        dashboard = new DashBoard();

        // Start simulation
        runner = new SimulationRunner(CONFIG_FILE, false);

        // Traffic lights are now initialized from network connections in initializeFromNetwork()
        // No need for separate SUMO initialization
        runner.setConnectionListener(adapter -> {
            Platform.runLater(() -> {
                System.out.println("SUMO connected - traffic lights already initialized from network");
            });
        });

        exec = Executors.newSingleThreadExecutor();
        exec.submit(runner);

        // Create control panel with all UI components
        controlPanel = new ControlPanel(runner, viewManager, dashboard, scene);

        // Set up route selection callbacks
        controlPanel.setRouteSelectionCallbacks(
                this::onStartRouteSelection,
                this::onRouteSelectionModeChange,
                this::onVehicleAdded);

        ScrollPane scrollPane = controlPanel.getScrollPane();

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setRight(scrollPane);

        Scene mainScene = new Scene(root, 1400, 900);
        stage.setScene(mainScene);
        stage.setTitle("Traffic Simulator - OOP Architecture");

        setupCanvas(root, stage);
        setupEventHandlers();

        stage.setOnCloseRequest(e -> {
            runner.stop();
            exec.shutdownNow();
            Platform.exit();
        });
    }

    /**
     * Main rendering loop (called ~60fps). Updates and draws all simulation objects.
     */
    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        g.setFill(Color.rgb(26, 36, 47));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        scene.updateVehicles(runner.getVehiclePositions());
        scene.updateTrafficLights(runner.getTrafficLightData());
        scene.render(g, viewManager.getTransform());
        scene.renderHighlight(g, viewManager.getTransform(), selectedElement, hoveredElement);

        // Render route selection highlights
        if (routeSelectionMode) {
            renderRouteSelection(g);
        }

        // Draw info box
        if (selectedElement != null && !routeSelectionMode) {
            drawInfoBox(g, selectedElement);
        }

        // Draw route selection instructions when in route mode
        if (routeSelectionMode) {
            drawRouteSelectionOverlay(g);
        }

        // Update dashboard at reduced frequency
        long currentTime = System.nanoTime();
        double timeSinceLastUpdate = (currentTime - lastDashboardUpdate) / 1_000_000_000.0;
        if (timeSinceLastUpdate >= DASHBOARD_UPDATE_INTERVAL) {
            updateDashboard();
            lastDashboardUpdate = currentTime;
        }
    }

    /**
     * Draws an info box showing details of the selected object.
     * 
     * @param g Graphics context
     * @param selected Selected object
     */
    private void drawInfoBox(GraphicsContext g, Object selected) {
        double x = 10;
        double y = 10;
        double width = 250;
        double height = 100;

        g.setFill(Color.rgb(0, 0, 0, 0.7));
        g.fillRect(x, y, width, height);
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.0);
        g.strokeRect(x, y, width, height);

        g.setFill(Color.WHITE);
        g.fillText("Selected: " + selected.getClass().getSimpleName(), x + 10, y + 20);

        if (selected instanceof Vehicle) {
            Vehicle v = (Vehicle) selected;
            g.fillText("ID: " + v.getId(), x + 10, y + 40);
            g.fillText("Type: " + v.getType(), x + 10, y + 60);
        } else if (selected instanceof Lane) {
            Lane l = (Lane) selected;
            g.fillText("ID: " + l.getId(), x + 10, y + 40);
            g.fillText("Width: " + String.format("%.2f", l.getWidth()) + "m", x + 10, y + 60);
        } else if (selected instanceof Junction) {
            Junction j = (Junction) selected;
            g.fillText("ID: " + j.getId(), x + 10, y + 40);
            g.fillText("Type: " + j.getType(), x + 10, y + 60);
        }
    }

    /**
     * Collects simulation metrics and updates the dashboard.
     */
    private void updateDashboard() {
        DashBoard.DashBoardData data = new DashBoard.DashBoardData();

        data.simTime = runner.getSimulationTime();
        collectVehicleMetrics(data);
        dashboard.update(data);
    }

    /** Callback when route selection starts. Clears selected edges. */
    private void onStartRouteSelection() {
        selectedRouteEdges.clear();
        System.out.println("Route selection mode started");
    }

    /**
     * Callback when route selection mode toggles.
     * 
     * @param active true if entering mode, false if exiting
     */
    private void onRouteSelectionModeChange(Boolean active) {
        routeSelectionMode = active;
        if (!active) {
            hoveredEdge = null;
            System.out.println("Route selection mode ended. Selected " + selectedRouteEdges.size() + " edges.");
        }
    }

    /** Callback when a vehicle is successfully added. */
    private void onVehicleAdded() {
        selectedRouteEdges.clear();
        routeSelectionMode = false;
        hoveredEdge = null;
        System.out.println("Vehicle added successfully!");
    }

    /**
     * Renders route selection highlights (green=start, red=end, cyan=route, yellow=hover).
     * 
     * @param g Graphics context
     */
    private void renderRouteSelection(GraphicsContext g) {
        VehicleAddPanel panel = controlPanel.getVehicleAddPanel();
        if (panel == null) return;

        CoordinateTransform t = viewManager.getTransform();
        highlightEdge(panel.getStartEdge(), Color.LIMEGREEN, g, t);
        highlightEdge(panel.getEndEdge(), Color.ORANGERED, g, t);
        
        String start = panel.getStartEdge();
        String end = panel.getEndEdge();
        for (String edgeId : panel.getSelectedRoute()) {
            if (!edgeId.equals(start) && !edgeId.equals(end)) {
                highlightEdge(edgeId, Color.CYAN, g, t);
            }
        }

        if (hoveredEdge != null) {
            String hoveredId = hoveredEdge.getNetworkEdge().id;
            if (!hoveredId.equals(start) && !hoveredId.equals(end) 
                    && !panel.getSelectedRoute().contains(hoveredId)) {
                hoveredEdge.highlight(g, t, Color.YELLOW);
            }
        }
    }

    /**
     * Highlights an edge by ID.
     * 
     * @param edgeId Edge ID
     * @param color Highlight color
     * @param g Graphics context
     * @param t Coordinate transform
     */
    private void highlightEdge(String edgeId, Color color, GraphicsContext g, CoordinateTransform t) {
        if (edgeId == null) return;
        Edge edge = scene.getEdgeById(edgeId);
        if (edge != null) edge.highlight(g, t, color);
    }

    /**
     * Draws route selection instructions overlay.
     * 
     * @param g Graphics context
     */
    private void drawRouteSelectionOverlay(GraphicsContext g) {
        VehicleAddPanel vehicleAddPanel = controlPanel.getVehicleAddPanel();

        double x = 10;
        double y = 10;
        double width = 300;
        double height = 100;

        // Semi-transparent background
        g.setFill(Color.rgb(30, 30, 30, 0.9));
        g.fillRect(x, y, width, height);
        g.setStroke(Color.LIMEGREEN);
        g.setLineWidth(2);
        g.strokeRect(x, y, width, height);

        // Title
        g.setFill(Color.LIMEGREEN);
        g.fillText("ROUTE SELECTION MODE", x + 10, y + 20);

        // Instructions based on state
        g.setFill(Color.WHITE);
        if (vehicleAddPanel != null) {
            String startEdge = vehicleAddPanel.getStartEdge();
            String endEdge = vehicleAddPanel.getEndEdge();

            if (startEdge == null) {
                g.setFill(Color.LIMEGREEN);
                g.fillText("[>] Click START edge (shown in GREEN)", x + 10, y + 45);
            } else if (endEdge == null) {
                g.fillText("✓ Start: " + startEdge, x + 10, y + 40);
                g.setFill(Color.ORANGERED);
                g.fillText("[>] Click END edge (shown in RED)", x + 10, y + 60);
            } else {
                g.setFill(Color.LIMEGREEN);
                g.fillText("✓ Start: " + startEdge, x + 10, y + 40);
                g.setFill(Color.ORANGERED);
                g.fillText("✓ End: " + endEdge, x + 10, y + 55);
                g.setFill(Color.CYAN);
                g.fillText("Route: " + vehicleAddPanel.getSelectedRoute().size() + " edges",
                        x + 10, y + 75);
            }
        } else {
            g.fillText("Click on road edges to select route", x + 10, y + 40);
        }
    }

    /**
     * Sets up canvas bindings and starts the animation loop.
     * 
     * @param root Root BorderPane
     * @param stage Primary stage
     */
    private void setupCanvas(BorderPane root, Stage stage) {
        canvas.widthProperty().bind(root.widthProperty().subtract(300));
        canvas.heightProperty().bind(root.heightProperty());

        canvas.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0 && canvas.getHeight() > 0) viewManager.resetView();
        });
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0 && canvas.getWidth() > 0) {
                transform = new CoordinateTransform(newVal.doubleValue());
                viewManager.setTransform(transform);
                viewManager.resetView();
            }
        });

        stage.show();
        Platform.runLater(() -> {
            if (canvas.getWidth() > 0 && canvas.getHeight() > 0) viewManager.resetView();
        });

        new javafx.animation.AnimationTimer() {
            public void handle(long now) { draw(); }
        }.start();
    }

    /** Sets up mouse and scroll event handlers for zoom, pan, and selection. */
    private void setupEventHandlers() {
        canvas.setOnScroll(e -> viewManager.zoomToPoint(e.getDeltaY() > 0 ? 1.1 : 0.9, e.getX(), e.getY()));
        canvas.setOnMousePressed(e -> viewManager.startPan(e.getX(), e.getY()));
        canvas.setOnMouseDragged(e -> viewManager.updatePan(e.getX(), e.getY()));
        canvas.setOnMouseMoved(e -> {
            hoveredElement = routeSelectionMode 
                ? scene.getEdgeAt(e.getX(), e.getY(), viewManager.getTransform())
                : scene.getElementAt(e.getX(), e.getY(), viewManager.getTransform());
            if (routeSelectionMode) hoveredEdge = (Edge) hoveredElement;
        });
        canvas.setOnMouseClicked(e -> handleCanvasClick(e.getX(), e.getY()));
    }

    /**
     * Handles canvas clicks for selection or route building.
     * 
     * @param x Screen X coordinate
     * @param y Screen Y coordinate
     */
    private void handleCanvasClick(double x, double y) {
        if (routeSelectionMode) {
            handleRouteSelectionClick(x, y);
            return;
        }

        Object clicked = scene.getElementAt(x, y, viewManager.getTransform());
        selectedElement = clicked;

        if (clicked instanceof TrafficLight) {
            TrafficLight tl = (TrafficLight) clicked;
            System.out.println("Clicked: Traffic Light - Junction: " + tl.getJunctionId() + 
                    " Signals: " + tl.getSignals().size());
            controlPanel.showTrafficLightControl(tl);
        } else {
            controlPanel.showNormalControls();
            if (clicked != null) logClickedElement(clicked);
        }
    }

    /**
     * Handles clicks during route selection.
     * 
     * @param x Screen X coordinate
     * @param y Screen Y coordinate
     */
    private void handleRouteSelectionClick(double x, double y) {
        String edgeId = scene.getEdgeIdAt(x, y, viewManager.getTransform());
        if (edgeId != null) {
            VehicleAddPanel panel = controlPanel.getVehicleAddPanel();
            if (panel != null) {
                panel.addEdgeToRoute(edgeId);
                selectedRouteEdges.add(edgeId);
            }
        }
    }

    /**
     * Logs clicked element info to console.
     * 
     * @param element Clicked object
     */
    private void logClickedElement(Object element) {
        System.out.println("Clicked: " + element.getClass().getSimpleName());
        if (element instanceof Junction) {
            Junction j = (Junction) element;
            System.out.println("Junction ID: " + j.getId() + " Type: " + j.getType());
        } else if (element instanceof Lane) {
            System.out.println("Lane ID: " + ((Lane) element).getId());
        } else if (element instanceof Vehicle) {
            Vehicle v = (Vehicle) element;
            System.out.println("Vehicle ID: " + v.getId() + " Type: " + v.getType());
        }
    }

    /**
     * Collects vehicle statistics for the dashboard.
     * 
     * @param data Dashboard data to populate
     */
    private void collectVehicleMetrics(DashBoard.DashBoardData data) {
        var positions = runner.getVehiclePositions();
        var speeds = runner.getVehicleSpeeds();
        
        double totalSpeed = 0.0;
        int[] counts = new int[5]; // car, truck, bus, moto, emergency
        
        for (var entry : positions.entrySet()) {
            String id = entry.getKey();
            if (id.startsWith("car")) counts[0]++;
            else if (id.startsWith("truck")) counts[1]++;
            else if (id.startsWith("bus")) counts[2]++;
            else if (id.startsWith("moto")) counts[3]++;
            else if (id.startsWith("ambu")) counts[4]++;
            totalSpeed += speeds.getOrDefault(id, 0.0);
        }
        
        int total = positions.size();
        data.activeVehicles = total;
        data.avgSpeed = total > 0 ? totalSpeed / total : 0.0;
        data.carCount = counts[0];
        data.truckCount = counts[1];
        data.busCount = counts[2];
        data.motorcycleCount = counts[3];
        data.emergencyCount = counts[4];
    }

    /**
     * Application entry point.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}