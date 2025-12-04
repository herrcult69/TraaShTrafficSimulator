import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;

public class Vehicle {
    private String id;
    private String type;
    private double worldX, worldY;
    private double angle;
    private double length, width;
    private Lane currentLane;
    private Rectangle2D bounds;
    private int signals;

    public Vehicle(String id, double worldX, double worldY, double angle) {
        this.id = id;
        this.worldX = worldX;
        this.worldY = worldY;
        this.angle = angle;

        // Determine vehicle type and dimensions from ID
        determineTypeFromId();
        updateBounds();
    }

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

    private void updateBounds() {
        double halfWidth = width / 2;
        // Bounds with head at (worldX, worldY) extending backward
        bounds = new Rectangle2D(
                worldX - length, worldY - halfWidth,
                length, width);
    }

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

    private Color getVehicleColor() {
        return switch (type) {
            case "car" -> Color.rgb(220, 70, 70);
            case "truck" -> Color.rgb(70, 120, 230);
            case "bus" -> Color.rgb(80, 200, 120);
            case "motorcycle" -> Color.rgb(243, 141, 9);
            case "emergency" -> Color.rgb(225, 206, 206);
            default -> Color.PURPLE;
        };
    }

    public void setCurrentLane(Lane lane) {
        if (currentLane != null) {
            currentLane.removeVehicle(this);
        }
        this.currentLane = lane;
        if (lane != null) {
            lane.addVehicle(this);
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public double getWorldX() {
        return worldX;
    }

    public double getWorldY() {
        return worldY;
    }

    public double getAngle() {
        return angle;
    }

    public Lane getCurrentLane() {
        return currentLane;
    }

    public Rectangle2D getBounds() {
        return bounds;
    }
}