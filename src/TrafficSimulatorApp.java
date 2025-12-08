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
 * Real-time SUMO Traffic Visualization with JavaFX
 * 
 * Features:
 * - Coordinate transformation between SUMO (Y-up) and JavaFX (Y-down)
 * - Multi-level scaling: base scale + user zoom + pan offset
 * - Object-oriented rendering with hit detection
 * - 60fps animation with background simulation thread
 * - Vehicle injection with route selection
 */
public class TrafficSimulatorApp extends Application {
    private static final String NETWORK_FILE = "resource/network.net.xml";
    private static final String CONFIG_FILE = "resource/simulation.sumocfg";

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

        // Set up listener to initialize traffic lights when SUMO connects
        runner.setConnectionListener(adapter -> {
            Platform.runLater(() -> {
                try {
                    scene.initializeTrafficLightsFromSUMO(adapter);
                    System.out.println("Traffic lights initialized from SUMO");
                } catch (Exception e) {
                    System.err.println("Error initializing traffic lights: " + e.getMessage());
                    e.printStackTrace();
                }
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

        // Make canvas fill the available space (subtract fixed right panel width)
        canvas.widthProperty().bind(root.widthProperty().subtract(300));
        canvas.heightProperty().bind(root.heightProperty());

        // Update view when canvas size changes
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0 && canvas.getHeight() > 0) {
                viewManager.resetView();
            }
        });
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0 && canvas.getWidth() > 0) {
                transform = new CoordinateTransform(newVal.doubleValue());
                viewManager.setTransform(transform);
                viewManager.resetView();
            }
        });

        stage.show();

        // Initialize view after stage is shown and canvas has proper size
        Platform.runLater(() -> {
            if (canvas.getWidth() > 0 && canvas.getHeight() > 0) {
                viewManager.resetView();
            }
        });

        // Animation and interactions
        new javafx.animation.AnimationTimer() {
            public void handle(long now) {
                draw();
            }
        }.start();

        canvas.setOnScroll(e -> viewManager.zoomToPoint(e.getDeltaY() > 0 ? 1.1 : 0.9, e.getX(), e.getY()));
        canvas.setOnMousePressed(e -> viewManager.startPan(e.getX(), e.getY()));
        canvas.setOnMouseDragged(e -> viewManager.updatePan(e.getX(), e.getY()));
        canvas.setOnMouseMoved(e -> {
            if (routeSelectionMode) {
                // In route selection mode, highlight edges
                hoveredEdge = scene.getEdgeAt(e.getX(), e.getY(), viewManager.getTransform());
            } else {
                hoveredElement = scene.getElementAt(e.getX(), e.getY(), viewManager.getTransform());
            }
        });

        // Click detection
        canvas.setOnMouseClicked(e -> {
            // Handle route selection mode
            if (routeSelectionMode) {
                String edgeId = scene.getEdgeIdAt(e.getX(), e.getY(), viewManager.getTransform());
                if (edgeId != null) {
                    // Add edge to route via the VehicleAddPanel
                    VehicleAddPanel vehicleAddPanel = controlPanel.getVehicleAddPanel();
                    if (vehicleAddPanel != null) {
                        vehicleAddPanel.addEdgeToRoute(edgeId);
                        selectedRouteEdges.add(edgeId);
                    }
                }
                return;
            }

            Object clickedElement = scene.getElementAt(e.getX(), e.getY(), viewManager.getTransform());
            selectedElement = clickedElement;

            if (clickedElement instanceof TrafficLight) {
                TrafficLight tl = (TrafficLight) clickedElement;
                System.out.println("Clicked: Traffic Light - Junction: " + tl.getJunctionId() + " Approach: "
                        + tl.getApproachEdgeId());
                controlPanel.showTrafficLightControl(tl);
            } else {
                // Hide traffic light control panel when clicking anything else
                controlPanel.showNormalControls();

                if (clickedElement != null) {
                    System.out.println("Clicked: " + clickedElement.getClass().getSimpleName());
                    if (clickedElement instanceof Junction) {
                        Junction junction = (Junction) clickedElement;
                        System.out.println("Junction ID: " + junction.getId() + " Type: " + junction.getType());
                    } else if (clickedElement instanceof Lane) {
                        Lane lane = (Lane) clickedElement;
                        System.out.println("Lane ID: " + lane.getId());
                    } else if (clickedElement instanceof Vehicle) {
                        Vehicle vehicle = (Vehicle) clickedElement;
                        System.out.println("Vehicle ID: " + vehicle.getId() + " Type: " + vehicle.getType());
                    }
                }
            }
        });

        stage.setOnCloseRequest(e -> {
            runner.stop();
            exec.shutdownNow();
            Platform.exit();
        });
    }

    /** Main rendering loop - called ~60 times per second */
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

    // Collect metrics and update dashboard
    private void updateDashboard() {
        DashBoard.DashBoardData data = new DashBoard.DashBoardData();

        var positions = runner.getVehiclePositions();
        var speeds = runner.getVehicleSpeeds();

        data.simTime = runner.getSimulationTime(); // Use actual SUMO time

        double totalSpeed = 0.0;
        int carCount = 0, truckCount = 0, busCount = 0, motoCount = 0, emergencyCount = 0;
        int totalVehicles = positions.size();

        for (var entry : positions.entrySet()) {
            String id = entry.getKey();

            // Count vehicle types
            if (id.startsWith("car"))
                carCount++;
            else if (id.startsWith("truck"))
                truckCount++;
            else if (id.startsWith("bus"))
                busCount++;
            else if (id.startsWith("moto"))
                motoCount++;
            else if (id.startsWith("ambu"))
                emergencyCount++;

            // Get speed
            double speed = speeds.getOrDefault(id, 0.0);
            totalSpeed += speed;
        }

        data.activeVehicles = totalVehicles;
        data.avgSpeed = totalVehicles > 0 ? totalSpeed / totalVehicles : 0.0;
        data.carCount = carCount;
        data.truckCount = truckCount;
        data.busCount = busCount;
        data.motorcycleCount = motoCount;
        data.emergencyCount = emergencyCount;
        dashboard.update(data);
    }

    //Route Selection Mode Methods (Open new overlay from the current control panel)

    private void onStartRouteSelection() {
        selectedRouteEdges.clear();
        System.out.println("Route selection mode started");
    }

    private void onRouteSelectionModeChange(Boolean active) {
        routeSelectionMode = active;
        if (!active) {
            hoveredEdge = null;
            System.out.println("Route selection mode ended. Selected " + selectedRouteEdges.size() + " edges.");
        }
    }

    private void onVehicleAdded() {
        selectedRouteEdges.clear();
        routeSelectionMode = false;
        hoveredEdge = null;
        System.out.println("Vehicle added successfully!");
    }

    private void renderRouteSelection(GraphicsContext g) {
        VehicleAddPanel vehicleAddPanel = controlPanel.getVehicleAddPanel();
        if (vehicleAddPanel == null)
            return;

        // Highlight start edge in green
        String startEdgeId = vehicleAddPanel.getStartEdge();
        if (startEdgeId != null) {
            Edge startEdge = scene.getEdgeById(startEdgeId);
            if (startEdge != null) {
                startEdge.highlight(g, viewManager.getTransform(), Color.LIMEGREEN);
            }
        }

        // Highlight end edge in red 
        // You will sometime sees the end edge as blue, but it's actually red, the vehicle made an U-turn at the end of the map, repeate the end edge (Check the Notice in TraaSAdapter.java)
        String endEdgeId = vehicleAddPanel.getEndEdge();
        if (endEdgeId != null) {
            Edge endEdge = scene.getEdgeById(endEdgeId);
            if (endEdge != null) {
                endEdge.highlight(g, viewManager.getTransform(), Color.ORANGERED);
            }
        }

        // Highlight computed route edges in cyan
        List<String> route = vehicleAddPanel.getSelectedRoute();
        for (String edgeId : route) {
            // Skip start and end (they have their own colors)
            if (edgeId.equals(startEdgeId) || edgeId.equals(endEdgeId))
                continue;

            Edge edge = scene.getEdgeById(edgeId);
            if (edge != null) {
                edge.highlight(g, viewManager.getTransform(), Color.CYAN);
            }
        }

        // Highlight hovered edge in yellow (if not already selected)
        if (hoveredEdge != null) {
            String hoveredId = hoveredEdge.getNetworkEdge().id;
            if (!hoveredId.equals(startEdgeId) && !hoveredId.equals(endEdgeId)
                    && !route.contains(hoveredId)) {
                hoveredEdge.highlight(g, viewManager.getTransform(), Color.YELLOW);
            }
        }
    }

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
        g.fillText("🚗 ROUTE SELECTION MODE", x + 10, y + 20);

        // Instructions based on state
        g.setFill(Color.WHITE);
        if (vehicleAddPanel != null) {
            String startEdge = vehicleAddPanel.getStartEdge();
            String endEdge = vehicleAddPanel.getEndEdge();

            if (startEdge == null) {
                g.setFill(Color.LIMEGREEN);
                g.fillText("➤ Click START edge (shown in GREEN)", x + 10, y + 45);
            } else if (endEdge == null) {
                g.fillText("✓ Start: " + startEdge, x + 10, y + 40);
                g.setFill(Color.ORANGERED);
                g.fillText("➤ Click END edge (shown in RED)", x + 10, y + 60);
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

    public static void main(String[] args) {
        launch(args);
    }
}