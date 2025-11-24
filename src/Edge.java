import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class Edge {
    private NetworkParser.Edge networkEdge;
    private double fromX, fromY, toX, toY;  // World coordinates
    private Junction fromJunction;
    private Junction toJunction;
    private List<Lane> lanes;
    private static final double JUNCTION_MARGIN = 0.0; // meters
    
    public Edge(NetworkParser.Edge networkEdge, NetworkParser.Junction from, NetworkParser.Junction to, Junction fromJunc, Junction toJunc) {
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
    
    private void createLanes() {
        int numLanes = networkEdge.lanes.size();
        
        // Create lanes for both directions (bidirectional road)
        // Direction 1 (negative offsets - left side)
        double cumulativeOffset = 0;
        for (int i = 0; i < numLanes; i++) {
            double laneWidth = networkEdge.lanes.get(i).width;
            double offset = -(cumulativeOffset + laneWidth / 2.0);
            String laneId = networkEdge.id + "_dir1_lane" + i;
            Lane lane = new Lane(laneId, this, laneWidth, i, offset);
            lanes.add(lane);
            cumulativeOffset += laneWidth;
        }
        
        // Direction 2 (positive offsets - right side)
        cumulativeOffset = 0;
        for (int i = 0; i < numLanes; i++) {
            double laneWidth = networkEdge.lanes.get(i).width;
            double offset = cumulativeOffset + laneWidth / 2.0;
            String laneId = networkEdge.id + "_dir2_lane" + i;
            Lane lane = new Lane(laneId, this, laneWidth, i + numLanes, offset);
            lanes.add(lane);
            cumulativeOffset += laneWidth;
        }
    }
    
    public Lane getLaneAt(double screenX, double screenY, CoordinateTransform transform) {
        for (Lane lane : lanes) {
            if (lane.contains(screenX, screenY, transform)) {
                return lane;
            }
        }
        return null;
    }
    
    public void render(GraphicsContext g, CoordinateTransform transform) {
        // Calculate edge direction
        double dx = toX - fromX;
        double dy = toY - fromY;
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length < 0.001) return;
        
        // Normalize direction
        double dirX = dx / length;
        double dirY = dy / length;
    
        // Get junction radii with smaller margin for smooth connection
        double fromRadius = fromJunction != null ? 
            fromJunction.getRadiusInDirection(dirX, dirY) + JUNCTION_MARGIN : 0.0;
        double toRadius = toJunction != null ? 
            toJunction.getRadiusInDirection(-dirX, -dirY) + JUNCTION_MARGIN : 0.0;
        
        // Clip edge at junction boundaries
        double startX = fromX + fromRadius * dirX;
        double startY = fromY + fromRadius * dirY;
        double endX = toX - toRadius * dirX;
        double endY = toY - toRadius * dirY;
        
        // Check if edge is too short after clipping
        double clippedLength = length - fromRadius - toRadius;
        if (clippedLength < 1.0) return;
        
        double x1 = transform.worldToScreenX(startX);
        double y1 = transform.worldToScreenY(startY);
        double x2 = transform.worldToScreenX(endX);
        double y2 = transform.worldToScreenY(endY);
        
        // Skip tiny edges
        if (Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)) < 1) return;
        
        double totalWidth = networkEdge.getTotalWidth() * 2; // Bidirectional
        double screenWidth = transform.worldToScreenSize(totalWidth);
        
        // Draw road surface (similar as junctions)
        g.setStroke(Color.rgb(55, 60, 65));
        g.setLineWidth(screenWidth);
        g.strokeLine(x1, y1, x2, y2);
        
        // Draw lane markings using clipped coordinates
        renderLaneMarkings(g, transform, startX, startY, endX, endY);
    }
    
    private void renderLaneMarkings(GraphicsContext g, CoordinateTransform transform,
                                    double startX, double startY, double endX, double endY) {
        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001) return;
        
        double perpX = dy / length;
        double perpY = -dx / length;
        
        int numLanes = networkEdge.lanes.size();
        double halfWidth = networkEdge.getTotalWidth();
        
        // Road edges (gray)
        g.setStroke(Color.rgb(180, 180, 180));
        g.setLineWidth(Math.max(2, transform.worldToScreenSize(0.15)));
        
        double edgeX1 = startX - halfWidth * perpX;
        double edgeY1 = startY - halfWidth * perpY;
        double edgeX2 = endX - halfWidth * perpX;
        double edgeY2 = endY - halfWidth * perpY;
        g.strokeLine(transform.worldToScreenX(edgeX1), transform.worldToScreenY(edgeY1),
                     transform.worldToScreenX(edgeX2), transform.worldToScreenY(edgeY2));
        
        edgeX1 = startX + halfWidth * perpX;
        edgeY1 = startY + halfWidth * perpY;
        edgeX2 = endX + halfWidth * perpX;
        edgeY2 = endY + halfWidth * perpY;
        g.strokeLine(transform.worldToScreenX(edgeX1), transform.worldToScreenY(edgeY1),
                     transform.worldToScreenX(edgeX2), transform.worldToScreenY(edgeY2));
        
        // Center line (yellow)
        g.setStroke(Color.rgb(255, 220, 50));
        g.setLineWidth(Math.max(2, transform.worldToScreenSize(0.15)));
        g.strokeLine(transform.worldToScreenX(startX), transform.worldToScreenY(startY),
                     transform.worldToScreenX(endX), transform.worldToScreenY(endY));
        
        // Lane dividers (white dashed)
        g.setStroke(Color.WHITE);
        g.setLineWidth(Math.max(1.5, transform.worldToScreenSize(0.12)));
        double dashSize = Math.max(8, transform.worldToScreenSize(3));
        double gapSize = Math.max(6, transform.worldToScreenSize(2));
        g.setLineDashes(dashSize, gapSize);
        
        // Draw dividers between lanes using cumulative offsets
        double cumulativeOffset = 0;
        for (int i = 0; i < numLanes - 1; i++) {
            cumulativeOffset += networkEdge.lanes.get(i).width;
            
            // Left side
            double divX1 = startX - cumulativeOffset * perpX;
            double divY1 = startY - cumulativeOffset * perpY;
            double divX2 = endX - cumulativeOffset * perpX;
            double divY2 = endY - cumulativeOffset * perpY;
            g.strokeLine(transform.worldToScreenX(divX1), transform.worldToScreenY(divY1),
                         transform.worldToScreenX(divX2), transform.worldToScreenY(divY2));
            
            // Right side
            divX1 = startX + cumulativeOffset * perpX;
            divY1 = startY + cumulativeOffset * perpY;
            divX2 = endX + cumulativeOffset * perpX;
            divY2 = endY + cumulativeOffset * perpY;
            g.strokeLine(transform.worldToScreenX(divX1), transform.worldToScreenY(divY1),
                         transform.worldToScreenX(divX2), transform.worldToScreenY(divY2));
        }
        g.setLineDashes();
    }
    
    // Getters
    public NetworkParser.Edge getNetworkEdge() { return networkEdge; }
    public List<Lane> getLanes() { return lanes; }
    public double getFromX() { return fromX; }
    public double getFromY() { return fromY; }
    public double getToX() { return toX; }
    public double getToY() { return toY; }
}