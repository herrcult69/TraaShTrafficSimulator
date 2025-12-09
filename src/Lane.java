import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Lane {
    private String id;
    private Edge parentEdge;
    private double x1, y1, x2, y2; // Lane center line in world coordinates
    private double width; // Lane width in world units (3.2m)
    private int index; // Lane index (0, 1, 2...)
    private double offsetFromCenter; // Distance from road center line

    public Lane(String id, Edge parentEdge, double width, int index, double offsetFromCenter) {
        this.id = id;
        this.parentEdge = parentEdge;
        this.width = width;
        this.index = index;
        this.offsetFromCenter = offsetFromCenter;
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

        if (length < 0.001)
            return false;

        double t = ((worldX - x1) * dx + (worldY - y1) * dy) / length;
        return t >= -0.1 && t <= 1.1; // Small tolerance
    }

    public void highlight(GraphicsContext g, CoordinateTransform transform, Color color) {
        // Get edge endpoints
        double edgeFromX = parentEdge.getFromX();
        double edgeFromY = parentEdge.getFromY();
        double edgeToX = parentEdge.getToX();
        double edgeToY = parentEdge.getToY();

        double dx = edgeToX - edgeFromX;
        double dy = edgeToY - edgeFromY;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length < 0.001)
            return;

        double dirX = dx / length;
        double dirY = dy / length;

        // Get junction radii for clipping
        Junction fromJunction = parentEdge.getFromJunction();
        Junction toJunction = parentEdge.getToJunction();

        double fromRadius = fromJunction.getRadiusInDirection(dirX, dirY);
        double toRadius = toJunction.getRadiusInDirection(-dirX, -dirY);

        // Clip lane at junction boundaries (same as edge rendering)
        double clippedStartX = edgeFromX + fromRadius * dirX;
        double clippedStartY = edgeFromY + fromRadius * dirY;
        double clippedEndX = edgeToX - toRadius * dirX;
        double clippedEndY = edgeToY - toRadius * dirY;

        // Apply lane offset to clipped coordinates
        double perpX = dy / length;
        double perpY = -dx / length;

        double laneStartX = clippedStartX + offsetFromCenter * perpX;
        double laneStartY = clippedStartY + offsetFromCenter * perpY;
        double laneEndX = clippedEndX + offsetFromCenter * perpX;
        double laneEndY = clippedEndY + offsetFromCenter * perpY;

        // Calculate perpendicular for lane width
        double laneDx = laneEndX - laneStartX;
        double laneDy = laneEndY - laneStartY;
        double laneLength = Math.sqrt(laneDx * laneDx + laneDy * laneDy);

        if (laneLength < 0.001)
            return;

        double lanePerpX = -laneDy / laneLength;
        double lanePerpY = laneDx / laneLength;
        double halfWidth = width / 2.0;

        // Calculate 4 corners of the clipped lane
        double[] xPoints = {
                transform.worldToScreenX(laneStartX - lanePerpX * halfWidth),
                transform.worldToScreenX(laneStartX + lanePerpX * halfWidth),
                transform.worldToScreenX(laneEndX + lanePerpX * halfWidth),
                transform.worldToScreenX(laneEndX - lanePerpX * halfWidth)
        };

        double[] yPoints = {
                transform.worldToScreenY(laneStartY - lanePerpY * halfWidth),
                transform.worldToScreenY(laneStartY + lanePerpY * halfWidth),
                transform.worldToScreenY(laneEndY + lanePerpY * halfWidth),
                transform.worldToScreenY(laneEndY - lanePerpY * halfWidth)
        };

        // Draw semi-transparent fill
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.15));
        g.fillPolygon(xPoints, yPoints, 4);

        // Draw dashed outline
        g.setStroke(color);
        g.setLineWidth(2.5);
        g.setLineDashes(8, 4);
        g.strokePolygon(xPoints, yPoints, 4);
        g.setLineDashes(null);
    }

    // Getters
    public String getId() {
        return id;
    }

    public Edge getParentEdge() {
        return parentEdge;
    }

    public double getWidth() {
        return width;
    }

    public int getIndex() {
        return index;
    }

    public double getOffsetFromCenter() {
        return offsetFromCenter;
    }

    public double getCenterX1() {
        return x1;
    }

    public double getCenterY1() {
        return y1;
    }

    public double getCenterX2() {
        return x2;
    }

    public double getCenterY2() {
        return y2;
    }
}