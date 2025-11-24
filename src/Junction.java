import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a road junction/intersection with proper geometry rendering.
 */
public class Junction {
    private NetworkParser.Junction networkJunction;
    private String id;
    private double x, y; // Center position in world coordinates
    private String type;
    private List<Point2D> shape; // Junction boundary polygon

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
            // renderCrosswalks(g, transform);
        }
        // else {
        // renderCircularJunction(g, transform);
        // }
    }

    /**
     * Renders junction with proper polygon shape from SUMO
     */
    private void renderPolygonJunction(GraphicsContext g, CoordinateTransform transform) {
        if (shape == null || shape.size() < 3)
            return;

        double[] xPoints = new double[shape.size()];
        double[] yPoints = new double[shape.size()];

        for (int i = 0; i < shape.size(); i++) {
            xPoints[i] = transform.worldToScreenX(shape.get(i).getX());
            yPoints[i] = transform.worldToScreenY(shape.get(i).getY());
        }

        // Fill junction area (slightly lighter than road for visibility)
        g.setFill(Color.rgb(55, 60, 65));
        g.fillPolygon(xPoints, yPoints, shape.size());

    }

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
                    maxDist = Math.max(maxDist, dist) - .5;
                }
            }
        }

        return maxDist > 0 ? maxDist : 8.0;
    }

    // Getters
    public String getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getType() {
        return type;
    }

    public List<Point2D> getShape() {
        return new ArrayList<>(shape);
    }

    public NetworkParser.Junction getNetworkJunction() {
        return networkJunction;
    }
}
