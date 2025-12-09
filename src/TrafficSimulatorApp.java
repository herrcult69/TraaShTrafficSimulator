import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;

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
        runner = new SimulationRunner(CONFIG_FILE, true);

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
        ControlPanel controlPanel = new ControlPanel(runner, viewManager, dashboard, scene);
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
        canvas.setOnMouseMoved(
                e -> hoveredElement = scene.getElementAt(e.getX(), e.getY(), viewManager.getTransform()));

        // Click detection
        canvas.setOnMouseClicked(e -> {
            Object clickedElement = scene.getElementAt(e.getX(), e.getY(), viewManager.getTransform());
            selectedElement = clickedElement;

            if (clickedElement instanceof TrafficLight) {
                TrafficLight tl = (TrafficLight) clickedElement;
                System.out.println("Clicked: Traffic Light - Junction: " + tl.getJunctionId() + 
                        " Signals: " + tl.getSignals().size());
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

        // Draw info box
        if (selectedElement != null) {
            drawInfoBox(g, selectedElement);
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

    public static void main(String[] args) {
        launch(args);
    }
}