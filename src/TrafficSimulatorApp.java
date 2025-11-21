import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

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

    // View transformation state
    private double scale = 1.0;    // Base scaling to fit network
    private double offsetX = 0.0;  // Center network horizontally
    private double offsetY = 0.0;  // Center network vertically
    private double zoom = 1.0;     // User zoom level (0.1 - 10.0)
    private double panX = 0.0;     // Horizontal pan offset
    private double panY = 0.0;     // Vertical pan offset
    
    // Mouse drag state
    private double dragStartX, dragStartY, dragStartPanX, dragStartPanY;

    @Override
    public void start(Stage stage) throws Exception {
        // Initialize components
        network = NetworkParser.parse(NETWORK_FILE);
        canvas = new Canvas(1000, 800);
        scene = new TrafficManager();
        transform = new CoordinateTransform(canvas.getHeight());
        
        // Initialize scene from network data
        scene.initializeFromNetwork(network);
        initializeView();

        // UI setup
        Button zoomIn = new Button("+"), zoomOut = new Button("-"), reset = new Button("Reset");
        zoomIn.setOnAction(e -> zoomToCenter(1.2));
        zoomOut.setOnAction(e -> zoomToCenter(0.8));
        reset.setOnAction(e -> initializeView());

        HBox controls = new HBox(10, zoomOut, zoomIn, reset);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10));

        stage.setScene(new Scene(new BorderPane(canvas, null, null, controls, null)));
        stage.setTitle("Traffic Simulator - OOP Architecture");
        stage.show();

        // Start simulation
        runner = new SimulationRunner(CONFIG_FILE, false);
        exec = Executors.newSingleThreadExecutor();
        exec.submit(runner);

        // Animation and interactions
        new javafx.animation.AnimationTimer() {
            public void handle(long now) { draw(); }
        }.start();

        canvas.setOnScroll(e -> zoomToPoint(e.getDeltaY() > 0 ? 1.1 : 0.9, e.getX(), e.getY()));
        canvas.setOnMousePressed(e -> { 
            dragStartX = e.getX(); 
            dragStartY = e.getY(); 
            dragStartPanX = panX; 
            dragStartPanY = panY; 
        });
        canvas.setOnMouseDragged(e -> { 
            panX = dragStartPanX + e.getX() - dragStartX; 
            panY = dragStartPanY - (e.getY() - dragStartY);
            updateTransform();
        });
        
        // Click detection (for future interaction features)
        canvas.setOnMouseClicked(e -> {
            Object clickedElement = scene.getElementAt(e.getX(), e.getY(), transform);
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
        });
        
        stage.setOnCloseRequest(e -> { 
            runner.stop(); 
            exec.shutdownNow(); 
            Platform.exit(); 
        });
    }

    /** Fit entire network to canvas with margins and reset zoom/pan */
    private void initializeView() {
        double margin = 50;
        double netW = network.maxX - network.minX;
        double netH = network.maxY - network.minY;
        
        if (netW == 0 || netH == 0) { 
            scale = 1.0; 
            offsetX = offsetY = 400;
            updateTransform();
            return; 
        }
        
        // Calculate scale to fit network with margins, maintain aspect ratio
        scale = Math.min((canvas.getWidth() - 2 * margin) / netW, 
                         (canvas.getHeight() - 2 * margin) / netH);
        
        // Center network on canvas
        offsetX = (canvas.getWidth() - netW * scale) / 2 - network.minX * scale;
        offsetY = (canvas.getHeight() - netH * scale) / 2 - network.minY * scale;
        
        // Reset user modifications
        zoom = 1.0; 
        panX = panY = 0.0;
        updateTransform();
    }

    /** Zoom to screen center (for buttons) */
    private void zoomToCenter(double factor) {
        double centerX = canvas.getWidth() / 2.0;
        double centerY = canvas.getHeight() / 2.0;
        zoomToPoint(factor, centerX, centerY);
    }

    /** Zoom to specific point (for scroll wheel at cursor) */
    private void zoomToPoint(double factor, double targetX, double targetY) {
        // Calculate current world coordinates at target point (simplified approach)
        double oldScale = scale * zoom;
        double worldX = (targetX - offsetX - panX) / oldScale;
        double worldY = (targetY - offsetY - panY) / oldScale;
        
        // Apply new zoom level
        zoom = Math.max(0.1, Math.min(10.0, zoom * factor));
        double newScale = scale * zoom;
        
        // Reposition offsets so the world point stays under the cursor
        panX = targetX - offsetX - worldX * newScale;
        panY = targetY - offsetY - worldY * newScale;
        
        updateTransform();
    }
    
    private void updateTransform() {
        transform.updateTransform(scale, offsetX, offsetY, zoom, panX, panY);
    }

    /** Main rendering loop - called ~60 times per second */
    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        
        g.setFill(Color.rgb(20, 24, 28));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        scene.updateVehicles(runner.getVehiclePositions());
        scene.render(g, transform);
    }

    public static void main(String[] args) { 
        launch(args); 
    }
}