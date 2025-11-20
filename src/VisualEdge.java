import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class VisualEdge {
    private NetworkParser.Edge networkEdge;
    private double fromX, fromY, toX, toY;  // World coordinates
    private List<VisualLane> lanes;
    private Rectangle2D bounds;
    
    public VisualEdge(NetworkParser.Edge networkEdge, NetworkParser.Junction from, NetworkParser.Junction to) {
        this.networkEdge = networkEdge;
        this.fromX = from.x;
        this.fromY = from.y;
        this.toX = to.x;
        this.toY = to.y;
        this.lanes = new ArrayList<>();
        
        createLanes();
        calculateBounds();
    }
    
    private void createLanes() {
        int numLanes = networkEdge.lanes.size();
        double laneWidth = 3.2; // Standard SUMO lane width
        
        // Create lanes for both directions (bidirectional road)
        // Direction 1 (negative offsets)
        for (int i = 0; i < numLanes; i++) {
            double offset = -laneWidth * (i + 0.5);
            String laneId = networkEdge.id + "_dir1_lane" + i;
            VisualLane lane = new VisualLane(laneId, this, laneWidth, i, offset);
            lanes.add(lane);
        }
        
        // Direction 2 (positive offsets)
        for (int i = 0; i < numLanes; i++) {
            double offset = laneWidth * (i + 0.5);
            String laneId = networkEdge.id + "_dir2_lane" + i;
            VisualLane lane = new VisualLane(laneId, this, laneWidth, i + numLanes, offset);
            lanes.add(lane);
        }
    }
    
    private void calculateBounds() {
        double totalWidth = networkEdge.getTotalWidth() * 2; // Bidirectional
        double minX = Math.min(fromX, toX) - totalWidth/2;
        double maxX = Math.max(fromX, toX) + totalWidth/2;
        double minY = Math.min(fromY, toY) - totalWidth/2;
        double maxY = Math.max(fromY, toY) + totalWidth/2;
        
        bounds = new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }
    
    public boolean contains(double screenX, double screenY, CoordinateTransform transform) {
        double worldX = transform.screenToWorldX(screenX);
        double worldY = transform.screenToWorldY(screenY);
        return bounds.contains(worldX, worldY);
    }
    
    public VisualLane getLaneAt(double screenX, double screenY, CoordinateTransform transform) {
        for (VisualLane lane : lanes) {
            if (lane.contains(screenX, screenY, transform)) {
                return lane;
            }
        }
        return null;
    }
    
    public void render(GraphicsContext g, CoordinateTransform transform) {
        double x1 = transform.worldToScreenX(fromX);
        double y1 = transform.worldToScreenY(fromY);
        double x2 = transform.worldToScreenX(toX);
        double y2 = transform.worldToScreenY(toY);
        
        // Skip tiny edges
        if (Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)) < 1) return;
        
        double totalWidth = networkEdge.getTotalWidth() * 2; // Bidirectional
        double screenWidth = transform.worldToScreenSize(totalWidth);
        
        // Draw road surface
        g.setStroke(Color.rgb(60, 66, 72));
        g.setLineWidth(screenWidth);
        g.strokeLine(x1, y1, x2, y2);
        
        // Draw lane markings
        renderLaneMarkings(g, transform);
    }
    
    private void renderLaneMarkings(GraphicsContext g, CoordinateTransform transform) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001) return;
        
        double perpX = dy / length;
        double perpY = -dx / length;
        
        int numLanes = networkEdge.lanes.size();
        double laneWidth = 3.2;
        double halfWidth = numLanes * laneWidth;
        
        // Road edges (gray)
        g.setStroke(Color.rgb(180, 180, 180));
        g.setLineWidth(Math.max(2, transform.worldToScreenSize(0.15)));
        
        double edgeX1 = fromX - halfWidth * perpX;
        double edgeY1 = fromY - halfWidth * perpY;
        double edgeX2 = toX - halfWidth * perpX;
        double edgeY2 = toY - halfWidth * perpY;
        g.strokeLine(transform.worldToScreenX(edgeX1), transform.worldToScreenY(edgeY1),
                     transform.worldToScreenX(edgeX2), transform.worldToScreenY(edgeY2));
        
        edgeX1 = fromX + halfWidth * perpX;
        edgeY1 = fromY + halfWidth * perpY;
        edgeX2 = toX + halfWidth * perpX;
        edgeY2 = toY + halfWidth * perpY;
        g.strokeLine(transform.worldToScreenX(edgeX1), transform.worldToScreenY(edgeY1),
                     transform.worldToScreenX(edgeX2), transform.worldToScreenY(edgeY2));
        
        // Center line (yellow)
        g.setStroke(Color.rgb(255, 220, 50));
        g.setLineWidth(Math.max(2, transform.worldToScreenSize(0.15)));
        g.strokeLine(transform.worldToScreenX(fromX), transform.worldToScreenY(fromY),
                     transform.worldToScreenX(toX), transform.worldToScreenY(toY));
        
        // Lane dividers (white dashed)
        g.setStroke(Color.WHITE);
        g.setLineWidth(Math.max(1.5, transform.worldToScreenSize(0.12)));
        double dashSize = Math.max(8, transform.worldToScreenSize(3));
        double gapSize = Math.max(6, transform.worldToScreenSize(2));
        g.setLineDashes(dashSize, gapSize);
        
        for (int i = 1; i < numLanes; i++) {
            double offset = laneWidth * i;
            // Left side
            double divX1 = fromX - offset * perpX;
            double divY1 = fromY - offset * perpY;
            double divX2 = toX - offset * perpX;
            double divY2 = toY - offset * perpY;
            g.strokeLine(transform.worldToScreenX(divX1), transform.worldToScreenY(divY1),
                         transform.worldToScreenX(divX2), transform.worldToScreenY(divY2));
            
            // Right side
            divX1 = fromX + offset * perpX;
            divY1 = fromY + offset * perpY;
            divX2 = toX + offset * perpX;
            divY2 = toY + offset * perpY;
            g.strokeLine(transform.worldToScreenX(divX1), transform.worldToScreenY(divY1),
                         transform.worldToScreenX(divX2), transform.worldToScreenY(divY2));
        }
        g.setLineDashes();
    }
    
    // Getters
    public NetworkParser.Edge getNetworkEdge() { return networkEdge; }
    public List<VisualLane> getLanes() { return lanes; }
    public double getFromX() { return fromX; }
    public double getFromY() { return fromY; }
    public double getToX() { return toX; }
    public double getToY() { return toY; }
    public Rectangle2D getBounds() { return bounds; }
}