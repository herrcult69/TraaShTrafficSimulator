import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class VisualLane {
    private String id;
    private VisualEdge parentEdge;
    private double x1, y1, x2, y2;  // Lane center line in world coordinates
    private double width;           // Lane width in world units (3.2m)
    private int index;              // Lane index (0, 1, 2...)
    private double offsetFromCenter; // Distance from road center line
    private List<VisualVehicle> vehicles;
    
    public VisualLane(String id, VisualEdge parentEdge, double width, int index, double offsetFromCenter) {
        this.id = id;
        this.parentEdge = parentEdge;
        this.width = width;
        this.index = index;
        this.offsetFromCenter = offsetFromCenter;
        this.vehicles = new ArrayList<>();
        calculateCenterLine();
    }
    
    private void calculateCenterLine() {
        double dx = parentEdge.getToX() - parentEdge.getFromX();
        double dy = parentEdge.getToY() - parentEdge.getFromY();
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length > 0.001) {
            double perpX = dy / length;
            double perpY = -dx / length;
            
            this.x1 = parentEdge.getFromX() + offsetFromCenter * perpX;
            this.y1 = parentEdge.getFromY() + offsetFromCenter * perpY;
            this.x2 = parentEdge.getToX() + offsetFromCenter * perpX;
            this.y2 = parentEdge.getToY() + offsetFromCenter * perpY;
        }
    }
    
    public boolean contains(double screenX, double screenY, CoordinateTransform transform) {
        // Convert screen coordinates to world coordinates
        double worldX = transform.screenToWorldX(screenX);
        double worldY = transform.screenToWorldY(screenY);
        
        // Calculate distance from point to lane center line
        double A = y2 - y1;
        double B = x1 - x2;
        double C = x2 * y1 - x1 * y2;
        double distance = Math.abs(A * worldX + B * worldY + C) / Math.sqrt(A * A + B * B);
        
        // Check if point is within lane width and along the lane length
        double laneWidthWorld = width / 2.0;
        return distance <= laneWidthWorld && isAlongLane(worldX, worldY);
    }
    
    private boolean isAlongLane(double worldX, double worldY) {
        // Check if point projection falls within lane start/end bounds
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = dx * dx + dy * dy;
        
        if (length < 0.001) return false;
        
        double t = ((worldX - x1) * dx + (worldY - y1) * dy) / length;
        return t >= -0.1 && t <= 1.1; // Small tolerance
    }
    
    public void addVehicle(VisualVehicle vehicle) {
        vehicles.add(vehicle);
    }
    
    public void removeVehicle(VisualVehicle vehicle) {
        vehicles.remove(vehicle);
    }
    
    public void render(GraphicsContext g, CoordinateTransform transform, boolean highlight) {
        // Lane markings are rendered by parent edge
        // Individual lane highlighting can be done here
        if (highlight) {
            g.setStroke(Color.YELLOW);
            g.setLineWidth(4);
            g.strokeLine(
                transform.worldToScreenX(x1), transform.worldToScreenY(y1),
                transform.worldToScreenX(x2), transform.worldToScreenY(y2)
            );
        }
    }
    
    // Getters
    public String getId() { return id; }
    public VisualEdge getParentEdge() { return parentEdge; }
    public double getWidth() { return width; }
    public int getIndex() { return index; }
    public List<VisualVehicle> getVehicles() { return vehicles; }
    public double getCenterX1() { return x1; }
    public double getCenterY1() { return y1; }
    public double getCenterX2() { return x2; }
    public double getCenterY2() { return y2; }
}