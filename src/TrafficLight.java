import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Represents a traffic light controlling connections at a junction.
 * 
 * <p>
 * A traffic light consists of multiple signals, where each signal controls a
 * specific
 * lane-to-lane connection at the junction. Key features:
 * <ul>
 * <li>Multiple signals per junction (one per incoming edge)</li>
 * <li>Each signal displays a directional arrow (straight, left, right,
 * turn-around)</li>
 * <li>Signal colors: red, yellow, green, or gray (off)</li>
 * <li>Manual control mode allows overriding automatic SUMO control</li>
 * <li>Precise positioning at lane stop lines</li>
 * </ul>
 * 
 * <p>
 * The traffic light state is represented as a string where each character
 * controls
 * one signal: 'G'/'g' = green, 'y'/'Y' = yellow, 'r'/'R' = red, 'o' = off.
 * </p>
 * 
 * <p>
 * <b>OOP Architecture:</b>
 * </p>
 * <ul>
 * <li><b>Abstract Class:</b> Extends {@link Renderable} (IS-A
 * relationship)</li>
 * <li><b>Interface:</b> Implements {@link Updatable} (CAN-BE-UPDATED
 * capability)</li>
 * </ul>
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see TrafficManager
 * @see Junction
 * @see Renderable
 * @see Updatable
 */
public class TrafficLight extends Renderable implements Updatable {
    // Color constants
    private static final Color RED = Color.rgb(255, 0, 0);
    private static final Color YELLOW = Color.rgb(255, 255, 0);
    private static final Color GREEN = Color.rgb(0, 255, 0);
    private static final Color GRAY = Color.rgb(80, 80, 80);

    // Dimension constants
    private static final double DEFAULT_LANE_WIDTH = 3.2;
    private static final double FORWARD_OFFSET = 1.0;
    private static final double LINE_LENGTH_FACTOR = 0.5;
    private static final double LINE_WIDTH_FACTOR = 0.15;
    private static final double ARROW_SIZE_FACTOR = 1;
    private static final double CLICK_RADIUS_FACTOR = 0.6;
    private static final double HIGHLIGHT_RADIUS_FACTOR = 0.8;

    /**
     * Represents an individual signal controlling one lane-to-lane connection.
     * Each signal has its own position, direction, and link index in the traffic
     * light state.
     */
    public static class Signal {
        /** Source edge ID */
        public String fromEdge;
        /** Source lane index */
        public int fromLaneIndex;
        /** Destination edge ID */
        public String toEdge;
        /** Destination lane index */
        public int toLaneIndex;
        /** Index in the traffic light state string */
        public int linkIndex;
        /** Signal position X coordinate in world space */
        public double x;
        /** Signal position Y coordinate in world space */
        public double y;
        /** Rotation angle for the signal display */
        public double rotationAngle;
        /**
         * Direction code: 's'=straight, 'l'/'L'=left, 'r'/'R'=right, 't'=turn around
         */
        public String direction;

        /**
         * Constructs a new Signal.
         * 
         * @param fromEdge      Source edge ID
         * @param fromLaneIndex Source lane index
         * @param toEdge        Destination edge ID
         * @param toLaneIndex   Destination lane index
         * @param linkIndex     Index in traffic light state string
         * @param direction     Direction code for arrow display
         */
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

    private static final Logger logger = Logger.getLogger(TrafficLight.class.getName());
    
    private String junctionId;
    private Junction junction;
    private List<Signal> signals;
    private String currentState;
    private boolean manualMode = false;

    /**
     * Data class containing traffic light state information from SUMO.
     */
    public static class TrafficLightData {
        /** The state string (e.g., "GGrr") controlling all signals */
        public String state;
        /** List of controlled lane IDs */
        public List<String> controlledLanes;
        /** Number of links controlled */
        public int linkCount;

        /**
         * Constructs TrafficLightData.
         * 
         * @param state     The state string
         * @param lanes     List of controlled lanes
         * @param linkCount Number of links
         */
        public TrafficLightData(String state, List<String> lanes, int linkCount) {
            this.state = state;
            this.controlledLanes = lanes;
            this.linkCount = linkCount;
        }
    }

    /**
     * Constructs a new traffic light for a specific junction.
     * 
     * @param junctionId The junction ID this traffic light belongs to
     * @param junction   The Junction object for position reference
     */
    public TrafficLight(String junctionId, Junction junction) {
        this.junctionId = junctionId;
        this.junction = junction;
        this.signals = new ArrayList<>();
        this.currentState = "";
    }

    /**
     * Adds a signal to this traffic light.
     * 
     * @param signal The signal to add
     */
    public void addSignal(Signal signal) {
        this.signals.add(signal);
    }

    /**
     * Calculates the world positions for all signals based on their controlled
     * lanes.
     * Must be called after all signals are added and before rendering.
     * 
     * @param edges List of all edges in the network for position calculation
     */
    public void calculatePositions(List<Edge> edges) {
        for (Signal signal : signals) {
            calculateSignalPosition(signal, edges);
        }
    }

    /**
     * Calculates the position and rotation for a single signal.
     * Positions the signal at the stop line of the controlled lane.
     * 
     * @param signal The signal to position
     * @param edges  List of all edges for lookup
     */
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
            logger.warning("Could not find edge for signal: " + signal.fromEdge);
            return;
        }

        // SUMO lane index directly maps to visual lane index
        // Each edge contains only lanes for its specific direction
        int visualLaneIndex = signal.fromLaneIndex;

        // Check if lane index is valid
        if (visualLaneIndex < 0 || visualLaneIndex >= fromEdge.getLanes().size()) {
            logger.warning("Lane index " + visualLaneIndex +
                    " out of bounds for edge " + signal.fromEdge +
                    " (has " + fromEdge.getLanes().size() + " lanes)");
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
            logger.warning("Zero-length edge: " + signal.fromEdge);
            signal.x = junction.getX();
            signal.y = junction.getY();
            return;
        }

        double dirX = dx / length;
        double dirY = dy / length;

        // Perpendicular vector for lateral offset (flipped to opposite side)
        double perpX = -dy / length; // Negated to flip to opposite side
        double perpY = dx / length; // Negated to flip to opposite side

        // Get the radius at the destination junction where the lane enters
        double toRadius = junction.getRadiusInDirection(-dirX, -dirY);

        // Calculate clipped end position (where the lane meets the junction - the stop
        // line)
        double clippedEndX = edgeToX - toRadius * dirX;
        double clippedEndY = edgeToY - toRadius * dirY;

        // Position traffic light forward from the stop line (toward junction) for
        // better clickability
        double baseX = clippedEndX + FORWARD_OFFSET * dirX;
        double baseY = clippedEndY + FORWARD_OFFSET * dirY;

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

    /**
     * Sets the current state of all signals from a state string.
     * 
     * @param fullState State string where each character controls one signal
     */
    public void setState(String fullState) {
        this.currentState = fullState;
    }

    // ========== Updatable Interface Implementation ==========

    /**
     * Updates this traffic light's state from simulation data.
     * Implements the Updatable interface for polymorphic updates.
     * 
     * @param data TrafficLightData object from SUMO
     */
    @Override
    public void updateFromSimulation(Object data) {
        if (data instanceof TrafficLightData) {
            TrafficLightData tlData = (TrafficLightData) data;
            if (!manualMode) {
                setState(tlData.state);
            }
        } else if (data instanceof String) {
            if (!manualMode) {
                setState((String) data);
            }
        }
    }

    /**
     * Returns the junction ID for matching with simulation updates.
     * 
     * @return The junction identifier
     */
    @Override
    public String getUpdateId() {
        return junctionId;
    }

    /**
     * Returns the color for a signal based on its link index in the state string.
     * 
     * @param linkIndex The signal's link index
     * @return The color (RED, YELLOW, GREEN, or GRAY)
     */
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

    /**
     * Returns the color of the first signal (for backward compatibility).
     * 
     * @return The current color of the first signal
     */
    public Color getCurrentColor() {
        // For backward compatibility - return color of first signal
        if (signals.isEmpty()) {
            return GRAY;
        }
        return getSignalColor(signals.get(0).linkIndex);
    }

    /**
     * Renders all signals of this traffic light.
     * 
     * @param g         The graphics context to draw on
     * @param transform The coordinate transformation
     */
    @Override
    public void render(GraphicsContext g, CoordinateTransform transform) {
        // Render each individual signal
        for (Signal signal : signals) {
            renderSignal(g, transform, signal);
        }
    }

    /**
     * Renders a single signal with directional arrow.
     * 
     * @param g         The graphics context
     * @param transform The coordinate transformation
     * @param signal    The signal to render
     */
    private void renderSignal(GraphicsContext g, CoordinateTransform transform, Signal signal) {
        double screenX = transform.worldToScreenX(signal.x);
        double screenY = transform.worldToScreenY(signal.y);
        double lineLength = calculateLineLength(transform);
        double lineWidth = Math.max(2, lineLength * LINE_WIDTH_FACTOR);
        Color activeColor = getSignalColor(signal.linkIndex);

        g.save();
        g.translate(screenX, screenY);
        g.rotate(signal.rotationAngle);

        g.setStroke(activeColor);
        g.setLineWidth(lineWidth);
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        if (signal.direction != null && signal.direction.equals("t")) {
            drawTurnAroundArrow(g, lineLength, lineWidth);
        } else {
            drawDirectionalArrow(g, signal.direction, lineLength, lineWidth);
        }

        g.restore();
    }

    private double calculateLineLength(CoordinateTransform transform) {
        return Math.max(3, transform.worldToScreenSize(DEFAULT_LANE_WIDTH * LINE_LENGTH_FACTOR));
    }

    private void drawTurnAroundArrow(GraphicsContext g, double lineLength, double lineWidth) {
        double sideOffset = lineLength * 0.5;
        double arrowSize = lineWidth * ARROW_SIZE_FACTOR;

        // Draw horizontal line to the side
        g.strokeLine(0, 0, -sideOffset, 0);

        // Draw arrow pointing backward
        g.strokeLine(-sideOffset, 0, -sideOffset, lineLength);

        // Draw arrow head
        g.strokeLine(-sideOffset, lineLength, -sideOffset - arrowSize, lineLength - arrowSize);
        g.strokeLine(-sideOffset, lineLength, -sideOffset + arrowSize, lineLength - arrowSize);
    }

    private void drawDirectionalArrow(GraphicsContext g, String direction, double lineLength, double lineWidth) {
        double directionAngle = getDirectionAngle(direction);
        g.rotate(directionAngle);

        // Draw line from center outward
        g.strokeLine(0, 0, 0, -lineLength);

        // Draw arrow head
        double arrowSize = lineWidth * ARROW_SIZE_FACTOR;
        g.strokeLine(0, -lineLength, -arrowSize, -lineLength + arrowSize);
        g.strokeLine(0, -lineLength, arrowSize, -lineLength + arrowSize);
    }

    /**
     * Get the rotation angle in degrees for the connection direction
     * Based on SUMO direction codes
     */
    private double getDirectionAngle(String direction) {
        if (direction == null || direction.isEmpty()) {
            return 0;
        }

        switch (direction.charAt(0)) {
            case 's':
                return 0; // straight
            case 'l':
                return -30; // slight left
            case 'L':
                return -60; // left
            case 'r':
                return 30; // slight right
            case 'R':
                return 60; // right
            case 't':
                return 180; // turn around (not used when drawTurnAroundArrow is called)
            default:
                return 0;
        }
    }

    /**
     * Checks if a screen point is within any signal's clickable area.
     * 
     * @param screenX   The X coordinate in screen space
     * @param screenY   The Y coordinate in screen space
     * @param transform The coordinate transformation
     * @return true if the point hits any signal
     */
    @Override
    public boolean contains(double screenX, double screenY, CoordinateTransform transform) {
        // Check if click is on any signal
        for (Signal signal : signals) {
            if (containsSignal(screenX, screenY, transform, signal)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a screen point is within a specific signal's clickable area.
     * 
     * @param screenX   Screen X coordinate
     * @param screenY   Screen Y coordinate
     * @param transform Coordinate transformation
     * @param signal    The signal to check
     * @return true if within clickable radius
     */
    private boolean containsSignal(double screenX, double screenY, CoordinateTransform transform, Signal signal) {
        double lightScreenX = transform.worldToScreenX(signal.x);
        double lightScreenY = transform.worldToScreenY(signal.y);
        double lineLength = calculateLineLength(transform);
        double clickRadius = Math.max(lineLength * CLICK_RADIUS_FACTOR, 5);

        double dx = screenX - lightScreenX;
        double dy = screenY - lightScreenY;
        return Math.sqrt(dx * dx + dy * dy) <= clickRadius;
    }

    /**
     * Highlights all signals of this traffic light with the specified color.
     * Used for selection and hover effects.
     * 
     * @param g         The graphics context for rendering
     * @param transform The coordinate transformation
     * @param color     The highlight color
     */
    @Override
    public void highlight(GraphicsContext g, CoordinateTransform transform, Color color) {
        // Highlight all signals for this traffic light
        for (Signal signal : signals) {
            highlightSignal(g, transform, signal, color);
        }
    }

    private void highlightSignal(GraphicsContext g, CoordinateTransform transform, Signal signal, Color color) {
        double screenX = transform.worldToScreenX(signal.x);
        double screenY = transform.worldToScreenY(signal.y);
        double lineLength = calculateLineLength(transform);
        double radius = lineLength * HIGHLIGHT_RADIUS_FACTOR;

        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.2));
        g.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);

        g.setStroke(color);
        g.setLineWidth(2.0);
        g.setLineDashes(6, 3);
        g.strokeOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
        g.setLineDashes(null);
    }

    // Getters
    /**
     * Returns the junction ID this traffic light belongs to.
     * 
     * @return The junction ID
     */
    public String getJunctionId() {
        return junctionId;
    }

    /**
     * Returns the junction object for position reference.
     * 
     * @return The Junction
     */
    public Junction getJunction() {
        return junction;
    }

    /**
     * Returns the current state string controlling all signals.
     * 
     * @return The state string (e.g., "GGrr")
     */
    public String getCurrentState() {
        return currentState;
    }

    /**
     * Returns the list of all signals in this traffic light.
     * 
     * @return List of Signal objects
     */
    public List<Signal> getSignals() {
        return signals;
    }

    /**
     * Returns list of link indices controlled by this traffic light, sorted in
     * ascending order.
     * 
     * @return Sorted list of link indices
     */
    public List<Integer> getLinkIndices() {
        List<Integer> indices = new ArrayList<>();
        for (Signal signal : signals) {
            indices.add(signal.linkIndex);
        }
        indices.sort(Integer::compareTo);
        return indices;
    }

    /**
     * Returns whether this traffic light is in manual control mode.
     * 
     * @return true if in manual mode, false if automatic
     */
    public boolean isManualMode() {
        return manualMode;
    }

    /**
     * Sets the manual control mode for this traffic light.
     * When in manual mode, SUMO updates are ignored.
     * 
     * @param manualMode true for manual control, false for automatic
     */
    public void setManualMode(boolean manualMode) {
        this.manualMode = manualMode;
    }
}