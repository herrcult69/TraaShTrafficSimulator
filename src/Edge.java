import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Edge {
    private NetworkParser.Edge networkEdge;
    private double fromX, fromY, toX, toY; // World coordinates
    private Junction fromJunction;
    private Junction toJunction;

    public Edge(NetworkParser.Edge networkEdge, NetworkParser.Junction from, NetworkParser.Junction to,
            Junction fromJunc, Junction toJunc) {
        this.networkEdge = networkEdge;
        this.fromX = from.x;
        this.fromY = from.y;
        this.toX = to.x;
        this.toY = to.y;
        this.fromJunction = fromJunc;
        this.toJunction = toJunc;
    }

    public void render(GraphicsContext g, CoordinateTransform transform) {
        // Calculate edge direction
        double dx = toX - fromX;
        double dy = toY - fromY;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length < 0.001)
            return;

        // Normalize direction
        double dirX = dx / length;
        double dirY = dy / length;

        // Get junction radii for clipping
        double fromRadius = fromJunction != null ? fromJunction.getRadius() : 0;
        double toRadius = toJunction != null ? toJunction.getRadius() : 0;

        // Clip edge at junction boundaries
        double startX = fromX + fromRadius * dirX;
        double startY = fromY + fromRadius * dirY;
        double endX = toX - toRadius * dirX;
        double endY = toY - toRadius * dirY;

        // Check if edge is too short after clipping
        double clippedLength = length - fromRadius - toRadius;
        if (clippedLength < 1.0)
            return;

        double x1 = transform.worldToScreenX(startX);
        double y1 = transform.worldToScreenY(startY);
        double x2 = transform.worldToScreenX(endX);
        double y2 = transform.worldToScreenY(endY);

        // Skip tiny edges
        if (Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)) < 1)
            return;

        double totalWidth = networkEdge.getTotalWidth() * 2; // Bidirectional
        double screenWidth = transform.worldToScreenSize(totalWidth);

        // Simple road rendering: solid gray with yellow center line
        
        // Draw solid road surface
        g.setStroke(Color.rgb(70, 70, 70));
        g.setLineWidth(screenWidth);
        g.strokeLine(x1, y1, x2, y2);

        // Draw yellow center line
        g.setStroke(Color.rgb(255, 220, 50));
        g.setLineWidth(Math.max(1, transform.worldToScreenSize(0.2)));
        g.strokeLine(x1, y1, x2, y2);
    }

    // Getters
    public NetworkParser.Edge getNetworkEdge() {
        return networkEdge;
    }

    public double getFromX() {
        return fromX;
    }

    public double getFromY() {
        return fromY;
    }

    public double getToX() {
        return toX;
    }

    public double getToY() {
        return toY;
    }
}