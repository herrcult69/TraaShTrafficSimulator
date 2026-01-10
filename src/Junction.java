import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a road junction (intersection) with polygon geometry.
 * Handles rendering, hit detection, and edge clipping calculations.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see Renderable
 */
public class Junction extends Renderable {
    // Constants for junction geometry
    private static final double DEFAULT_RADIUS = 8.0;
    private static final double SMALL_JUNCTION_RADIUS = 5.0;
    private static final double MIN_RADIUS = 3.0;
    private static final double DIRECTION_THRESHOLD = 0.8;
    private static final double DISTANCE_THRESHOLD = 0.1;
    private static final double RADIUS_ADJUSTMENT = 1.0;

    private NetworkParser.Junction networkJunction;
    private String id;
    private double x, y; // Center position in world coordinates
    private String type;
    private List<Point2D> shape; // Junction boundary polygon
    private double radius; // Get the radius

    /**
     * Constructs a junction from network data.
     * 
     * @param networkJunction Parsed junction data
     */
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
        calculateRadius();
    }

    /**
     * Checks if a screen point falls within the junction boundary.
     * 
     * @param screenX X coordinate in screen space
     * @param screenY Y coordinate in screen space
     * @param transform Coordinate transform
     * @return True if point is inside junction
     */
    @Override
    public boolean contains(double screenX, double screenY, CoordinateTransform transform) {
        if (shape == null || shape.size() < 3) {
            // Fallback to circular check
            double worldX = transform.screenToWorldX(screenX);
            double worldY = transform.screenToWorldY(screenY);
            double dist = Math.sqrt(Math.pow(worldX - x, 2) + Math.pow(worldY - y, 2));
            return dist < 5.0; // Default radius
        }

        // Point in polygon test
        double worldX = transform.screenToWorldX(screenX);
        double worldY = transform.screenToWorldY(screenY);

        boolean result = false;
        for (int i = 0, j = shape.size() - 1; i < shape.size(); j = i++) {
            if ((shape.get(i).getY() > worldY) != (shape.get(j).getY() > worldY) &&
                    (worldX < (shape.get(j).getX() - shape.get(i).getX()) * (worldY - shape.get(i).getY())
                            / (shape.get(j).getY() - shape.get(i).getY()) + shape.get(i).getX())) {
                result = !result;
            }
        }
        return result;
    }

    /**
     * Draws a highlight overlay on the junction.
     * 
     * @param g Graphics context
     * @param transform Coordinate transform
     * @param color Highlight color
     */
    @Override
    public void highlight(GraphicsContext g, CoordinateTransform transform, Color color) {
        if (shape == null || shape.size() < 3)
            return;

        double[] xPoints = new double[shape.size()];
        double[] yPoints = new double[shape.size()];

        for (int i = 0; i < shape.size(); i++) {
            xPoints[i] = transform.worldToScreenX(shape.get(i).getX());
            yPoints[i] = transform.worldToScreenY(shape.get(i).getY());
        }

        // Semi-transparent fill
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.15));
        g.fillPolygon(xPoints, yPoints, shape.size());

        // Dashed outline
        g.setStroke(color);
        g.setLineWidth(2.5);
        g.setLineDashes(8, 4);
        g.strokePolygon(xPoints, yPoints, shape.size());
        g.setLineDashes(null);
    }

    /**
     * Draws the junction geometry. Skips internal junctions.
     * 
     * @param g Graphics context
     * @param transform Coordinate transform
     */
    @Override
    public void render(GraphicsContext g, CoordinateTransform transform) {
        // Skip internal junctions (those with ':' in ID)
        if (id.contains(":")) {
            return;
        }

        if (shape != null && shape.size() >= 3) {
            renderPolygonJunction(g, transform);
        }
    }

    /**
     * Renders the junction as a filled polygon.
     * 
     * @param g Graphics context
     * @param transform Coordinate transform
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

    /**
     * Calculates the junction's effective radius in a specific direction.
     * 
     * @param dirX X component of direction vector
     * @param dirY Y component of direction vector
     * @return Radius in meters along that direction
     */
    public double getRadiusInDirection(double dirX, double dirY) {
        if (shape.isEmpty()) {
            return DEFAULT_RADIUS;
        }

        // For small junctions (3 points = triangle), use simple approach
        if (shape.size() <= 3) {
            return SMALL_JUNCTION_RADIUS;
        }

        // Find the shape point most aligned with the direction
        double maxDist = 0;
        for (Point2D p : shape) {
            double dx = p.getX() - x;
            double dy = p.getY() - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            // Check if this point is roughly in the same direction
            if (dist > DISTANCE_THRESHOLD) {
                double dotProduct = (dx * dirX + dy * dirY) / dist;
                if (dotProduct > DIRECTION_THRESHOLD) {
                    maxDist = Math.max(maxDist, dist) - RADIUS_ADJUSTMENT;
                }
            }
        }

        return maxDist > 0 ? maxDist : DEFAULT_RADIUS;
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

    private void calculateRadius() {
        radius = DEFAULT_RADIUS;
        if (shape != null && !shape.isEmpty()) {
            radius = 0;
            for (Point2D point : shape) {
                double dx = point.getX() - x;
                double dy = point.getY() - y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                radius = Math.max(radius, distance);
            }

            // Ensure minimum radius for click detection
            if (radius < MIN_RADIUS) {
                radius = DEFAULT_RADIUS;
            }
        }
    }

    // Getters
    /** Returns junction ID. */
    public String getId() {
        return id;
    }

    /** Returns X coordinate in meters. */
    public double getX() {
        return x;
    }

    /** Returns Y coordinate in meters. */
    public double getY() {
        return y;
    }

    /** Returns junction type. */
    public String getType() {
        return type;
    }

    /**
     * Returns a copy of the junction's boundary shape points.
     * 
     * @return List of 2D points defining the polygon boundary
     */
    public List<Point2D> getShape() {
        return new ArrayList<>(shape);
    }

    /**
     * Returns the original parsed network junction data.
     * 
     * @return The NetworkParser.Junction data
     */
    public NetworkParser.Junction getNetworkJunction() {
        return networkJunction;
    }
}
