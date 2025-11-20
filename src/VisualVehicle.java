import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;

public class VisualVehicle {
    private String id;
    private String type;
    private double worldX, worldY;
    private double angle;
    private double length, width;
    private VisualLane currentLane;
    private Rectangle2D bounds;
    
    public VisualVehicle(String id, double worldX, double worldY, double angle) {
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
            length = 12.0;
            width = 2.5;
        } else if (id.startsWith("bus")) {
            type = "bus";
            length = 12.0;
            width = 2.5;
        } else if (id.startsWith("moto")) {
            type = "motorcycle";
            length = 2.0;
            width = 0.8;
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
        double halfLength = length / 2;
        double halfWidth = width / 2;
        bounds = new Rectangle2D(
            worldX - halfLength, worldY - halfWidth,
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
        
        // Draw vehicle shape based on type
        g.setFill(getVehicleColor());
        g.fillRect(-screenLength/2, -screenWidth/2, screenLength, screenWidth);
        
        g.setStroke(Color.BLACK);
        g.setLineWidth(1);
        g.strokeRect(-screenLength/2, -screenWidth/2, screenLength, screenWidth);
        
        g.restore();
    }
    
    private Color getVehicleColor() {
        return switch (type) {
            case "car" -> Color.rgb(220, 70, 70);
            case "truck" -> Color.rgb(70, 120, 230);
            case "bus" -> Color.rgb(80, 200, 120);
            case "motorcycle" -> Color.rgb(230, 230, 230);
            default -> Color.WHITE;
        };
    }
    
    public void setCurrentLane(VisualLane lane) {
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
    public VisualLane getCurrentLane() { return currentLane; }
    public Rectangle2D getBounds() { return bounds; }
}