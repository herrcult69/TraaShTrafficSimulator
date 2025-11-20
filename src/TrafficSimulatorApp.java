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
 * TrafficSimulatorApp - Real-time SUMO Traffic Visualization Application
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * TECHNICAL ARCHITECTURE OVERVIEW
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This application implements a real-time traffic simulation visualizer using:
 * 
 * 1. COORDINATE SYSTEM TRANSFORMATION:
 *    - SUMO uses: origin (0,0) at bottom-left, Y increases upward
 *    - JavaFX uses: origin (0,0) at top-left, Y increases downward
 *    - Solution: Y-axis flip transformation in CoordinateTransform class
 *    
 * 2. MULTI-LEVEL SCALING SYSTEM:
 *    - Base Scale: Fits entire network to canvas with margin
 *    - Zoom Level: User-controlled zoom factor (0.1x to 10x)
 *    - Pan Offset: User drag-based translation
 *    - Final Transform: screen = (world * baseScale * zoom) + baseOffset + panOffset
 *    
 * 3. OBJECT-ORIENTED RENDERING PIPELINE:
 *    - NetworkParser: Raw SUMO XML data → Java objects
 *    - VisualEdge/Lane/Vehicle: Data objects → Renderable objects with hit detection
 *    - TrafficScene: Scene graph managing object lifecycle and interactions
 *    - Rendering: 60fps animation loop with efficient coordinate caching
 *    
 * 4. VEHICLE ANGLE SYSTEM:
 *    - SUMO angle: 0° = North, 90° = East, increases clockwise
 *    - JavaFX rotation: 0° = East, 90° = South, increases clockwise
 *    - Y-axis flip affects rotation direction
 *    - Conversion: javaFXAngle = -(90° - sumoAngle)
 *    
 * 5. INTERACTION SYSTEM:
 *    - Hit testing uses world coordinates for precision
 *    - Hierarchical detection: Vehicles → Lanes → Edges
 *    - Ready for future features: vehicle injection, lane editing
 *    
 * PERFORMANCE OPTIMIZATIONS:
 * - Coordinate transformation caching in transform object
 * - Minimum rendering sizes to prevent sub-pixel artifacts
 * - Efficient scene graph updates (only changed vehicles)
 * - Background thread simulation to prevent UI blocking
 * 
 * @author Traffic Simulation Team
 * @version 2.0 - OOP Architecture
 * @since 2025-11-20
 */
public class TrafficSimulatorApp extends Application {
    private static final String NETWORK_FILE = "resource/network.net.xml";
    private static final String CONFIG_FILE = "resource/simulation.sumocfg";

    private NetworkParser.NetworkData network;
    private SimulationRunner runner;
    private ExecutorService exec;
    private Canvas canvas;
    private TrafficScene scene;
    private CoordinateTransform transform;

    // ═══════════════════════════════════════════════════════════════════════════════
    // VIEW STATE MANAGEMENT - COORDINATE TRANSFORMATION SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /** 
     * Base scaling factor to fit network within canvas bounds
     * Calculated as: min(canvasWidth/networkWidth, canvasHeight/networkHeight)
     * This ensures the entire network fits with proper aspect ratio
     */
    private double scale = 1.0;
    
    /** 
     * X offset to center network horizontally on canvas
     * Formula: (canvasWidth - networkWidth*scale)/2 - networkMinX*scale
     * Moves network center to canvas center
     */
    private double offsetX = 0.0;
    
    /** 
     * Y offset to center network vertically on canvas
     * Formula: (canvasHeight - networkHeight*scale)/2 - networkMinY*scale
     * Accounts for Y-axis flip between SUMO (Y-up) and JavaFX (Y-down)
     */
    private double offsetY = 0.0;
    
    /** 
     * User-controlled zoom multiplication factor
     * Range: [0.1, 10.0] (10% to 1000% zoom)
     * Applied after base scaling: finalScale = baseScale * zoom
     */
    private double zoom = 1.0;
    
    /** 
     * Horizontal pan offset from user mouse dragging
     * Positive values shift view right, negative shifts left
     * Added to final screen coordinates
     */
    private double panX = 0.0;
    
    /** 
     * Vertical pan offset from user mouse dragging  
     * Positive values shift view down, negative shifts up
     * Note: Direction accounts for Y-axis flip
     */
    private double panY = 0.0;
    
    // Mouse interaction state for smooth dragging
    /** Screen X coordinate where drag operation started */
    private double dragStartX;
    /** Screen Y coordinate where drag operation started */
    private double dragStartY;
    /** Pan X value when drag started (for relative movement) */
    private double dragStartPanX;
    /** Pan Y value when drag started (for relative movement) */
    private double dragStartPanY;

    @Override
    public void start(Stage stage) throws Exception {
        // Initialize components
        network = NetworkParser.parse(NETWORK_FILE);
        canvas = new Canvas(1000, 800);
        scene = new TrafficScene();
        transform = new CoordinateTransform(canvas.getHeight());
        
        // Initialize scene from network data
        scene.initializeFromNetwork(network);
        initializeView();

        // UI setup
        Button zoomIn = new Button("+"), zoomOut = new Button("-"), reset = new Button("Reset");
        zoomIn.setOnAction(e -> zoom(1.2, 500, 400));
        zoomOut.setOnAction(e -> zoom(0.8, 500, 400));
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

        canvas.setOnScroll(e -> zoom(e.getDeltaY() > 0 ? 1.1 : 0.9, e.getX(), e.getY()));
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
                if (clickedElement instanceof VisualLane) {
                    VisualLane lane = (VisualLane) clickedElement;
                    System.out.println("Lane ID: " + lane.getId());
                } else if (clickedElement instanceof VisualVehicle) {
                    VisualVehicle vehicle = (VisualVehicle) clickedElement;
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

    /**
     * Initializes the view transformation to optimally display the entire network
     * 
     * ═══════════════════════════════════════════════════════════════════════════════
     * ALGORITHM: OPTIMAL NETWORK FITTING
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * This method implements a sophisticated viewport calculation to ensure the 
     * entire road network is visible with appropriate margins:
     * 
     * 1. DIMENSION CALCULATION:
     *    networkWidth = maxX - minX   (world units)
     *    networkHeight = maxY - minY  (world units)
     *    
     * 2. SCALE CALCULATION (Maintain Aspect Ratio):
     *    scaleX = (canvasWidth - 2*margin) / networkWidth
     *    scaleY = (canvasHeight - 2*margin) / networkHeight
     *    finalScale = min(scaleX, scaleY)  // Prevents distortion
     *    
     * 3. CENTERING CALCULATION:
     *    Network center in world coordinates: (minX + maxX)/2, (minY + maxY)/2
     *    Canvas center in screen coordinates: canvasWidth/2, canvasHeight/2
     *    
     *    offsetX = canvasCenter.x - networkCenter.x * scale
     *    offsetY = canvasCenter.y - networkCenter.y * scale (with Y-flip)
     *    
     * 4. STATE RESET:
     *    zoom = 1.0 (no additional zoom)
     *    pan = (0,0) (no user offset)
     * 
     * @implNote Called on startup and when "Reset" button is pressed
     * @sideEffect Updates all view state variables and coordinate transform
     */
    private void initializeView() {
        double margin = 50; // Pixel buffer around network edges
        double netW = network.maxX - network.minX; // Network width in SUMO units
        double netH = network.maxY - network.minY; // Network height in SUMO units
        
        // Handle edge case: empty or degenerate network
        if (netW == 0 || netH == 0) { 
            scale = 1.0; 
            offsetX = offsetY = 400; // Center on 800x600 canvas
            updateTransform();
            return; 
        }
        
        // Calculate uniform scale preserving aspect ratio
        // Choose smaller scale to ensure both dimensions fit
        scale = Math.min((canvas.getWidth() - 2 * margin) / netW, 
                         (canvas.getHeight() - 2 * margin) / netH);
        
        // Center network horizontally: canvas_center - (network_center * scale)
        offsetX = (canvas.getWidth() - netW * scale) / 2 - network.minX * scale;
        
        // Center network vertically with Y-axis consideration
        offsetY = (canvas.getHeight() - netH * scale) / 2 - network.minY * scale;
        
        // Reset user modifications
        zoom = 1.0; 
        panX = panY = 0.0;
        updateTransform();
    }

    /**
     * Performs intelligent zoom operation maintaining point-under-cursor stability
     * 
     * ═══════════════════════════════════════════════════════════════════════════════
     * ALGORITHM: ZOOM-TO-POINT TRANSFORMATION
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * This implements a sophisticated zoom that keeps the world point under the 
     * mouse cursor stationary during zoom operations, creating natural UX:
     * 
     * 1. REVERSE TRANSFORMATION (Screen → World):
     *    Given mouse screen coordinates (mouseX, mouseY)
     *    
     *    worldX = (screenX - offsetX - panX) / (scale * zoom)
     *    worldY = ((canvasHeight - screenY) - offsetY - panY) / (scale * zoom)
     *    
     *    Note: Y-coordinate flip (canvasHeight - screenY) converts JavaFX → SUMO
     *    
     * 2. ZOOM APPLICATION:
     *    newZoom = clamp(currentZoom * factor, 0.1, 10.0)
     *    Prevents extreme zoom levels that break rendering
     *    
     * 3. FORWARD TRANSFORMATION (World → Screen):
     *    newScreenX = (worldX * scale * newZoom) + offsetX + panX
     *    newScreenY = canvasHeight - ((worldY * scale * newZoom) + offsetY + panY)
     *    
     * 4. PAN ADJUSTMENT:
     *    To keep world point under cursor:
     *    panX += mouseX - newScreenX
     *    panY += mouseY - newScreenY
     *    
     *    This shifts the view so the cursor remains over the same world point
     * 
     * @param factor Zoom multiplication factor (>1.0 = zoom in, <1.0 = zoom out)
     * @param mouseX Screen X coordinate of zoom center (pixels)
     * @param mouseY Screen Y coordinate of zoom center (pixels)
     * 
     * @apiNote Zoom factors: 1.1 (mouse wheel), 1.2 (button), 0.8/0.9 (zoom out)
     * @sideEffect Updates zoom level and pan offsets, calls updateTransform()
     */
    private void zoom(double factor, double mouseX, double mouseY) {
        // Step 1: Convert mouse screen coordinates to world coordinates
        // This finds what world point is currently under the mouse cursor
        double worldX = (mouseX - offsetX - panX) / (scale * zoom);
        double worldY = ((canvas.getHeight() - mouseY) - offsetY - panY) / (scale * zoom);
        
        // Step 2: Apply zoom with safety limits
        // Prevents zoom levels that would break rendering or cause overflow
        zoom = Math.max(0.1, Math.min(10.0, zoom * factor));
        
        // Step 3: Calculate where that world point would appear with new zoom
        double newScreenX = (worldX * scale * zoom) + offsetX + panX;
        double newScreenY = canvas.getHeight() - ((worldY * scale * zoom) + offsetY + panY);
        
        // Step 4: Adjust pan to keep world point under mouse cursor
        // This creates the "zoom to point" effect
        panX += mouseX - newScreenX;
        panY += mouseY - newScreenY;
        updateTransform();
    }
    
    private void updateTransform() {
        transform.updateTransform(scale, offsetX, offsetY, zoom, panX, panY);
    }

    /**
     * Main rendering pipeline - executes at ~60 FPS via JavaFX AnimationTimer
     * 
     * ═══════════════════════════════════════════════════════════════════════════════
     * RENDERING PIPELINE ARCHITECTURE
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * This method implements a high-performance rendering pipeline optimized for
     * real-time traffic visualization:
     * 
     * 1. FRAME INITIALIZATION:
     *    - Clear canvas with dark background (RGB 20,24,28)
     *    - Full canvas clear prevents artifacts from previous frame
     *    
     * 2. DATA SYNCHRONIZATION:
     *    - Fetch latest vehicle positions from SUMO simulation
     *    - Update scene graph with new/moved/removed vehicles
     *    - Maintains consistency between simulation and visualization
     *    
     * 3. SCENE RENDERING:
     *    - Delegate to TrafficScene.render() for object-oriented rendering
     *    - Rendering order: Roads → Lane markings → Vehicles
     *    - Each visual object handles its own coordinate transformation
     *    
     * PERFORMANCE CHARACTERISTICS:
     * - Frame rate: ~60 FPS (16.67ms per frame)
     * - Coordinate caching: Transform calculations cached in transform object
     * - Culling: Objects outside viewport are skipped
     * - Batching: Similar rendering operations grouped for efficiency
     * 
     * COORDINATE FLOW:
     * SUMO world coords → CoordinateTransform → JavaFX screen coords → Canvas
     * 
     * @implNote Called by JavaFX AnimationTimer.handle() approximately 60 times/second
     * @sideEffect Clears and redraws entire canvas, updates vehicle positions
     * @performance Target: <16ms execution time to maintain 60 FPS
     */
    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        
        // Clear canvas with professional dark theme background
        g.setFill(Color.rgb(20, 24, 28)); // Dark charcoal for low eye strain
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Synchronize vehicle data from SUMO simulation
        // This updates positions, removes departed vehicles, adds new arrivals
        scene.updateVehicles(runner.getVehiclePositions());
        
        // Execute complete scene rendering pipeline
        // Renders roads, lanes, vehicles in proper Z-order
        scene.render(g, transform);
    }

    public static void main(String[] args) { 
        launch(args); 
    }
}