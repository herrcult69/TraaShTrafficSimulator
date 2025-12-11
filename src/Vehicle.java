import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;

/**
 * Represents a single vehicle in the traffic simulation.
 * 
 * <p>Each vehicle has:
 * <ul>
 *   <li>Unique identifier determining its type (car, truck, bus, motorcycle, emergency)</li>
 *   <li>Position and orientation in world coordinates</li>
 *   <li>Type-specific dimensions and visual appearance</li>
 *   <li>Signal state (turn signals, brake lights, emergency flashers)</li>
 * </ul>
 * 
 * <p>The vehicle type is automatically determined from its ID prefix:
 * <ul>
 *   <li>"car" → Passenger car (4.5m × 1.8m)</li>
 *   <li>"truck" → Truck (8.0m × 2.5m)</li>
 *   <li>"bus" → Bus (10.0m × 2.5m)</li>
 *   <li>"moto" → Motorcycle (2.0m × 0.8m)</li>
 *   <li>"ambu" → Emergency vehicle (6.0m × 2.5m)</li>
 * </ul>
 *
 * @author M A T^2 H Team
 * @version 2.0 
 * @see TrafficManager
 * @see SimulationRunner
 */
public class Vehicle {
    private String id;
    private String type;
    private double worldX, worldY;
    private double angle;
    private double length, width;
    private Rectangle2D bounds;
    private int signals;

    /**
     * Constructs a new vehicle with the specified position and orientation.
     * The vehicle type and dimensions are automatically determined from the ID prefix.
     * 
     * @param id The unique vehicle identifier (prefix determines type)
     * @param worldX The X coordinate in world space (meters)
     * @param worldY The Y coordinate in world space (meters)
     * @param angle The orientation angle in degrees (0 = east, 90 = north)
     */
    public Vehicle(String id, double worldX, double worldY, double angle) {
        this.id = id;
        this.worldX = worldX;
        this.worldY = worldY;
        this.angle = angle;

        // Determine vehicle type and dimensions from ID
        determineTypeFromId();
        updateBounds();
    }

    /**
     * Determines the vehicle type and sets appropriate dimensions based on the ID prefix.
     */
    private void determineTypeFromId() {
        if (id.startsWith("car")) {
            type = "car";
            length = 4.5;
            width = 1.8;
        } else if (id.startsWith("truck")) {
            type = "truck";
            length = 8.0;
            width = 2.5;
        } else if (id.startsWith("bus")) {
            type = "bus";
            length = 10.0;
            width = 2.5;
        } else if (id.startsWith("moto")) {
            type = "motorcycle";
            length = 2.0;
            width = 0.8;
        } else if (id.startsWith("ambu")) {
            type = "emergency";
            length = 6.0;
            width = 2.5;
        } else {
            type = "unknown";
            length = 4.5;
            width = 1.8;
        }
    }

    /**
     * Updates the vehicle's position, angle, and signal state from SUMO data.
     * 
     * @param sumoData Array containing [x, y, angle, signals] where:
     *                 x, y are world coordinates in meters,
     *                 angle is in degrees (SUMO format),
     *                 signals is a bit field for turn signals and brake lights
     */
    public void updatePosition(double[] sumoData) {
        this.worldX = sumoData[0];
        this.worldY = sumoData[1];
        if (sumoData.length > 2) {
            this.angle = -(90.0 - sumoData[2]); // Fixed angle calculation
        }
        if (sumoData.length > 3) {
            this.signals = (int) sumoData[3];
        }
        updateBounds();
    }

    /**
     * Updates the vehicle's bounding rectangle based on current position and dimensions.
     */
    private void updateBounds() {
        double halfWidth = width / 2;
        // Bounds with head at (worldX, worldY) extending backward
        bounds = new Rectangle2D(
                worldX - length, worldY - halfWidth,
                length, width);
    }

    /**
     * Checks if a screen point falls within the vehicle's clickable area.
     * Uses circular hit detection for better user interaction.
     * 
     * @param screenX The X coordinate in screen space
     * @param screenY The Y coordinate in screen space
     * @param transform The coordinate transformation
     * @return true if the point is within the vehicle's clickable radius
     */
    public boolean contains(double screenX, double screenY, CoordinateTransform transform) {
        double clickWorldX = transform.screenToWorldX(screenX);
        double clickWorldY = transform.screenToWorldY(screenY);

        double dx = clickWorldX - worldX;
        double dy = clickWorldY - worldY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // The circle should be a little bit smaller than the width
        double radius = width * .90 ;
        return distance <= radius;
    }

    /**
     * Draws a dashed rectangle outline around the vehicle when selected or hovered.
     * 
     * @param g The graphics context to draw on
     * @param transform The coordinate transformation
     * @param color The highlight color
     */
    public void highlight(GraphicsContext g, CoordinateTransform transform, Color color) {
        double screenX = transform.worldToScreenX(worldX);
        double screenY = transform.worldToScreenY(worldY);
        double screenLength = Math.max(transform.worldToScreenSize(length), 6);
        double screenWidth = Math.max(transform.worldToScreenSize(width), 3);

        g.save();
        g.translate(screenX, screenY);
        g.rotate(angle);


        // Dashed outline
        g.setStroke(color);
        g.setLineWidth(2.5);
        g.setLineDashes(6, 3);
        g.strokeRect(-screenLength, -screenWidth / 2, screenLength, screenWidth);
        g.setLineDashes(null);

        g.restore();
    }

    /**
     * Renders the vehicle with proper body, windshield, headlights, and signal indicators.
     * The vehicle is drawn with its front at (worldX, worldY) extending backward.
     * 
     * @param g The graphics context to draw on
     * @param transform The coordinate transformation
     */
    public void render(GraphicsContext g, CoordinateTransform transform) {
        double screenX = transform.worldToScreenX(worldX);
        double screenY = transform.worldToScreenY(worldY);
        double screenLength = Math.max(transform.worldToScreenSize(length), 6);
        double screenWidth = Math.max(transform.worldToScreenSize(width), 3);

        g.save();
        g.translate(screenX, screenY);
        g.rotate(angle);

        // Draw vehicle body
        g.setFill(getVehicleColor());
        g.fillRect(-screenLength, -screenWidth / 2, screenLength, screenWidth);

        g.setStroke(Color.BLACK);
        g.setLineWidth(1.5);
        g.strokeRect(-screenLength, -screenWidth / 2, screenLength, screenWidth);

        // Draw windshield (approx 20% from front, 20% length)
        double windshieldStart = -screenLength * 0.2;
        double windshieldLength = screenLength * 0.2;
        double windshieldWidth = screenWidth * 0.8;
        g.setFill(Color.LIGHTBLUE);
        g.fillRect(windshieldStart - windshieldLength, -windshieldWidth / 2, windshieldLength, windshieldWidth);

        // Draw headlights
        g.setFill(Color.YELLOW.deriveColor(0, 1, 1, 0.6));
        // Left headlight
        g.fillOval(-screenLength * 0.05, -screenWidth / 2 + screenWidth * 0.1, screenLength * 0.1, screenWidth * 0.3);
        // Right headlight
        g.fillOval(-screenLength * 0.05, screenWidth / 2 - screenWidth * 0.4, screenLength * 0.1, screenWidth * 0.3);

        // Draw signals
        // Right turn: bit 0 (1)
        // Left turn: bit 1 (2)
        // Emergency: bit 2 (4) -> implies both
        // Brake: bit 3 (8)

        boolean rightSignal = (signals & 1) != 0 || (signals & 4) != 0;
        boolean leftSignal = (signals & 2) != 0 || (signals & 4) != 0;
        boolean brakeSignal = (signals & 8) != 0;

        if (brakeSignal) {
            g.setFill(Color.RED);
            // Back lights
            g.fillRect(-screenLength, -screenWidth / 2, screenLength * 0.05, screenWidth);
        }

        if (rightSignal) {
            g.setFill(Color.ORANGE);
            // Front Right
            g.fillOval(0, screenWidth / 2 - screenWidth * 0.2, screenLength * 0.1, screenWidth * 0.2);
            // Back Right
            g.fillOval(-screenLength, screenWidth / 2 - screenWidth * 0.2, screenLength * 0.1, screenWidth * 0.2);
        }

        if (leftSignal) {
            g.setFill(Color.ORANGE);
            // Front Left
            g.fillOval(0, -screenWidth / 2, screenLength * 0.1, screenWidth * 0.2);
            // Back Left
            g.fillOval(-screenLength, -screenWidth / 2, screenLength * 0.1, screenWidth * 0.2);
        }

        g.restore();
    }

    /**
     * Returns the color associated with this vehicle type.
     * 
     * @return The vehicle's color based on its type
     */
    private Color getVehicleColor() {
        return switch (type) {
            case "car" -> Color.rgb(220, 60, 115);
            case "truck" -> Color.rgb(50, 120, 230);
            case "bus" -> Color.rgb(80, 200, 120);
            case "motorcycle" -> Color.rgb(240, 140, 10);
            case "emergency" -> Color.rgb(225, 205, 205);
            default -> Color.PURPLE;
        };
    }

    // Getters
    /**
     * Returns the unique vehicle identifier.
     * 
     * @return The vehicle ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the vehicle type (car, truck, bus, motorcycle, emergency, or unknown).
     * 
     * @return The vehicle type as a string
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the vehicle's current X coordinate in world space.
     * 
     * @return The X coordinate in meters
     */
    public double getWorldX() {
        return worldX;
    }

    /**
     * Returns the vehicle's current Y coordinate in world space.
     * 
     * @return The Y coordinate in meters
     */
    public double getWorldY() {
        return worldY;
    }

    /**
     * Returns the vehicle's current orientation angle.
     * 
     * @return The angle in degrees
     */
    public double getAngle() {
        return angle;
    }

    /**
     * Returns the vehicle's bounding rectangle in world coordinates.
     * 
     * @return The bounding rectangle
     */
    public Rectangle2D getBounds() {
        return bounds;
    }
}