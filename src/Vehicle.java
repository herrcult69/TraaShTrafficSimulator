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
        updateBounds();
    }
    
    private void updateBounds() {
        double halfWidth = width / 2;
        // Bounds with head at (worldX, worldY) extending backward
        bounds = new Rectangle2D(
            worldX - length, worldY - halfWidth,
            length, width
        );
    }
    
    public boolean contains(double screenX, double screenY, CoordinateTransform transform) {
        double worldX = transform.screenToWorldX(screenX);
        double worldY = transform.screenToWorldY(screenY);
        return bounds.contains(worldX, worldY);
    }
    
    public void render(GraphicsContext g, CoordinateTransform transform) {
        double screenX = transform.worldToScreenX(worldX);
        double screenY = transform.worldToScreenY(worldY);
        double screenLength = Math.max(transform.worldToScreenSize(length), 6);
        double screenWidth = Math.max(transform.worldToScreenSize(width), 3);
        
        g.save();
        g.translate(screenX, screenY);
        g.rotate(angle);
        
        // Draw vehicle with head at origin (worldX, worldY), extending backward
        g.setFill(getVehicleColor());
        g.fillRect(-screenLength, -screenWidth/2, screenLength, screenWidth);
        
        g.setStroke(Color.BLACK);
        g.setLineWidth(1.5);
        g.strokeRect(-screenLength, -screenWidth/2, screenLength, screenWidth);
        
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
    public String getId() { return id; }
    public String getType() { return type; }
    public double getWorldX() { return worldX; }
    public double getWorldY() { return worldY; }
    public double getAngle() { return angle; }
    public Lane getCurrentLane() { return currentLane; }
    public Rectangle2D getBounds() { return bounds; }
}