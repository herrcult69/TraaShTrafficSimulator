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

    // Individual signal representing one connection
    public static class Signal {
        public String fromEdge;
        public int fromLaneIndex;
        public String toEdge;
        public int toLaneIndex;
        public int linkIndex;
        public double x, y;
        public double rotationAngle;
        public String direction;  // s, l, r, t, etc.
        
        public Signal(String fromEdge, int fromLaneIndex, String toEdge, int toLaneIndex, 
                     int linkIndex, String direction) {
            this.fromEdge = fromEdge;
            this.fromLaneIndex = fromLaneIndex;
            this.toEdge = toEdge;
            this.toLaneIndex = toLaneIndex;
            this.linkIndex = linkIndex;
            this.direction = direction;
        }
    }

    private String junctionId;
    private Junction junction;
    private List<Signal> signals;
    private String currentState;
    private boolean manualMode = false;

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

    public TrafficLight(String junctionId, Junction junction) {
        this.junctionId = junctionId;
        this.junction = junction;
        this.signals = new ArrayList<>();
        this.currentState = "";
    }

    public void addSignal(Signal signal) {
        this.signals.add(signal);
    }

    public void calculatePositions(List<Edge> edges) {
        for (Signal signal : signals) {
            calculateSignalPosition(signal, edges);
        }
    }

    private void calculateSignalPosition(Signal signal, List<Edge> edges) {
        // Find the edge this signal controls
        Edge fromEdge = null;
        for (Edge edge : edges) {
            if (edge.getNetworkEdge().id.equals(signal.fromEdge)) {
                fromEdge = edge;
                break;
            }
        }
        
        if (fromEdge == null || fromEdge.getLanes().isEmpty()) {
            System.out.println("WARNING: Could not find edge for signal: " + signal.fromEdge);
            return;
        }

        // Map SUMO lane index to visual lane index
        // Edge has lanes: [dir1_0, dir1_1, ..., dir2_0, dir2_1, ...]
        // SUMO lane index maps to dir2 lanes (positive offset side)
        int numLanesPerDirection = fromEdge.getNetworkEdge().getNumLanes();
        int visualLaneIndex = numLanesPerDirection + signal.fromLaneIndex; // Map to dir2
        
        // Check if lane index is valid
        if (visualLaneIndex >= fromEdge.getLanes().size()) {
            System.out.println("WARNING: Lane index " + visualLaneIndex + 
                             " out of bounds for edge " + signal.fromEdge);
            return;
        }

        // Get the specific lane this signal controls
        Lane controlledLane = fromEdge.getLanes().get(visualLaneIndex);

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
            System.out.println("WARNING: Zero-length edge [" + signal.fromEdge + "]");
            signal.x = junction.getX();
            signal.y = junction.getY();
            return;
        }

        double dirX = dx / length;
        double dirY = dy / length;

        // Perpendicular vector for lateral offset (flipped to opposite side)
        double perpX = -dy / length;  // Negated to flip to opposite side
        double perpY = dx / length;   // Negated to flip to opposite side

        // Get the radius at the destination junction where the lane enters
        double toRadius = junction.getRadiusInDirection(-dirX, -dirY);

        // Calculate clipped end position (where the lane meets the junction - the stop line)
        double clippedEndX = edgeToX - toRadius * dirX;
        double clippedEndY = edgeToY - toRadius * dirY;

        // Position traffic light slightly before the stop line (1.5 meters forward for better clickability)
        double forwardOffset = 1.5; // meters forward from stop line
        double baseX = clippedEndX - forwardOffset * dirX;
        double baseY = clippedEndY - forwardOffset * dirY;

        // Position signal at the specific lane center
        // Each lane has a specific offset from the edge centerline
        double laneOffset = controlledLane.getOffsetFromCenter();
        
        // Get total edge width to position signals consistently
        double totalEdgeWidth = fromEdge.getNetworkEdge().getTotalWidth();

        // Apply lateral offset to position at the specific lane
        signal.x = baseX - totalEdgeWidth * perpX + perpX * laneOffset;
        signal.y = baseY - totalEdgeWidth * perpY + perpY * laneOffset;

        // Calculate rotation angle perpendicular to road (account for Y-axis flip)
        double roadAngle = Math.toDegrees(Math.atan2(dirY, dirX));
        signal.rotationAngle = -roadAngle + 90;
    }

    public void setState(String fullState) {
        this.currentState = fullState;
    }

    private Color getSignalColor(int linkIndex) {
        if (currentState == null || currentState.isEmpty()) {
            return GRAY;
        }

        if (linkIndex < 0 || linkIndex >= currentState.length()) {
            return GRAY;
        }

        char signal = currentState.charAt(linkIndex);

        // SUMO signal states:
        // G/g = green (G = priority, g = no priority)
        // y/Y = yellow
        // r/R = red
        // o/O = off/blinking
        switch (signal) {
            case 'G':
            case 'g':
                return GREEN;
            case 'y':
            case 'Y':
                return YELLOW;
            case 'r':
            case 'R':
                return RED;
            default:
                return GRAY;
        }
    }

    public Color getCurrentColor() {
        // For backward compatibility - return color of first signal
        if (signals.isEmpty()) {
            return GRAY;
        }
        return getSignalColor(signals.get(0).linkIndex);
    }

    public void render(GraphicsContext g, CoordinateTransform transform) {
        // Render each individual signal
        for (Signal signal : signals) {
            renderSignal(g, transform, signal);
        }
    }

    private void renderSignal(GraphicsContext g, CoordinateTransform transform, Signal signal) {
        double screenX = transform.worldToScreenX(signal.x);
        double screenY = transform.worldToScreenY(signal.y);
        
        // Signal line length proportional to lane width
        double laneWidth = 3.2; // Default SUMO lane width in meters
        double lineLength = Math.max(3, transform.worldToScreenSize(laneWidth * 0.7));
        double lineWidth = Math.max(2, lineLength * 0.15);

        // Get signal color
        Color activeColor = getSignalColor(signal.linkIndex);

        // Save graphics state
        g.save();
        g.translate(screenX, screenY);
        
        // Apply base rotation (perpendicular to road)
        g.rotate(signal.rotationAngle);
        
        // Now apply direction-specific rotation and draw line
        // Direction codes: s=straight, l=left, r=right, L=strong left, R=strong right, t=turn around
        double directionAngle = getDirectionAngle(signal.direction);
        g.rotate(directionAngle);
        
        // Draw the directional line indicator
        g.setStroke(activeColor);
        g.setLineWidth(lineWidth);
        g.setLineCap(javafx.scene.canvas.Canvas.class.isInstance(g.getCanvas()) 
            ? javafx.scene.shape.StrokeLineCap.ROUND 
            : javafx.scene.shape.StrokeLineCap.ROUND);
        
        // Draw line from center outward in the direction
        g.strokeLine(0, 0, 0, -lineLength);
        
        // Add arrow head for better direction indication
        double arrowSize = lineWidth * 1.5;
        g.strokeLine(0, -lineLength, -arrowSize, -lineLength + arrowSize);
        g.strokeLine(0, -lineLength, arrowSize, -lineLength + arrowSize);

        g.restore();
    }

    /**
     * Get the rotation angle in degrees for the connection direction
     * Based on SUMO direction codes
     */
    private double getDirectionAngle(String direction) {
        if (direction == null || direction.isEmpty()) {
            return 0; // Default to straight
        }
        
        switch (direction.charAt(0)) {
            case 's': // straight
                return 0;
            case 'l': // slight left
                return -30;
            case 'L': // left
                return -60;
            case 'r': // slight right
                return 30;
            case 'R': // right
                return 60;
            case 't': // turn around
                return 180;
            default:
                return 0;
        }
    }

    public boolean contains(double screenX, double screenY, CoordinateTransform transform) {
        // Check if click is on any signal
        for (Signal signal : signals) {
            if (containsSignal(screenX, screenY, transform, signal)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSignal(double screenX, double screenY, CoordinateTransform transform, Signal signal) {
        double lightScreenX = transform.worldToScreenX(signal.x);
        double lightScreenY = transform.worldToScreenY(signal.y);
        double laneWidth = 3.2;
        double lineLength = Math.max(3, transform.worldToScreenSize(laneWidth * 0.7));
        double clickRadius = Math.max(lineLength * 0.6, 5);

        double dx = screenX - lightScreenX;
        double dy = screenY - lightScreenY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        return distance <= clickRadius;
    }

    public void highlight(GraphicsContext g, CoordinateTransform transform, Color color) {
        // Highlight all signals for this traffic light
        for (Signal signal : signals) {
            highlightSignal(g, transform, signal, color);
        }
    }

    private void highlightSignal(GraphicsContext g, CoordinateTransform transform, Signal signal, Color color) {
        double screenX = transform.worldToScreenX(signal.x);
        double screenY = transform.worldToScreenY(signal.y);
        double laneWidth = 3.2;
        double lineLength = Math.max(3, transform.worldToScreenSize(laneWidth * 0.7));
        double radius = lineLength * 0.8;

        // Draw semi-transparent circle
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.2));
        g.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);

        // Draw dashed border
        g.setStroke(color);
        g.setLineWidth(2.0);
        g.setLineDashes(6, 3);
        g.strokeOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
        g.setLineDashes(null);
    }

    // Getters
    public String getJunctionId() {
        return junctionId;
    }

    public Junction getJunction() {
        return junction;
    }

    public String getCurrentState() {
        return currentState;
    }

    public List<Signal> getSignals() {
        return signals;
    }
    
    /**
     * Get list of link indices controlled by this traffic light, sorted in ascending order
     */
    public List<Integer> getLinkIndices() {
        List<Integer> indices = new ArrayList<>();
        for (Signal signal : signals) {
            indices.add(signal.linkIndex);
        }
        indices.sort(Integer::compareTo);
        return indices;
    }

    public boolean isManualMode() {
        return manualMode;
    }

    public void setManualMode(boolean manualMode) {
        this.manualMode = manualMode;
    }
}