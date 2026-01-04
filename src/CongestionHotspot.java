import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a congestion hotspot in the traffic network.
 * 
 * <p>A congestion hotspot tracks areas where traffic density and low speeds
 * indicate congestion. The system analyzes:
 * <ul>
 *   <li>Vehicle density (vehicles per kilometer)</li>
 *   <li>Average speed on the edge</li>
 *   <li>Congestion severity level (1-5)</li>
 *   <li>Historical congestion duration</li>
 * </ul>
 * 
 * <p>Hotspots are classified by severity:
 * <ul>
 *   <li>Level 1: Light congestion (green)</li>
 *   <li>Level 2: Moderate congestion (yellow-green)</li>
 *   <li>Level 3: Heavy congestion (yellow)</li>
 *   <li>Level 4: Severe congestion (orange)</li>
 *   <li>Level 5: Critical congestion (red)</li>
 * </ul>
 *
 * @author M A T^2 H Team
 * @version 2.0 
 * @see Edge
 * @see TrafficManager
 */
public class CongestionHotspot {
    private Edge edge;
    private double congestionScore;
    private int severityLevel; // 1-5 (1=light, 5=critical)
    private double avgSpeed;
    private double density;
    private long congestionStartTime;
    private long congestionDuration; // in milliseconds
    
    // Thresholds for congestion detection
    private static final double HIGH_DENSITY_THRESHOLD = 20.0; // vehicles per km
    private static final double MODERATE_DENSITY_THRESHOLD = 10.0;
    private static final double LOW_SPEED_THRESHOLD = 5.0; // m/s (18 km/h)
    private static final double MODERATE_SPEED_THRESHOLD = 10.0; // m/s (36 km/h)
    
    /**
     * Constructs a new congestion hotspot for the specified edge.
     * 
     * @param edge The edge where congestion is detected
     */
    public CongestionHotspot(Edge edge) {
        this.edge = edge;
        this.congestionScore = 0.0;
        this.severityLevel = 0;
        this.avgSpeed = 0.0;
        this.density = 0.0;
        this.congestionStartTime = System.currentTimeMillis();
        this.congestionDuration = 0;
    }
    
    /**
     * Updates the congestion metrics and recalculates severity.
     * Retrieves current speed and density data directly from the edge.
     */
    public void updateMetrics() {
        this.avgSpeed = edge.getAverageSpeed();
        this.density = edge.getVehicleDensity();
        
        // Calculate congestion score (0-100)
        // Higher score = worse congestion
        double speedFactor = 0.0;
        if (avgSpeed < LOW_SPEED_THRESHOLD) {
            speedFactor = 1.0 - (avgSpeed / LOW_SPEED_THRESHOLD);
        } else if (avgSpeed < MODERATE_SPEED_THRESHOLD) {
            speedFactor = 0.5 * (1.0 - (avgSpeed - LOW_SPEED_THRESHOLD) / (MODERATE_SPEED_THRESHOLD - LOW_SPEED_THRESHOLD));
        }
        
        double densityFactor = 0.0;
        if (density > HIGH_DENSITY_THRESHOLD) {
            densityFactor = 1.0;
        } else if (density > MODERATE_DENSITY_THRESHOLD) {
            densityFactor = (density - MODERATE_DENSITY_THRESHOLD) / (HIGH_DENSITY_THRESHOLD - MODERATE_DENSITY_THRESHOLD);
        }
        
        // Weighted combination: speed is more important
        this.congestionScore = (speedFactor * 0.4 + densityFactor * 0.6) * 100.0;
        
        // Update severity level
        if (congestionScore >= 80) {
            severityLevel = 5; // Critical
        } else if (congestionScore >= 60) {
            severityLevel = 4; // Severe
        } else if (congestionScore >= 40) {
            severityLevel = 3; // Heavy
        } else if (congestionScore >= 20) {
            severityLevel = 2; // Moderate
        } else if (congestionScore >= 10) {
            severityLevel = 1; // Light
        } else {
            severityLevel = 0; // No congestion
        }
        
        // Update duration
        if (severityLevel > 0) {
            congestionDuration = System.currentTimeMillis() - congestionStartTime;
        } else {
            congestionStartTime = System.currentTimeMillis();
            congestionDuration = 0;
        }
    }
    
    /**
     * Checks if this location currently has congestion.
     * 
     * @return true if severity level is greater than 0
     */
    public boolean isCongested() {
        return severityLevel > 0;
    }
    
    /**
     * Returns the color representing the congestion severity.
     * 
     * @return Color from green (light) to red (critical)
     */
    public Color getSeverityColor() {
        switch (severityLevel) {
            case 5: return Color.rgb(220, 20, 60);    // Crimson red
            case 4: return Color.rgb(255, 140, 0);    // Dark orange
            case 3: return Color.rgb(255, 215, 0);    // Gold
            case 2: return Color.rgb(173, 255, 47);   // Yellow-green
            case 1: return Color.rgb(50, 205, 50);    // Lime green
            default: return Color.GREEN;
        }
    }
    
    /**
     * Returns a text description of the severity level.
     * 
     * @return Severity description string
     */
    public String getSeverityDescription() {
        switch (severityLevel) {
            case 5: return "CRITICAL";
            case 4: return "SEVERE";
            case 3: return "HEAVY";
            case 2: return "MODERATE";
            case 1: return "LIGHT";
            default: return "NONE";
        }
    }
    
    /**
     * Renders the congestion hotspot as a visual overlay on the edge.
     * Uses color-coded highlighting to show congestion severity.
     * 
     * @param g The graphics context to draw on
     * @param transform The coordinate transformation
     */
    public void render(GraphicsContext g, CoordinateTransform transform) {
        if (!isCongested()) {
            return;
        }
        
        // Get edge geometry
        double fromX = edge.getFromX();
        double fromY = edge.getFromY();
        double toX = edge.getToX();
        double toY = edge.getToY();
        
        // Calculate direction
        double dx = toX - fromX;
        double dy = toY - fromY;
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length < 0.001) return;
        
        double dirX = dx / length;
        double dirY = dy / length;
        
        // Clip at junction boundaries
        Junction fromJunction = edge.getFromJunction();
        Junction toJunction = edge.getToJunction();
        
        double fromRadius = fromJunction != null ? fromJunction.getRadiusInDirection(dirX, dirY) : 0;
        double toRadius = toJunction != null ? toJunction.getRadiusInDirection(-dirX, -dirY) : 0;
        
        double startX = fromX + fromRadius * dirX;
        double startY = fromY + fromRadius * dirY;
        double endX = toX - toRadius * dirX;
        double endY = toY - toRadius * dirY;
        
        // Calculate perpendicular offset
        double perpX = dy / length;
        double perpY = -dx / length;
        double totalWidth = edge.getNetworkEdge().getTotalWidth();
        double halfWidth = totalWidth / 2.0;
        
        double offsetStartX = startX + halfWidth * perpX;
        double offsetStartY = startY + halfWidth * perpY;
        double offsetEndX = endX + halfWidth * perpX;
        double offsetEndY = endY + halfWidth * perpY;
        
        // Convert to screen coordinates
        double x1 = transform.worldToScreenX(offsetStartX);
        double y1 = transform.worldToScreenY(offsetStartY);
        double x2 = transform.worldToScreenX(offsetEndX);
        double y2 = transform.worldToScreenY(offsetEndY);
        
        double screenWidth = transform.worldToScreenSize(totalWidth);
        
        // Draw congestion overlay with pulsing effect for severe congestion
        Color severityColor = getSeverityColor();
        double alpha = severityLevel >= 4 ? 0.7 : 0.5;
        
        // Outer glow for higher severity
        if (severityLevel >= 3) {
            g.setStroke(Color.color(severityColor.getRed(), severityColor.getGreen(), 
                                   severityColor.getBlue(), alpha * 0.3));
            g.setLineWidth(screenWidth + 8);
            g.strokeLine(x1, y1, x2, y2);
        }
        
        // Main congestion indicator
        g.setStroke(Color.color(severityColor.getRed(), severityColor.getGreen(), 
                               severityColor.getBlue(), alpha));
        g.setLineWidth(screenWidth + 2);
        g.strokeLine(x1, y1, x2, y2);
    }
    
    // Getters
    public Edge getEdge() {
        return edge;
    }
    
    public double getCongestionScore() {
        return congestionScore;
    }
    
    public int getSeverityLevel() {
        return severityLevel;
    }
    
    public double getAvgSpeed() {
        return avgSpeed;
    }
    
    public double getDensity() {
        return density;
    }
    
    public long getCongestionDuration() {
        return congestionDuration;
    }
    
    public String getEdgeId() {
        return edge.getNetworkEdge().id;
    }
    
    /**
     * Returns a formatted string with detailed congestion information.
     * 
     * @return Detailed congestion info string
     */
    public String getDetailedInfo() {
        long durationSeconds = congestionDuration / 1000;
        return String.format(
            "Edge: %s\n" +
            "Severity: %s (Level %d)\n" +
            "Score: %.1f/100\n" +
            "Avg Speed: %.1f m/s (%.1f km/h)\n" +
            "Density: %.1f veh/km\n" +
            "Vehicles: %d\n" +
            "Duration: %d:%02d",
            getEdgeId(),
            getSeverityDescription(),
            severityLevel,
            congestionScore,
            avgSpeed, avgSpeed * 3.6,
            density,
            edge.getVehicleCount(),
            durationSeconds / 60, durationSeconds % 60
        );
    }
}
