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
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Main JavaFX application for visualizing SUMO traffic simulations.
 * Handles coordinate transformation, rendering, user interaction, and real-time
 * updates.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see TrafficManager
 * @see ViewManager
 */
public class TrafficSimulatorApp extends Application {
    private static final Logger logger = Logger.getLogger(TrafficSimulatorApp.class.getName());
    
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
     * Initializes and starts the application.
     * 
     * @param stage Primary stage for the application
     * @throws Exception If network files cannot be loaded or initialization fails
     */
    @Override
    public void start(Stage stage) throws Exception {
        logger.info("=== Traffic Simulator Starting ===");
        try {
            // Initialize components
            logger.info("Loading network file: " + NETWORK_FILE);
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
                    logger.info("SUMO connection established - traffic lights initialized from network");
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

            logger.info("Application initialization complete");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "FATAL: Failed to initialize application", e);
            
            // Show error dialog to user
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Initialization Error");
            alert.setHeaderText("Failed to start Traffic Simulator");
            alert.setContentText("Error: " + e.getMessage() +
                    "\n\nPlease check that network files exist in SumoConfig/ directory.");
            alert.showAndWait();

            Platform.exit();
            throw e;
        }

        stage.setOnCloseRequest(e -> {
            runner.stop();
            exec.shutdownNow();
            Platform.exit();
        });
    }

    /**
     * Renders the simulation scene. Called approximately 60 times per second.
     */
    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        g.setFill(Color.rgb(26, 36, 47));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        scene.updateVehicles(runner.getVehiclePositions(), runner.getSimulationTime());
        scene.updateVehicleSpeeds(runner.getVehicleSpeeds());
        scene.updateEdgeStatistics(runner.getVehicleEdges());
        scene.updateCongestionHotspots(runner.getVehicleEdges(), runner.getVehicleSpeeds());
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

        // Update dashboard and congestion monitor at reduced frequency
        long currentTime = System.nanoTime();
        double timeSinceLastUpdate = (currentTime - lastDashboardUpdate) / 1_000_000_000.0;
        if (timeSinceLastUpdate >= DASHBOARD_UPDATE_INTERVAL) {
            updateDashboard();
            updateCongestionMonitor();
            lastDashboardUpdate = currentTime;
        }
    }

    /**
     * Draws an info box for the selected element.
     * 
     * @param g Graphics context
     * @param selected Selected simulation element
     */
    private void drawInfoBox(GraphicsContext g, Object selected) {
        double x = 10;
        double y = 10;
        double width = 250;
        double height = 100;
        
        // Increase height for vehicle to accommodate speed stats
        if (selected instanceof Vehicle) {
            height = 180;
        }

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
            g.fillText("Current Speed: " + String.format("%.2f", v.getCurrentSpeed() * 3.6) + " km/h", x + 10, y + 80);
            g.fillText("Average Speed: " + String.format("%.2f", v.getAverageSpeed() * 3.6) + " km/h", x + 10, y + 100);
            g.fillText("Max Speed: " + String.format("%.2f", v.getMaxSpeed() * 3.6) + " km/h", x + 10, y + 120);
            g.fillText("Travel Time: " + String.format("%.2f", v.getTravelTime()) + " s", x + 10, y + 140);
            g.fillText("Total Distance: " + String.format("%.2f", v.getTotalDistance()) + " m", x + 10, y + 160);
        } else if (selected instanceof Edge) {
            Edge edge = (Edge) selected;
            height = 140;
            g.setFill(Color.rgb(0, 0, 0, 0.7));
            g.fillRect(x, y, width, height);
            g.setStroke(Color.WHITE);
            g.setLineWidth(1.0);
            g.strokeRect(x, y, width, height);
            g.setFill(Color.WHITE);
            g.fillText("Selected: Edge", x + 10, y + 20);
            g.fillText("ID: " + edge.getNetworkEdge().id, x + 10, y + 40);
            g.fillText("Length: " + String.format("%.2f", edge.getEdgeLength()) + " m", x + 10, y + 60);
            g.fillText("Lanes: " + edge.getNetworkEdge().getNumLanes(), x + 10, y + 80);
            g.fillText("Vehicles: " + edge.getVehicleCount(), x + 10, y + 100);
            g.fillText("Density: " + String.format("%.2f", edge.getVehicleDensity()) + " veh/km", x + 10, y + 120);
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

    /** Updates dashboard with current simulation metrics. */
    private void updateDashboard() {
        DashBoard.DashBoardData data = new DashBoard.DashBoardData();

        data.simTime = runner.getSimulationTime();
        collectVehicleMetrics(data);
        dashboard.update(data);
    }
    
    /**
     * Updates the congestion monitor panel with current hotspot data.
     * Called at a reduced frequency to avoid excessive UI updates.
     */
    private void updateCongestionMonitor() {
        java.util.List<CongestionHotspot> topHotspots = scene.getTopCongestionHotspots(5);
        int totalCount = scene.getCongestionHotspots().size();
        controlPanel.getCongestionMonitorPanel().updateHotspots(topHotspots, totalCount);
    }

    /** Initializes route selection state when user begins selecting a vehicle route. */
    private void onStartRouteSelection() {
        selectedRouteEdges.clear();
        logger.info("Route selection mode started");
    }

    /**
     * Toggles route selection mode.
     * 
     * @param active True when entering route selection mode, false when exiting
     */
    private void onRouteSelectionModeChange(Boolean active) {
        routeSelectionMode = active;
        if (!active) {
            hoveredEdge = null;
            logger.info("Route selection ended. Selected " + selectedRouteEdges.size() + " edges");
        }
    }

    /** Resets route selection state after vehicle is added to simulation. */
    private void onVehicleAdded() {
        selectedRouteEdges.clear();
        routeSelectionMode = false;
        hoveredEdge = null;
        logger.info("Vehicle added successfully");
    }

    /**
     * Highlights edges during route selection mode.
     * 
     * @param g Graphics context
     */
    private void renderRouteSelection(GraphicsContext g) {
        VehicleAddPanel panel = controlPanel.getVehicleAddPanel();
        if (panel == null)
            return;

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
     * Highlights a specific edge with the given color.
     * 
     * @param edgeId Edge identifier
     * @param color Highlight color
     * @param g Graphics context
     * @param t Coordinate transform
     */
    private void highlightEdge(String edgeId, Color color, GraphicsContext g, CoordinateTransform t) {
        if (edgeId == null)
            return;
        Edge edge = scene.getEdgeById(edgeId);
        if (edge != null)
            edge.highlight(g, t, color);
    }

    /**
     * Displays on-screen instructions for route selection.
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
     * Configures canvas sizing and starts the rendering loop.
     * 
     * @param root Root layout container
     * @param stage Primary stage
     */
    private void setupCanvas(BorderPane root, Stage stage) {
        canvas.widthProperty().bind(root.widthProperty().subtract(300));
        canvas.heightProperty().bind(root.heightProperty());

        canvas.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0 && canvas.getHeight() > 0)
                viewManager.resetView();
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
            if (canvas.getWidth() > 0 && canvas.getHeight() > 0)
                viewManager.resetView();
        });

        new javafx.animation.AnimationTimer() {
            public void handle(long now) {
                draw();
            }
        }.start();
    }

    /** Configures mouse and scroll interactions. */
    private void setupEventHandlers() {
        canvas.setOnScroll(e -> viewManager.zoomToPoint(e.getDeltaY() > 0 ? 1.1 : 0.9, e.getX(), e.getY()));
        canvas.setOnMousePressed(e -> viewManager.startPan(e.getX(), e.getY()));
        canvas.setOnMouseDragged(e -> viewManager.updatePan(e.getX(), e.getY()));
        canvas.setOnMouseMoved(e -> {
            if (routeSelectionMode) {
                hoveredElement = scene.getEdgeAt(e.getX(), e.getY(), viewManager.getTransform());
                hoveredEdge = (Edge) hoveredElement;
            } else if (e.isControlDown()) {
                // If Ctrl is pressed, hover over edges instead of lanes
                hoveredElement = scene.getEdgeAt(e.getX(), e.getY(), viewManager.getTransform());
            } else {
                // Normal mode: hover over individual elements (vehicles, lanes, etc.)
                hoveredElement = scene.getElementAt(e.getX(), e.getY(), viewManager.getTransform());
            }
        });
        canvas.setOnMouseClicked(e -> handleCanvasClick(e.getX(), e.getY(), e.isControlDown()));
    }

    /**
     * Handles mouse clicks on the canvas.
     * 
     * @param x The X coordinate of the click in screen space
     * @param y The Y coordinate of the click in screen space
     * @param ctrlPressed Whether the Ctrl key is pressed
     */
    private void handleCanvasClick(double x, double y, boolean ctrlPressed) {
        if (routeSelectionMode) {
            handleRouteSelectionClick(x, y);
            return;
        }

        Object clicked;
        
        // If Ctrl is pressed, select edge instead of lane
        if (ctrlPressed) {
            clicked = scene.getEdgeAt(x, y, viewManager.getTransform());
        } else {
            clicked = scene.getElementAt(x, y, viewManager.getTransform());
        }
        
        selectedElement = clicked;

        if (clicked instanceof TrafficLight) {
            TrafficLight tl = (TrafficLight) clicked;
            logger.fine("Traffic Light clicked - Junction: " + tl.getJunctionId() + 
                    ", Signals: " + tl.getSignals().size());
            controlPanel.showTrafficLightControl(tl);
        } else {
            controlPanel.showNormalControls();
            if (clicked != null)
                logClickedElement(clicked);
        }
    }

    /**
     * Processes clicks while in route selection mode.
     * 
     * @param x X coordinate in screen space
     * @param y Y coordinate in screen space
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
     * Logs details about the clicked element.
     * 
     * @param element Clicked simulation element
     */
    private void logClickedElement(Object element) {
        logger.fine("Clicked: " + element.getClass().getSimpleName());
        if (element instanceof Junction) {
            Junction j = (Junction) element;
            System.out.println("Junction ID: " + j.getId() + " Type: " + j.getType());
        } else if (element instanceof Edge) {
            Edge edge = (Edge) element;
            System.out.println("Edge ID: " + edge.getNetworkEdge().id + " Vehicles: " + edge.getVehicleCount());
        } else if (element instanceof Lane) {
            logger.fine("Lane ID: " + ((Lane) element).getId());
        } else if (element instanceof Vehicle) {
            Vehicle v = (Vehicle) element;
            logger.fine("Vehicle ID: " + v.getId() + ", Type: " + v.getType());
        }
    }

    /**
     * Gathers vehicle statistics from the simulation.
     * 
     * @param data Dashboard data structure to populate
     */
    private void collectVehicleMetrics(DashBoard.DashBoardData data) {
        var positions = runner.getVehiclePositions();
        var speeds = runner.getVehicleSpeeds();

        double totalSpeed = 0.0;
        int[] counts = new int[5]; // car, truck, bus, moto, emergency

        for (var entry : positions.entrySet()) {
            String id = entry.getKey();
            if (id.startsWith("car"))
                counts[0]++;
            else if (id.startsWith("truck"))
                counts[1]++;
            else if (id.startsWith("bus"))
                counts[2]++;
            else if (id.startsWith("moto"))
                counts[3]++;
            else if (id.startsWith("ambu"))
                counts[4]++;
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
     * Application entry point. Initializes logging configuration and launches the JavaFX application.
     * Logs are written to both console and a timestamped file in the logs directory.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        configureLogging();
        launch(args);
    }

    /**
     * Configures the application logging system with console and file handlers.
     * Creates a timestamped log file in the logs directory for each application run.
     */
    private static void configureLogging() {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("logs"));
            
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            String logFilePath = "logs/traffic-simulator-" + timestamp + ".log";
            
            java.io.FileInputStream configFile = new java.io.FileInputStream("logging.properties");
            java.util.logging.LogManager.getLogManager().readConfiguration(configFile);
            configFile.close();
            
            java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
            java.util.logging.FileHandler fileHandler = new java.util.logging.FileHandler(logFilePath, true);
            fileHandler.setLevel(java.util.logging.Level.ALL);
            fileHandler.setFormatter(new java.util.logging.SimpleFormatter());
            rootLogger.addHandler(fileHandler);
            
            System.out.println("Logging configured - output to console and " + logFilePath);
        } catch (Exception e) {
            System.err.println("WARNING: Could not configure logging, using default settings");
            e.printStackTrace();
        }
    }
}