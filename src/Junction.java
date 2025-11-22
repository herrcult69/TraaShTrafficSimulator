import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a road junction/intersection with proper geometry rendering.
 */
public class Junction {
    private NetworkParser.Junction networkJunction;
    private String id;
    private double x, y;  // Center position in world coordinates
    private String type;
    private List<Point2D> shape;  // Junction boundary polygon
    private Rectangle2D bounds;
    
    public Junction(NetworkParser.Junction networkJunction) {
        this.networkJunction = networkJunction;
        this.id = networkJunction.id;
        this.x = networkJunction.x;
        this.y = networkJunction.y;
        this.type = networkJunction.type != null ? networkJunction.type : "priority";
        this.shape = new ArrayList<>();
        
        // Parse shape if available
        if (networkJunction.shape != null && !networkJunction.shape.isEmpty()) {
            parseShape(networkJunction.shape);
        }
        
        calculateBounds();
    }
    
    private void parseShape(String shapeStr) {
        // SUMO shape format: "x1,y1 x2,y2 x3,y3 ..."
        String[] points = shapeStr.trim().split("\\s+");
        for (String point : points) {
            String[] coords = point.split(",");
            if (coords.length == 2) {
                try {
                    double px = Double.parseDouble(coords[0]);
                    double py = Double.parseDouble(coords[1]);
                    shape.add(new Point2D(px, py));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid shape coordinate: " + point);
                }
            }
        }
    }
    
    private void calculateBounds() {
        if (shape.isEmpty()) {
            // Default circular boundary
            double radius = 10.0;
            bounds = new Rectangle2D(x - radius, y - radius, radius * 2, radius * 2);
            return;
        }
        
        // Calculate bounding box from shape
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (Point2D p : shape) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }
        
        bounds = new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }
    
    /**
     * Main rendering method - draws the junction geometry
     */
    public void render(GraphicsContext g, CoordinateTransform transform) {
        // Skip internal junctions (those with ':' in ID)
        if (id.contains(":")) {
            return;
        }
        
        if (shape != null && shape.size() >= 3) {
            renderPolygonJunction(g, transform);
            //renderCrosswalks(g, transform);
        } else {
            renderCircularJunction(g, transform);
        }
    }
    
    /**
     * Renders junction with proper polygon shape from SUMO
     */
    private void renderPolygonJunction(GraphicsContext g, CoordinateTransform transform) {
        if (shape == null || shape.size() < 3) return;
        
        double[] xPoints = new double[shape.size()];
        double[] yPoints = new double[shape.size()];
        
        for (int i = 0; i < shape.size(); i++) {
            xPoints[i] = transform.worldToScreenX(shape.get(i).getX());
            yPoints[i] = transform.worldToScreenY(shape.get(i).getY());
        }
        
        // Fill junction area (slightly lighter than road for visibility)
        g.setFill(Color.rgb(55, 60, 65));
        g.fillPolygon(xPoints, yPoints, shape.size());
        
        // Subtle border to distinguish from roads
        g.setStroke(Color.rgb(80, 85, 90));
        g.setLineWidth(Math.max(1, transform.worldToScreenSize(0.3)));
        //g.strokePolygon(xPoints, yPoints, shape.size());
    }
    
    /**
     * Renders crosswalk patterns on junction edges
     */
    private void renderCrosswalks(GraphicsContext g, CoordinateTransform transform) {
        if (shape == null || shape.size() < 3) return;
        
        // Draw crosswalk stripes on each edge of the junction
        int numEdges = shape.size();
        for (int i = 0; i < numEdges; i++) {
            Point2D p1 = shape.get(i);
            Point2D p2 = shape.get((i + 1) % numEdges);
            
            double edgeLength = p1.distance(p2);
            
            // Only draw crosswalks on edges longer than 8 meters
            if (edgeLength > 8.0) {
                drawCrosswalkStripes(g, transform, p1, p2);
            }
        }
    }
    
    /**
     * Draws zebra crossing stripes between two points
     */
    private void drawCrosswalkStripes(GraphicsContext g, CoordinateTransform transform, 
                                      Point2D p1, Point2D p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length < 0.1) return;
        
        // Normalize direction
        dx /= length;
        dy /= length;
        
        // Perpendicular direction (for stripe width)
        double perpX = -dy;
        double perpY = dx;
        
        double stripeWidth = 0.5;  // meters
        double stripeSpacing = 1.0; // meters
        double crosswalkWidth = 3.0; // meters
        
        // Draw stripes along the edge
        g.setFill(Color.rgb(240, 240, 240)); // White stripes
        
        double offset = 2.0; // Start 2 meters from edge
        for (double d = offset; d < length - offset; d += stripeSpacing) {
            // Calculate stripe center
            double cx = p1.getX() + dx * d;
            double cy = p1.getY() + dy * d;
            
            // Draw stripe perpendicular to edge
            double x1 = cx - perpX * crosswalkWidth / 2;
            double y1 = cy - perpY * crosswalkWidth / 2;
            double x2 = cx + perpX * crosswalkWidth / 2;
            double y2 = cy + perpY * crosswalkWidth / 2;
            
            double sx1 = transform.worldToScreenX(x1);
            double sy1 = transform.worldToScreenY(y1);
            double sx2 = transform.worldToScreenX(x2);
            double sy2 = transform.worldToScreenY(y2);
            
            g.setLineWidth(Math.max(2, transform.worldToScreenSize(stripeWidth)));
            g.setStroke(Color.rgb(240, 240, 240));
            g.strokeLine(sx1, sy1, sx2, sy2);
        }
        
        // Add yellow warning lines on sides
        g.setStroke(Color.rgb(255, 220, 0));
        g.setLineWidth(Math.max(1, transform.worldToScreenSize(0.15)));
        
        // Left side yellow line
        double lx1 = p1.getX() - perpX * crosswalkWidth / 2;
        double ly1 = p1.getY() - perpY * crosswalkWidth / 2;
        double lx2 = p2.getX() - perpX * crosswalkWidth / 2;
        double ly2 = p2.getY() - perpY * crosswalkWidth / 2;
        
        g.strokeLine(
            transform.worldToScreenX(lx1), transform.worldToScreenY(ly1),
            transform.worldToScreenX(lx2), transform.worldToScreenY(ly2)
        );
        
        // Right side yellow line
        double rx1 = p1.getX() + perpX * crosswalkWidth / 2;
        double ry1 = p1.getY() + perpY * crosswalkWidth / 2;
        double rx2 = p2.getX() + perpX * crosswalkWidth / 2;
        double ry2 = p2.getY() + perpY * crosswalkWidth / 2;
        
        g.strokeLine(
            transform.worldToScreenX(rx1), transform.worldToScreenY(ry1),
            transform.worldToScreenX(rx2), transform.worldToScreenY(ry2)
        );
    }
    
    /**
     * Fallback: render as circle if no shape data
     */
    private void renderCircularJunction(GraphicsContext g, CoordinateTransform transform) {
        double screenX = transform.worldToScreenX(x);
        double screenY = transform.worldToScreenY(y);
        double radius = Math.max(6, transform.worldToScreenSize(5));
        
        // Junction circle (slightly darker)
        g.setFill(Color.rgb(45, 48, 52));
        g.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
        
        // Subtle border
        g.setStroke(Color.rgb(60, 65, 70));
        g.setLineWidth(1);
        g.strokeOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
    }
    
    /**
     * Gets the distance from junction center to boundary in a given direction
     */
    public double getRadiusInDirection(double dirX, double dirY) {
        if (shape.isEmpty()) {
            return 8.0; // Default radius
        }
        
        // For small junctions (3 points = triangle), use simple approach
        if (shape.size() <= 3) {
            return 5.0;
        }
        
        // Find the shape point most aligned with the direction
        double maxDist = 0;
        for (Point2D p : shape) {
            double dx = p.getX() - x;
            double dy = p.getY() - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            
            // Check if this point is roughly in the same direction
            if (dist > 0.1) {
                double dotProduct = (dx * dirX + dy * dirY) / dist;
                if (dotProduct > 0.5) { // Point is in the forward direction
                    maxDist = Math.max(maxDist, dist);
                }
            }
        }
        
        return maxDist > 0 ? maxDist : 8.0;
    }
    
    /**
     * Checks if a screen coordinate point is inside the junction (for clicking)
     */
    public boolean contains(double screenX, double screenY, CoordinateTransform transform) {
        double worldX = transform.screenToWorldX(screenX);
        double worldY = transform.screenToWorldY(screenY);
        
        if (shape.isEmpty()) {
            // Use circular bounds
            double dx = worldX - x;
            double dy = worldY - y;
            return Math.sqrt(dx * dx + dy * dy) <= 8.0;
        }
        
        // Use ray casting algorithm for polygon containment
        return isPointInPolygon(worldX, worldY);
    }
    
    /**
     * Ray casting algorithm to check if point is inside polygon
     */
    private boolean isPointInPolygon(double x, double y) {
        int intersections = 0;
        int n = shape.size();
        
        for (int i = 0; i < n; i++) {
            Point2D p1 = shape.get(i);
            Point2D p2 = shape.get((i + 1) % n);
            
            if (p1.getY() > y != p2.getY() > y) {
                double xIntersect = (p2.getX() - p1.getX()) * (y - p1.getY()) / 
                                   (p2.getY() - p1.getY()) + p1.getX();
                if (x < xIntersect) {
                    intersections++;
                }
            }
        }
        
        return (intersections % 2) == 1;
    }
    
    /**
     * Checks if a world coordinate point is inside the junction
     */
    public boolean containsPoint(double worldX, double worldY) {
        if (shape.isEmpty()) {
            // Use circular bounds
            double dx = worldX - x;
            double dy = worldY - y;
            return Math.sqrt(dx * dx + dy * dy) <= 8.0;
        }
        
        return bounds.contains(worldX, worldY);
    }
    
    // Getters
    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public String getType() { return type; }
    public Rectangle2D getBounds() { return bounds; }
    public List<Point2D> getShape() { return new ArrayList<>(shape); }
    public NetworkParser.Junction getNetworkJunction() { return networkJunction; }
}
