import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a directed road segment between two junctions.
 * Contains multiple lanes and handles rendering with lane markings.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see Renderable
 */
public class Edge extends Renderable {
    private NetworkParser.Edge networkEdge;
    private double fromX, fromY, toX, toY; // World coordinates
    private Junction fromJunction;
    private Junction toJunction;
    private List<Lane> lanes;

    /**
     * Constructs a new visual Edge from parsed network data.
     * 
     * @param networkEdge The parsed edge data from SUMO network file
     * @param from        The source junction network data
     * @param to          The destination junction network data
     * @param fromJunc    The visual source junction object
     * @param toJunc      The visual destination junction object
     */
    public Edge(NetworkParser.Edge networkEdge, NetworkParser.Junction from, NetworkParser.Junction to,
            Junction fromJunc, Junction toJunc) {
        this.networkEdge = networkEdge;
        this.fromX = from.x;
        this.fromY = from.y;
        this.toX = to.x;
        this.toY = to.y;
        this.fromJunction = fromJunc;
        this.toJunction = toJunc;
        this.lanes = new ArrayList<>();

        createLanes();
    }

    /**
     * Creates visual Lane objects for all lanes in this edge.
     */
    private void createLanes() {
        // Create lanes directly from SUMO network data
        // SUMO already handles directionality with positive/negative edge IDs
        // Each edge contains only the lanes for that specific direction

        double cumulativeOffset = 0;
        for (int i = 0; i < networkEdge.lanes.size(); i++) {
            NetworkParser.Lane sumoLane = networkEdge.lanes.get(i);
            double laneWidth = sumoLane.width;

            // Calculate offset from edge centerline
            // Lanes are ordered from right to left in SUMO
            double offset = cumulativeOffset + laneWidth / 2.0;

            // Use SUMO's actual lane ID
            String laneId = sumoLane.id;
            Lane lane = new Lane(laneId, this, laneWidth, i, offset);
            lanes.add(lane);
            cumulativeOffset += laneWidth;
        }
    }

    /**
     * Returns the lane at the specified screen coordinates, or null if none.
     * 
     * @param screenX   The X coordinate in screen space
     * @param screenY   The Y coordinate in screen space
     * @param transform The coordinate transformation
     * @return The lane at that position, or null
     */
    public Lane getLaneAt(double screenX, double screenY, CoordinateTransform transform) {
        for (Lane lane : lanes) {
            if (lane.contains(screenX, screenY, transform)) {
                return lane;
            }
        }
        return null;
    }

    /**
     * Renders the edge including road surface and lane markings.
     * 
     * @param g         The graphics context to draw on
     * @param transform The coordinate transformation
     */
    @Override
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

        // Get junction radii with smaller margin for smooth connection
        double fromRadius = fromJunction != null ? fromJunction.getRadiusInDirection(dirX, dirY) : 0;
        double toRadius = toJunction != null ? toJunction.getRadiusInDirection(-dirX, -dirY) : 0;

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

        // Calculate perpendicular vector (reuse clippedLength and direction)
        double perpX = dy / length;
        double perpY = -dx / length;

        // Calculate actual width from lanes (one direction only)
        double totalWidth = networkEdge.getTotalWidth();
        double screenWidth = transform.worldToScreenSize(totalWidth);

        // Offset the edge centerline by half its width to position it correctly
        double halfWidth = totalWidth / 2.0;
        double offsetStartX = startX + halfWidth * perpX;
        double offsetStartY = startY + halfWidth * perpY;
        double offsetEndX = endX + halfWidth * perpX;
        double offsetEndY = endY + halfWidth * perpY;

        // Draw road surface at the offset position
        g.setStroke(Color.rgb(55, 60, 65));
        g.setLineWidth(screenWidth);
        g.strokeLine(
                transform.worldToScreenX(offsetStartX),
                transform.worldToScreenY(offsetStartY),
                transform.worldToScreenX(offsetEndX),
                transform.worldToScreenY(offsetEndY));

        // Draw lane markings using clipped coordinates
        renderLaneMarkings(g, transform, startX, startY, endX, endY);
    }

    private void renderLaneMarkings(GraphicsContext g, CoordinateTransform transform,
            double startX, double startY, double endX, double endY) {
        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001)
            return;

        double perpX = dy / length;
        double perpY = -dx / length;

        int numLanes = networkEdge.lanes.size();

        // Draw center line (yellow) - only for positive edge IDs to avoid double
        // drawing
        if (!networkEdge.id.startsWith("-")) {
            g.setStroke(Color.rgb(255, 220, 50));
            g.setLineWidth(Math.max(1, transform.worldToScreenSize(0.5)));
            g.strokeLine(transform.worldToScreenX(startX), transform.worldToScreenY(startY),
                    transform.worldToScreenX(endX), transform.worldToScreenY(endY));
        }

        // Only draw lane dividers if there are multiple lanes
        if (numLanes > 1) {
            g.setStroke(Color.WHITE);
            g.setLineWidth(Math.max(.5, transform.worldToScreenSize(0.12)));
            double dashSize = Math.max(2, transform.worldToScreenSize(3));
            double gapSize = Math.max(1, transform.worldToScreenSize(2));
            g.setLineDashes(dashSize, gapSize);

            // Draw dividers between lanes
            double cumulativeOffset = 0;
            for (int i = 0; i < numLanes - 1; i++) {
                cumulativeOffset += networkEdge.lanes.get(i).width;

                double divX1 = startX + cumulativeOffset * perpX;
                double divY1 = startY + cumulativeOffset * perpY;
                double divX2 = endX + cumulativeOffset * perpX;
                double divY2 = endY + cumulativeOffset * perpY;
                g.strokeLine(transform.worldToScreenX(divX1), transform.worldToScreenY(divY1),
                        transform.worldToScreenX(divX2), transform.worldToScreenY(divY2));
            }
            g.setLineDashes();
        }
    }

    // Getters
    /**
     * Returns the original parsed network edge data.
     * 
     * @return The NetworkParser.Edge data
     */
    public NetworkParser.Edge getNetworkEdge() {
        return networkEdge;
    }

    /**
     * Returns the list of visual lanes in this edge.
     * 
     * @return List of Lane objects
     */
    public List<Lane> getLanes() {
        return lanes;
    }

    /**
     * Returns the source junction X coordinate in world space.
     * 
     * @return The X coordinate in meters
     */
    public double getFromX() {
        return fromX;
    }

    /**
     * Returns the source junction Y coordinate in world space.
     * 
     * @return The Y coordinate in meters
     */
    public double getFromY() {
        return fromY;
    }

    /**
     * Returns the destination junction X coordinate in world space.
     * 
     * @return The X coordinate in meters
     */
    public double getToX() {
        return toX;
    }

    /**
     * Returns the destination junction Y coordinate in world space.
     * 
     * @return The Y coordinate in meters
     */
    public double getToY() {
        return toY;
    }

    /**
     * Returns the source junction visual object.
     * 
     * @return The source Junction
     */
    public Junction getFromJunction() {
        return fromJunction;
    }

    /**
     * Returns the destination junction visual object.
     * 
     * @return The destination Junction
     */
    public Junction getToJunction() {
        return toJunction;
    }

    /**
     * Highlights this edge with a colored overlay.
     * Used during route selection to show selected edges.
     * 
     * @param g         The graphics context to draw on
     * @param transform The coordinate transformation
     * @param color     The highlight color
     */
    @Override
    public void highlight(GraphicsContext g, CoordinateTransform transform, Color color) {
        // Calculate edge direction
        double dx = toX - fromX;
        double dy = toY - fromY;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length < 0.001)
            return;

        // Normalize direction
        double dirX = dx / length;
        double dirY = dy / length;

        // Get junction radii
        double fromRadius = fromJunction != null ? fromJunction.getRadiusInDirection(dirX, dirY) : 0;
        double toRadius = toJunction != null ? toJunction.getRadiusInDirection(-dirX, -dirY) : 0;

        // Clip edge at junction boundaries (avoid highlighting over junctions)
        double startX = fromX + fromRadius * dirX;
        double startY = fromY + fromRadius * dirY;
        double endX = toX - toRadius * dirX;
        double endY = toY - toRadius * dirY;

        // Calculate perpendicular vector for offset
        double perpX = dy / length;
        double perpY = -dx / length;

        // Apply same offset as render() - position edge on its side of the road
        double totalWidth = networkEdge.getTotalWidth();
        double halfWidth = totalWidth / 2.0;
        double offsetStartX = startX + halfWidth * perpX;
        double offsetStartY = startY + halfWidth * perpY;
        double offsetEndX = endX + halfWidth * perpX;
        double offsetEndY = endY + halfWidth * perpY;

        double x1 = transform.worldToScreenX(offsetStartX);
        double y1 = transform.worldToScreenY(offsetStartY);
        double x2 = transform.worldToScreenX(offsetEndX);
        double y2 = transform.worldToScreenY(offsetEndY);

        double screenWidth = transform.worldToScreenSize(totalWidth);

        // Draw highlighted overlay
        g.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.7));
        g.setLineWidth(screenWidth + 4);
        g.setLineDashes(10, 5);
        g.strokeLine(x1, y1, x2, y2);
        g.setLineDashes();

        // Draw solid inner highlight
        g.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.4));
        g.setLineWidth(screenWidth);
        g.strokeLine(x1, y1, x2, y2);
    }

    /**
     * Not used - hit detection uses getLaneAt() instead.
     * Required by Renderable abstract class.
     */
    @Override
    public boolean contains(double screenX, double screenY, CoordinateTransform transform) {
        return false;
    }
}