import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.ArrayList;

public class TrafficLight {
    private String junctionId;
    private Junction junction;
    private String approachEdgeId;
    private double x, y;
    private String direction;
    private String currentState;
    private List<Integer> linkIndices; // for main light: straight + right turns
    private List<Integer> turnLinkIndices; // ArrowLight: left turns only
    private List<NetworkParser.Connection> connections; // Store connections for lane lookup
    
    // // Force control - separate for main and turn signals
    // private boolean mainForcedControl = false;
    // private String mainForcedState = null; // "green" or "red"
    // private boolean turnForcedControl = false;
    // private String turnForcedState = null; // "green" or "red"
    // private TraaSAdapter adapter = null;

    // Move TrafficLightData HERE
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
        this.turnLinkIndices = new ArrayList<>();
        this.currentState = "";
        this.connections = new ArrayList<>();
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
        
        // Find which lanes this TL controls by looking at link indices
        // Edge creates lanes: [0..numLanes-1] = Direction 1 (negative offset)
        //                     [numLanes..2*numLanes-1] = Direction 2 (positive offset)
        // SUMO fromLane indices are [0..numLanes-1]
        // We need to map SUMO lane index to the correct visual lane
        
        int numLanesPerDirection = fromEdge.getNetworkEdge().lanes.size();
        
        // Determine which direction the traffic flows: toward junction (Direction 2) or away (Direction 1)
        // Direction 2 lanes (positive offset) flow from fromX,fromY -> toX,toY (toward junction)
        // So for approaching traffic, we want Direction 2 lanes (indices numLanes to 2*numLanes-1)
        
        double avgLaneOffset = 0.0;
        int count = 0;
        
        // Get all controlled lanes from linkIndices and turnLinkIndices
        for (int linkIdx : linkIndices) {
            avgLaneOffset += getLaneOffsetForLink(linkIdx, fromEdge, numLanesPerDirection);
            count++;
        }
        for (int linkIdx : turnLinkIndices) {
            avgLaneOffset += getLaneOffsetForLink(linkIdx, fromEdge, numLanesPerDirection);
            count++;
        }
        
        if (count > 0) {
            avgLaneOffset /= count;
        }
        
        // Apply lateral offset
        this.x = baseX + perpX * avgLaneOffset;
        this.y = baseY + perpY * avgLaneOffset;

        // Calculate direction label
        this.direction = calculateDirectionName(dirX, dirY);
        
        System.out.println("  TL [" + approachEdgeId + "] positioned at (" + 
                          String.format("%.1f", x) + ", " + String.format("%.1f", y) + 
                          ") with offset " + String.format("%.1f", avgLaneOffset) + ", facing " + direction);
    }
    
    private double getLaneOffsetForLink(int linkIdx, Edge fromEdge, int numLanesPerDirection) {
        // Find the connection for this link to get the fromLane index
        for (NetworkParser.Connection conn : connections) {
            if (conn.tl != null && conn.tl.equals(junctionId) && 
                conn.from.equals(approachEdgeId) && conn.linkIndex == linkIdx) {
                
                int sumoLaneIdx = conn.fromLane;
                // Map SUMO lane index to Direction 2 visual lane (approaching traffic)
                int visualLaneIdx = numLanesPerDirection + sumoLaneIdx;
                
                if (visualLaneIdx < fromEdge.getLanes().size()) {
                    Lane lane = fromEdge.getLanes().get(visualLaneIdx);
                    
                    // Calculate offset from edge centerline
                    double laneX = lane.getCenterX2();
                    double laneY = lane.getCenterY2();
                    double edgeToX = fromEdge.getToX();
                    double edgeToY = fromEdge.getToY();
                    
                    // Get perpendicular distance
                    double dx = edgeToX - fromEdge.getFromX();
                    double dy = edgeToY - fromEdge.getFromY();
                    double len = Math.sqrt(dx * dx + dy * dy);
                    double perpX = dy / len;
                    double perpY = -dx / len;
                    
                    double offset = (laneX - edgeToX) * perpX + (laneY - edgeToY) * perpY;
                    return offset;
                }
                break;
            }
        }
        return 0.0;
    }
    private String calculateDirectionName(double dirX, double dirY) {
        double angle = Math.toDegrees(Math.atan2(dirY, dirX));
        if (angle < 0) angle += 360;
        if (angle < 45 || angle >= 315) return "E";
        if (angle < 135) return "N";
        if (angle < 225) return "W";
        return "S";
    }

    public void setPosition(double x, double y, String direction){
        this.x = x;
        this.y = y;
        this.direction = direction;
    }
    // Add a link index for main traffic light (straight/ right turns)
    public void addLinkIndex(int index){
        linkIndices.add(index);
    }
    // Add a link index for arrow light (left turns)
    public void addTurnLinkIndex(int index) {
        turnLinkIndices.add(index);
    }
    
    // Classify links based on connection directions from network XML
    public void classifyLinks(List<Integer> links, List<NetworkParser.Connection> connections) {
        this.connections = connections; // Store for later use in calculatePosition
        
        for (int linkIdx : links) {
            // Find direction for this link from connections
            String direction = null;
            for (NetworkParser.Connection conn : connections) {
                if (conn.tl != null && conn.tl.equals(junctionId) && conn.linkIndex == linkIdx) {
                    direction = conn.dir;
                    break;
                }
            }
            
            // dir="l" or dir="L" means left turn
            if (direction != null && (direction.equals("l") || direction.equals("L"))) {
                addTurnLinkIndex(linkIdx);  // Left turn arrow
            } else {
                addLinkIndex(linkIdx);  // Main light (straight/right/turn-around)
            }
        }
    }

    // public void setAdapter(TraaSAdapter adapter) {
    //     this.adapter = adapter;
    // }
    
    public void setState(String fullState) {
        this.currentState = fullState;
    }
    
    // // Main light control
    // public void forceMainGreen() {
    //     mainForcedControl = true;
    //     mainForcedState = "green";
    //     applyForcedState();
    // }
    
    // public void forceMainRed() {
    //     mainForcedControl = true;
    //     mainForcedState = "red";
    //     applyForcedState();
    // }
    
    // public void releaseMainControl() {
    //     mainForcedControl = false;
    //     mainForcedState = null;
    // }
    
    // // Turn signal control
    // public void forceTurnGreen() {
    //     turnForcedControl = true;
    //     turnForcedState = "green";
    //     applyForcedState();
    // }
    
    // public void forceTurnRed() {
    //     turnForcedControl = true;
    //     turnForcedState = "red";
    //     applyForcedState();
    // }
    
    // public void releaseTurnControl() {
    //     turnForcedControl = false;
    //     turnForcedState = null;
    // }
    
    // public void applyForcedState() {
    //     if ((!mainForcedControl && !turnForcedControl) || adapter == null) return;
        
    //     try {
    //         // Get current state of the entire junction
    //         String currentFullState = adapter.getTrafficLightState(junctionId);
    //         if (currentFullState == null || currentFullState.isEmpty()) return;
            
    //         // Create new state by only modifying this traffic light's forced links
    //         char[] stateArray = currentFullState.toCharArray();
            
    //         // Force main light links (straight/right) if controlled
    //         if (mainForcedControl && mainForcedState != null) {
    //             char forcedChar = "green".equals(mainForcedState) ? 'G' : 'r';
    //             for (int linkIdx : linkIndices) {
    //                 if (linkIdx < stateArray.length) {
    //                     stateArray[linkIdx] = forcedChar;
    //                 }
    //             }
    //         }
            
    //         // Force turn arrow links (left turns) if controlled
    //         if (turnForcedControl && turnForcedState != null) {
    //             char forcedChar = "green".equals(turnForcedState) ? 'G' : 'r';
    //             for (int linkIdx : turnLinkIndices) {
    //                 if (linkIdx < stateArray.length) {
    //                     stateArray[linkIdx] = forcedChar;
    //                 }
    //             }
    //         }
            
    //         String newState = new String(stateArray);
    //         adapter.setTrafficLightState(junctionId, newState);
    //     } catch (Exception e) {
    //         System.err.println("Error forcing traffic light state: " + e.getMessage());
    //     }
    // }
    
    // public boolean isMainForcedControl() {
    //     return mainForcedControl;
    // }
    
    // public String getMainForcedState() {
    //     return mainForcedState;
    // }
    
    // public boolean isTurnForcedControl() {
    //     return turnForcedControl;
    // }
    
    // public String getTurnForcedState() {
    //     return turnForcedState;
    // }
    
    // public boolean hasAnyForcedControl() {
    //     return mainForcedControl || turnForcedControl;
    // }
    
    // Get color for main traffic light (straight + right turns only)
    public Color getCurrentColor() {
        if (currentState == null || currentState.isEmpty() || linkIndices.isEmpty()) {
            return Color.GRAY;
        }

        boolean hasRed = false;
        boolean hasYellow = false;
        boolean hasGreen = false;

        // Check straight and right turn links
        for (int index : linkIndices) {
            if (index >= currentState.length()) continue;

            char signal = currentState.charAt(index);
            switch (signal) {
                case 'G': case 'g':
                    hasGreen = true;
                    break;
                case 'y': case 'Y':
                    hasYellow = true;
                    break;
                case 'r': case 'R':
                    hasRed = true;
                    break;
            }
        }
        // Most restrictive wins
        if (hasRed) return Color.rgb(255, 0, 0);
        if (hasYellow) return Color.rgb(255, 255, 0);
        if (hasGreen) return Color.rgb(0, 255, 0);
        return Color.GRAY;
    }
    // Get arrow light color (left turns only)
    public Color getYieldArrowColor() {
        if (currentState == null || currentState.isEmpty() || linkIndices.isEmpty()) {
            return Color.rgb(255, 0, 0);  // Red by default
        }
        
        // Check the left turn links
        for (int index : turnLinkIndices) {
            if (index >= currentState.length()) continue;
            
            char signal = currentState.charAt(index);
            if (signal == 'g' || signal == 'G') {
                return Color.rgb(0, 255,0); 
            }
        }
        return Color.rgb(255, 0, 0);  // Red arrow
    }
    
    /**
     * Check if this traffic light has turn links (always show arrow)
     * Turn links are typically the last 1-2 links in each approach
     */
    public boolean hasYieldSignals() {
        return !turnLinkIndices.isEmpty();
    }
    
    public void render(GraphicsContext g, CoordinateTransform transform) {
        double screenX = transform.worldToScreenX(x);
        double screenY = transform.worldToScreenY(y);
        double size = transform.worldToScreenSize(1.5);

        // Draw main traffic light (3-light)
        renderMainLight(g, screenX, screenY, size);
        
        // Draw yield arrow light if needed (to the left)
        if (hasYieldSignals()) {
            double arrowOffset = size * 1.2;  // Position to the left
            renderYieldArrow(g, screenX - arrowOffset, screenY, size * 0.6);
        }
    }
    
    /**
     * Render the main 3-light traffic signal
     */
    private void renderMainLight(GraphicsContext g, double screenX, double screenY, double size) {
        // Draw black box background
        g.setFill(Color.rgb(40, 40, 40));
        g.fillRect(screenX - size/2, screenY - size/2, size, size * 1.8);

        // Draw border
        g.setStroke(Color.rgb(80, 80, 80));
        g.setLineWidth(1);
        g.strokeRect(screenX - size/2, screenY - size/2, size, size * 1.8);

        // Get current signal color
        Color activeColor = getCurrentColor();

        // Draw three lights (red, yellow, green) vertically
        double lightRadius = size * 0.3;
        double spacing = size * 0.45;

        // Red light (top)
        g.setFill(activeColor.equals(Color.rgb(255, 0, 0))
            ? Color.rgb(255, 0, 0)
            : Color.rgb(60, 0, 0));
        g.fillOval(screenX - lightRadius, screenY - spacing, lightRadius * 2, lightRadius * 2);

        // Yellow light (middle)
        g.setFill(activeColor.equals(Color.rgb(255, 255, 0))
            ? Color.rgb(255, 255, 0)
            : Color.rgb(60, 60, 0));
        g.fillOval(screenX - lightRadius, screenY, lightRadius * 2, lightRadius * 2);

        // Green light (bottom)
        g.setFill(activeColor.equals(Color.rgb(0, 255, 0))
            ? Color.rgb(0, 255, 0)
            : Color.rgb(0, 60, 0));
        g.fillOval(screenX - lightRadius, screenY + spacing, lightRadius * 2, lightRadius * 2);
    }
    
    /**
     * Render the yield arrow light (2-light: red/green arrow)
     */
    private void renderYieldArrow(GraphicsContext g, double screenX, double screenY, double size) {
        // Draw black box background
        g.setFill(Color.rgb(40, 40, 40));
        g.fillRect(screenX - size/2, screenY - size/2, size, size * 1.5);

        // Draw border
        g.setStroke(Color.rgb(80, 80, 80));
        g.setLineWidth(1);
        g.strokeRect(screenX - size/2, screenY - size/2, size, size * 1.5);

        Color arrowColor = getYieldArrowColor();
        double arrowSize = size * 0.8;
        
        if (arrowColor.equals(Color.rgb(255, 0, 0))) {
            // Red X or circle
            g.setFill(Color.rgb(255, 0, 0));
            g.fillOval(screenX - arrowSize/2, screenY - arrowSize/2, arrowSize, arrowSize);
        } else if (arrowColor.equals(Color.rgb(0, 255, 0))) {
            // Green left arrow
            g.setFill(Color.rgb(0, 255, 0));
            drawLeftArrow(g, screenX, screenY, arrowSize);
        } else {
            // Gray
            g.setFill(Color.rgb(80, 80, 80));
            g.fillOval(screenX - arrowSize/2, screenY - arrowSize/2, arrowSize, arrowSize);
        }
    }
    
    /**
     * Draw a left-pointing arrow
     */
    private void drawLeftArrow(GraphicsContext g, double centerX, double centerY, double size) {
        double[] xPoints = {
            centerX + size * 0.3,   // Right tip
            centerX - size * 0.3,   // Left arrow point
            centerX + size * 0.3    // Right tip (bottom)
        };
        double[] yPoints = {
            centerY - size * 0.25,  // Top
            centerY,                 // Middle point
            centerY + size * 0.25   // Bottom
        };
        g.fillPolygon(xPoints, yPoints, 3);
        
        // Arrow shaft
        g.fillRect(centerX - size * 0.1, centerY - size * 0.1, size * 0.4, size * 0.2);
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
    public List<Integer> getTurnLinkIndices() { return turnLinkIndices; }
    public String getDirection() { return direction; }
    public double getX() { return x; }
    public double getY() { return y; }
}