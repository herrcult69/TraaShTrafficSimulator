import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.ArrayList;

public class TrafficLight {
    // Color constants
    private static final Color RED = Color.rgb(255, 0, 0);
    private static final Color YELLOW = Color.rgb(255, 255, 0);
    private static final Color GREEN = Color.rgb(0, 255, 0);
    private static final Color GRAY = Color.rgb(80, 80, 80);
    private static final Color RED_DIM = Color.rgb(60, 0, 0);
    private static final Color YELLOW_DIM = Color.rgb(60, 60, 0);
    private static final Color GREEN_DIM = Color.rgb(0, 60, 0);
    private static final Color BG_DARK = Color.rgb(40, 40, 40);
    
    private String junctionId;
    private Junction junction;
    private String approachEdgeId;
    private double x, y;
    private double rotationAngle;
    private String currentState;
    private List<Integer> linkIndices;

    public static class TrafficLightData {
        public String state;
        public List<String> controlledLanes;
        public int linkCount;
        
        public TrafficLightData(String state, List<String> lanes, int linkCount) {
            this.state = state;
            this.controlledLanes = lanes;
            this.linkCount = linkCount;
        }
    }
    public TrafficLight(String junctionId, Junction junction, String approachEdgeId){
        this.junctionId = junctionId;
        this.junction = junction;
        this.approachEdgeId = approachEdgeId;
        this.linkIndices = new ArrayList<>();
        this.currentState = "";
    }

    public void calculatePosition(List<Edge> edges) {
        Edge fromEdge = null;
        for (Edge edge: edges) {
            if (edge.getNetworkEdge().id.equals(approachEdgeId)) {
                fromEdge = edge;
                break;
            }
        }
        if (fromEdge == null || fromEdge.getLanes().isEmpty()) {
            System.out.println("WARNING: Could not find edge for traffic light: " + approachEdgeId);
            return;
        }
        
        // Get edge geometry
        double edgeFromX = fromEdge.getFromX();
        double edgeFromY = fromEdge.getFromY();
        double edgeToX = fromEdge.getToX();
        double edgeToY = fromEdge.getToY();
        
        // Calculate direction vector
        double dx = edgeToX - edgeFromX;
        double dy = edgeToY - edgeFromY;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length < 0.001) {
            System.out.println("WARNING: Zero-length edge [" + approachEdgeId + "], using junction position");
            this.x = junction.getX();
            this.y = junction.getY();
            return;
        }

        double dirX = dx / length;
        double dirY = dy / length;
        
        // Perpendicular vector for lateral offset
        double perpX = dy / length;
        double perpY = -dx / length;

        // Get the radius at the destination junction
        double toRadius = junction.getRadiusInDirection(-dirX, -dirY);
        
        // Calculate clipped end position
        double clippedEndX = edgeToX - toRadius * dirX;
        double clippedEndY = edgeToY - toRadius * dirY;
        
        // Position traffic light slightly INTO the junction (2m for visibility)
        double junctionOffset = 2.0;
        double baseX = clippedEndX + dirX * junctionOffset;
        double baseY = clippedEndY + dirY * junctionOffset;
        
        // Position at center of the approaching lanes (Direction 2)
        double totalLaneWidth = fromEdge.getNetworkEdge().getTotalWidth();
        double centerLaneOffset = totalLaneWidth + (3.2 / 2);

        // Apply lateral offset to position at road center
        this.x = baseX + perpX * centerLaneOffset;
        this.y = baseY + perpY * centerLaneOffset;

        // Calculate rotation angle perpendicular to road (account for Y-axis flip)
        double roadAngle = Math.toDegrees(Math.atan2(dirY, dirX));
        this.rotationAngle = -roadAngle + 90;
    }
    
    public void classifyLinks(List<Integer> links) {
        this.linkIndices.addAll(links);
    }

    public void setState(String fullState) {
        this.currentState = fullState;
    }

    private Color getSignalColor(List<Integer> indices) {
        if (currentState == null || currentState.isEmpty() || indices.isEmpty()) {
            return GRAY;
        }

        boolean hasGreen = false, hasYellow = false, hasRed = false;
        
        for (int index : indices) {
            if (index >= currentState.length()) continue;
            char signal = currentState.charAt(index);
            
            if (signal == 'G' || signal == 'g') hasGreen = true;
            else if (signal == 'y' || signal == 'Y') hasYellow = true;
            else if (signal == 'r' || signal == 'R') hasRed = true;
        }
        
        if (hasRed) return RED;
        if (hasYellow) return YELLOW;
        if (hasGreen) return GREEN;
        return GRAY;
    }

    public Color getCurrentColor() {
        return getSignalColor(linkIndices);
    }
    
    public void render(GraphicsContext g, CoordinateTransform transform) {
        double screenX = transform.worldToScreenX(x);
        double screenY = transform.worldToScreenY(y);
        double size = Math.max(3, transform.worldToScreenSize(2.5));

        // Save graphics state and apply rotation
        g.save();
        g.translate(screenX, screenY);
        g.rotate(rotationAngle);

        // Render traffic light model
        g.setFill(BG_DARK);
        g.fillRect(-size/2, -size/2, size, size * 1.8);
        g.setStroke(GRAY);
        g.setLineWidth(1);
        g.strokeRect(-size/2, -size/2, size, size * 1.8);

        Color activeColor = getCurrentColor();
        double lightRadius = size * 0.3;
        double spacing = size * 0.45;

        g.setFill(activeColor.equals(RED) ? RED : RED_DIM);
        g.fillOval(-lightRadius, -spacing, lightRadius * 2, lightRadius * 2);

        g.setFill(activeColor.equals(YELLOW) ? YELLOW : YELLOW_DIM);
        g.fillOval(-lightRadius, 0, lightRadius * 2, lightRadius * 2);

        g.setFill(activeColor.equals(GREEN) ? GREEN : GREEN_DIM);
        g.fillOval(-lightRadius, spacing, lightRadius * 2, lightRadius * 2);

        g.restore();
    }

    public boolean contains(double screenX, double screenY, CoordinateTransform transform) {
        double lightScreenX = transform.worldToScreenX(x);
        double lightScreenY = transform.worldToScreenY(y);
        double size = transform.worldToScreenSize(1.5);
        double width = size * 2.5;
        double height = size * 1.8;

        double left = lightScreenX - width / 2;
        double top = lightScreenY - height / 2;
        double right = left + width;
        double bottom = top + height;

        return screenX >= left && screenX <= right && screenY >= top && screenY <= bottom;
    }
    // Getters
    public String getJunctionId() { return junctionId; }
    public String getApproachEdgeId() { return approachEdgeId; }
    public Junction getJunction() { return junction; }
    public String getCurrentState() { return currentState; }
    public List<Integer> getLinkIndices() { return linkIndices; }
    public double getX() { return x; }
    public double getY() { return y; }
}