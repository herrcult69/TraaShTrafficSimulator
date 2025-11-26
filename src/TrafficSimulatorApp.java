import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Main JavaFX application for real-time SUMO traffic visualization.
 * Manages application lifecycle, window setup, and 60 FPS rendering loop.
 * Automatically centers and scales the network to fit the canvas.
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

    @Override
    public void start(Stage stage) throws Exception {
        // Initialize components
        network = NetworkParser.parse(NETWORK_FILE);
        canvas = new Canvas();
        scene = new TrafficManager();
        transform = new CoordinateTransform(900);

        // Initialize scene from network data
        scene.initializeFromNetwork(network);

        // Create mockup dashboard
        DashBoard dashboard = new DashBoard();
        
        // Start simulation
        runner = new SimulationRunner(CONFIG_FILE, true);
        exec = Executors.newSingleThreadExecutor();
        exec.submit(runner);

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setRight(dashboard.getScrollPane());

        Scene mainScene = new Scene(root, 1400, 900);
        stage.setScene(mainScene);
        stage.setTitle("Traffic Simulator - Milestone 1");
        
        // Make canvas fill the available space
        canvas.widthProperty().bind(root.widthProperty().subtract(300));
        canvas.heightProperty().bind(root.heightProperty());
        
        // Simple view initialization
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0 && canvas.getWidth() > 0) {
                transform = new CoordinateTransform(newVal.doubleValue());
                resetView();
            }
        });
        
        stage.show();
        
        // Initialize view after stage is shown
        Platform.runLater(() -> {
            if (canvas.getWidth() > 0 && canvas.getHeight() > 0) {
                resetView();
            }
        });

        // Animation (no interactions for milestone 1 - just display)
        new javafx.animation.AnimationTimer() {
            public void handle(long now) {
                draw();
            }
        }.start();

        stage.setOnCloseRequest(e -> {
            runner.stop();
            exec.shutdownNow();
            Platform.exit();
        });
    }

    /** Fit network to canvas with simple scaling and center it */
    private void resetView() {
        double margin = 50;
        double scaleX = (canvas.getWidth() - 2 * margin) / (network.maxX - network.minX);
        double scaleY = (canvas.getHeight() - 2 * margin) / (network.maxY - network.minY);
        double scale = Math.min(scaleX, scaleY);
        
        // Calculate scaled network dimensions
        double scaledWidth = (network.maxX - network.minX) * scale;
        double scaledHeight = (network.maxY - network.minY) * scale;
        
        // Center the network on canvas
        double offsetX = (canvas.getWidth() - scaledWidth) / 2 - network.minX * scale;
        double offsetY = (canvas.getHeight() - scaledHeight) / 2 - network.minY * scale;
        
        transform.updateTransform(scale, offsetX, offsetY);
    }

    /** Main rendering loop - called ~60 times per second */
    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        g.setFill(Color.rgb(0, 14, 36));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        scene.updateVehicles(runner.getVehiclePositions());
        scene.render(g, transform);
    }

    public static void main(String[] args) {
        launch(args);
    }
}