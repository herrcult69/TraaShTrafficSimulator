import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

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
        ControlPanel controlPanel = new ControlPanel(runner, viewManager, dashboard);
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

        // Click detection (for future interaction features)
        canvas.setOnMouseClicked(e -> {
            Object clickedElement = scene.getElementAt(e.getX(), e.getY(), viewManager.getTransform());
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
                // } else if (clickedElement instanceof TrafficLight) {
                //     TrafficLight tl = (TrafficLight) clickedElement;
                //     String status = "";
                //     if (tl.isMainForcedControl()) {
                //         status += " Main: FORCED " + tl.getMainForcedState().toUpperCase();
                //     }
                //     if (tl.isTurnForcedControl()) {
                //         status += " Turn: FORCED " + tl.getTurnForcedState().toUpperCase();
                //     }
                //     System.out.println("Traffic Light: Junction " + tl.getJunctionId() + 
                //         ", Edge " + tl.getApproachEdgeId() + status);
                // }
                }
            }
        }
        );

        stage.setOnCloseRequest(e -> {
            runner.stop();
            exec.shutdownNow();
            Platform.exit();
        });
    }
    
    // private void showTrafficLightMenu(TrafficLight tl, double screenX, double screenY) {
    //     ContextMenu menu = new ContextMenu();
        
    //     // Main light controls
    //     MenuItem mainGreen = new MenuItem("Main: Force Green");
    //     mainGreen.setOnAction(e -> {
    //         tl.forceMainGreen();
    //         System.out.println("Forced MAIN GREEN: Junction " + tl.getJunctionId() + ", Edge " + tl.getApproachEdgeId());
    //     });
        
    //     MenuItem mainRed = new MenuItem("Main: Force Red");
    //     mainRed.setOnAction(e -> {
    //         tl.forceMainRed();
    //         System.out.println("Forced MAIN RED: Junction " + tl.getJunctionId() + ", Edge " + tl.getApproachEdgeId());
    //     });
        
    //     MenuItem mainRelease = new MenuItem("Main: Release Control");
    //     mainRelease.setOnAction(e -> {
    //         tl.releaseMainControl();
    //         System.out.println("Released MAIN control: Junction " + tl.getJunctionId() + ", Edge " + tl.getApproachEdgeId());
    //     });
    //     mainRelease.setDisable(!tl.isMainForcedControl());
        
    //     // Turn signal controls (only if there are turn links)
    //     if (!tl.getTurnLinkIndices().isEmpty()) {
    //         MenuItem turnGreen = new MenuItem("Turn: Force Green");
    //         turnGreen.setOnAction(e -> {
    //             tl.forceTurnGreen();
    //             System.out.println("Forced TURN GREEN: Junction " + tl.getJunctionId() + ", Edge " + tl.getApproachEdgeId());
    //         });
            
    //         MenuItem turnRed = new MenuItem("Turn: Force Red");
    //         turnRed.setOnAction(e -> {
    //             tl.forceTurnRed();
    //             System.out.println("Forced TURN RED: Junction " + tl.getJunctionId() + ", Edge " + tl.getApproachEdgeId());
    //         });
            
    //         MenuItem turnRelease = new MenuItem("Turn: Release Control");
    //         turnRelease.setOnAction(e -> {
    //             tl.releaseTurnControl();
    //             System.out.println("Released TURN control: Junction " + tl.getJunctionId() + ", Edge " + tl.getApproachEdgeId());
    //         });
    //         turnRelease.setDisable(!tl.isTurnForcedControl());
            
    //         menu.getItems().addAll(mainGreen, mainRed, mainRelease, 
    //                                new javafx.scene.control.SeparatorMenuItem(),
    //                                turnGreen, turnRed, turnRelease);
    //     } else {
    //         menu.getItems().addAll(mainGreen, mainRed, mainRelease);
    //     }
        
    //     // Auto-hide when clicking elsewhere
    //     menu.setAutoHide(true);
    //     canvas.setOnMousePressed(e -> {
    //         if (e.isPrimaryButtonDown()) {
    //             menu.hide();
    //             viewManager.startPan(e.getX(), e.getY());
    //         }
    //     });
        
    //     menu.show(canvas, screenX, screenY);
    // }

    /** Main rendering loop - called ~60 times per second */
    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        g.setFill(Color.rgb(26, 36, 47));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        scene.updateVehicles(runner.getVehiclePositions());
        scene.updateTrafficLights(runner.getTrafficLightData());
        scene.render(g, viewManager.getTransform());

        // Update dashboard at reduced frequency
        long currentTime = System.nanoTime();
        double timeSinceLastUpdate = (currentTime - lastDashboardUpdate) / 1_000_000_000.0;
        if (timeSinceLastUpdate >= DASHBOARD_UPDATE_INTERVAL) {
            updateDashboard();
            lastDashboardUpdate = currentTime;
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
            if (id.startsWith("car")) carCount++;
            else if (id.startsWith("truck")) truckCount++;
            else if (id.startsWith("bus")) busCount++;
            else if (id.startsWith("moto")) motoCount++;
            else if (id.startsWith("ambu")) emergencyCount++;

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