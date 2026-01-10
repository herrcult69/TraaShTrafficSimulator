import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;import java.util.logging.Logger;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Manages all traffic simulation objects including junctions, edges, vehicles,
 * and traffic lights.
 * Handles creation from network data, updates from SUMO, rendering, and hit
 * detection.
 * 
 * @author M A T^2 H Team
 * @version 2.0
 * @see NetworkParser
 */
public class TrafficManager {
    private static final Logger logger = Logger.getLogger(TrafficManager.class.getName());
    
    private List<Junction> junctions;
    private List<Edge> edges;
    private Map<String, Vehicle> vehicles;
    private List<TrafficLight> trafficLights; // Changed to List for multiple lights per junction
    private Map<String, NetworkParser.Junction> junctionIndex;
    private Map<String, Junction> visualJunctionIndex;
    
    // Congestion tracking
    private Map<String, CongestionHotspot> congestionHotspots;
    private boolean showCongestionOverlay;
    
    // Travel time tracking
    private List<Double> completedTravelTimes;
    private double currentSimTime;

    // Travel distance tracking
    private List<Double> completedTravelDistances;

    /**
     * Constructs a new TrafficManager with empty collections.
     */
    public TrafficManager() {
        this.junctions = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.vehicles = new HashMap<>();
        this.trafficLights = new ArrayList<>(); // Initialize traffic lights
        this.junctionIndex = new HashMap<>();
        this.visualJunctionIndex = new HashMap<>();
        this.congestionHotspots = new HashMap<>();
        this.showCongestionOverlay = false;
        this.completedTravelTimes = new ArrayList<>();
        this.completedTravelDistances = new ArrayList<>();
        this.currentSimTime = 0.0;
    }

    /**
     * Initializes visual objects from network data.
     * 
     * @param network Parsed network data
     */
    public void initializeFromNetwork(NetworkParser.NetworkData network) {
        // Build junction index and create visual junctions
        for (NetworkParser.Junction junction : network.junctions) {
            junctionIndex.put(junction.id, junction);
            Junction visualJunction = new Junction(junction);
            junctions.add(visualJunction);
            visualJunctionIndex.put(junction.id, visualJunction);
        }
        logger.info("Created " + junctions.size() + " junctions");

        // Create visual edges with junction references
        for (NetworkParser.Edge edge : network.edges) {
            NetworkParser.Junction from = junctionIndex.get(edge.from);
            NetworkParser.Junction to = junctionIndex.get(edge.to);
            Junction fromJunc = visualJunctionIndex.get(edge.from);
            Junction toJunc = visualJunctionIndex.get(edge.to);

            if (from != null && to != null && fromJunc != null && toJunc != null) {
                Edge visualEdge = new Edge(edge, from, to, fromJunc, toJunc);
                edges.add(visualEdge);
            }
        }
        logger.info("Created " + edges.size() + " edges with lanes");

        // Initialize traffic lights from connections
        initializeTrafficLightsFromConnections(network.connections);
    }

    /**
     * Creates traffic light objects from network connections.
     * 
     * @param connections List of network connections with traffic light assignments
     */
    private void initializeTrafficLightsFromConnections(List<NetworkParser.Connection> connections) {
        // Group connections by traffic light ID AND incoming edge
        // This creates separate TrafficLight objects for each incoming road
        Map<String, List<NetworkParser.Connection>> connectionsByEdge = new HashMap<>();

        for (NetworkParser.Connection conn : connections) {
            if (conn.tl != null && !conn.tl.isEmpty()) {
                // Key: "junctionId:fromEdge" - creates separate TL per incoming edge
                String key = conn.tl + ":" + conn.from;
                connectionsByEdge.computeIfAbsent(key, k -> new ArrayList<>()).add(conn);
            }
        }

        // Create traffic light objects - one per incoming edge at each junction
        for (Map.Entry<String, List<NetworkParser.Connection>> entry : connectionsByEdge.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split(":");
            String junctionId = parts[0];
            List<NetworkParser.Connection> tlConnections = entry.getValue();

            Junction junction = visualJunctionIndex.get(junctionId);
            if (junction == null) {
                logger.warning("Junction not found for traffic light: " + junctionId);
                continue;
            }

            TrafficLight trafficLight = new TrafficLight(junctionId, junction);

            // Create a signal for each connection from this specific edge
            for (NetworkParser.Connection conn : tlConnections) {
                TrafficLight.Signal signal = new TrafficLight.Signal(
                        conn.from,
                        conn.fromLane,
                        conn.to,
                        conn.toLane,
                        conn.linkIndex,
                        conn.dir);
                trafficLight.addSignal(signal);
            }

            // Calculate positions for all signals
            trafficLight.calculatePositions(edges);
            trafficLights.add(trafficLight);
        }

        logger.info("Initialized " + trafficLights.size() + " traffic lights with " +
                connections.stream().filter(c -> c.tl != null).count() + " total signals");
    }

    /**
     * Updates vehicle positions and states from SUMO simulation data.
     * 
     * @param vehiclePositions Map from vehicle ID to array [x, y, angle, signals]
     * @param simTime The current simulation time in seconds
     */
    public void updateVehicles(Map<String, double[]> vehiclePositions, double simTime) {
        this.currentSimTime = simTime;
        
        // Track which vehicles are leaving
        List<String> leavingVehicles = new ArrayList<>();
        for (String vehicleId : vehicles.keySet()) {
            if (!vehiclePositions.containsKey(vehicleId)) {
                leavingVehicles.add(vehicleId);
            }
        }
        
        // Mark exiting vehicles and record their travel times
        for (String vehicleId : leavingVehicles) {
            Vehicle vehicle = vehicles.get(vehicleId);
            if (vehicle != null && !vehicle.hasExited()) {
                vehicle.markAsExited(simTime);
                double travelTime = vehicle.getTravelTime();
                if (travelTime > 0) {
                    completedTravelTimes.add(travelTime);
                }
                double travelDistance = vehicle.getTotalDistance();
                if (travelDistance > 0) {
                    completedTravelDistances.add(travelDistance);
                }
            }
        }
        
        // Update existing vehicles and create new ones
        for (Map.Entry<String, double[]> entry : vehiclePositions.entrySet()) {
            String vehicleId = entry.getKey();
            double[] position = entry.getValue();

            Vehicle vehicle = vehicles.get(vehicleId);
            if (vehicle == null) {
                // Create new vehicle with entry time
                vehicle = new Vehicle(vehicleId, position[0], position[1],
                        position.length > 2 ? position[2] : 0.0, simTime);
                vehicles.put(vehicleId, vehicle);
            } else {
                // Update existing vehicle position and current time
                vehicle.updatePosition(position);
                vehicle.setCurrentTime(simTime);
            }
        }

        // Remove vehicles that are no longer in SUMO
        vehicles.entrySet().removeIf(entry -> !vehiclePositions.containsKey(entry.getKey()));
    }
    
    /**
     * Legacy method for backward compatibility. Uses simTime = 0.0.
     * 
     * @param vehiclePositions Map from vehicle ID to array [x, y, angle, signals]
     * @deprecated Use {@link #updateVehicles(Map, double)} instead
     */
    @Deprecated
    public void updateVehicles(Map<String, double[]> vehiclePositions) {
        updateVehicles(vehiclePositions, this.currentSimTime);
    }
    
    /**
     * Updates vehicle speed statistics from SUMO simulation data.
     * 
     * @param vehicleSpeeds Map from vehicle ID to speed in m/s
     */
    public void updateVehicleSpeeds(Map<String, Double> vehicleSpeeds) {
        for (Map.Entry<String, Double> entry : vehicleSpeeds.entrySet()) {
            String vehicleId = entry.getKey();
            Double speed = entry.getValue();
            
            Vehicle vehicle = vehicles.get(vehicleId);
            if (vehicle != null && speed != null) {
                vehicle.updateSpeed(speed);
            }
        }
    }
    
    /**
     * Updates edge statistics including vehicle counts and density.
     * 
     * @param vehicleEdges Map from vehicle ID to edge ID
     */
    public void updateEdgeStatistics(Map<String, String> vehicleEdges) {
        // Reset all edge counts
        for (Edge edge : edges) {
            edge.setVehicleCount(0);
        }
        
        // Count vehicles on each edge
        Map<String, Integer> edgeCounts = new java.util.HashMap<>();
        for (String edgeId : vehicleEdges.values()) {
            edgeCounts.put(edgeId, edgeCounts.getOrDefault(edgeId, 0) + 1);
        }
        
        // Update edge vehicle counts
        for (Map.Entry<String, Integer> entry : edgeCounts.entrySet()) {
            Edge edge = getEdgeById(entry.getKey());
            if (edge != null) {
                edge.setVehicleCount(entry.getValue());
            }
        }
    }
    
    /**
     * Updates congestion hotspot tracking based on current traffic conditions.
     * Analyzes vehicle density and speed on each edge to identify congestion.
     * 
     * @param vehicleEdges Map from vehicle ID to edge ID (for vehicle counting)
     * @param vehicleSpeeds Map from vehicle ID to speed in m/s
     */
    public void updateCongestionHotspots(Map<String, String> vehicleEdges, Map<String, Double> vehicleSpeeds) {
        // Reset speed statistics for all edges
        for (Edge edge : edges) {
            edge.resetSpeedStatistics();
        }
        
        // Collect speed samples for each edge
        for (Map.Entry<String, String> entry : vehicleEdges.entrySet()) {
            String vehicleId = entry.getKey();
            String edgeId = entry.getValue();
            Double speed = vehicleSpeeds.get(vehicleId);
            
            if (speed != null) {
                Edge edge = getEdgeById(edgeId);
                if (edge != null) {
                    edge.addSpeedSample(speed);
                }
            }
        }
        
        // Update or create congestion hotspots
        for (Edge edge : edges) {
            String edgeId = edge.getNetworkEdge().id;
            
            // Get or create hotspot for this edge
            CongestionHotspot hotspot = congestionHotspots.get(edgeId);
            if (hotspot == null) {
                hotspot = new CongestionHotspot(edge);
                congestionHotspots.put(edgeId, hotspot);
            }
            
            // Update metrics (retrieves speed and density directly from edge)
            hotspot.updateMetrics();
        }
        
        // Remove hotspots that are no longer congested
        congestionHotspots.entrySet().removeIf(entry -> !entry.getValue().isCongested());
    }

    /**
     * Updates traffic light states from SUMO simulation data.
     * 
     * @param tlData Map from junction ID to traffic light state data
     */
    public void updateTrafficLights(Map<String, TrafficLight.TrafficLightData> tlData) {
        if (tlData == null || tlData.isEmpty())
            return;

        // Update all physical traffic lights with the full state string
        for (Map.Entry<String, TrafficLight.TrafficLightData> entry : tlData.entrySet()) {
            String junctionId = entry.getKey();
            TrafficLight.TrafficLightData data = entry.getValue();

            // Update all traffic lights at this junction (skip if in manual mode)
            for (TrafficLight tl : trafficLights) {
                if (tl.getJunctionId().equals(junctionId) && !tl.isManualMode()) {
                    tl.setState(data.state);
                }
            }
        }
    }

    /**
     * Sets manual mode for all traffic lights at a junction.
     * 
     * @param junctionId The junction ID
     * @param manualMode true for manual control, false for automatic
     * @param state      Optional state string to set
     */
    public void setJunctionManualMode(String junctionId, boolean manualMode, String state) {
        for (TrafficLight tl : trafficLights) {
            if (tl.getJunctionId().equals(junctionId)) {
                tl.setManualMode(manualMode);
                if (state != null) {
                    tl.setState(state);
                }
            }
        }
    }

    /**
     * Finds the simulation object at the given screen coordinates.
     * 
     * @param screenX   The X coordinate in screen space
     * @param screenY   The Y coordinate in screen space
     * @param transform The coordinate transformation
     * @return The object at that position, or null
     */
    public Object getElementAt(double screenX, double screenY, CoordinateTransform transform) {
        // Check vehicles first (top layer)
        for (Vehicle vehicle : vehicles.values()) {
            if (vehicle.contains(screenX, screenY, transform)) {
                return vehicle;
            }
        }

        // Check traffic lights
        for (TrafficLight tl : trafficLights) {
            if (tl.contains(screenX, screenY, transform)) {
                return tl;
            }
        }

        // Check junctions
        for (Junction junction : junctions) {
            if (junction.contains(screenX, screenY, transform)) {
                return junction;
            }
        }

        // Check lanes
        for (Edge edge : edges) {
            Lane lane = edge.getLaneAt(screenX, screenY, transform);
            if (lane != null) {
                return lane;
            }
        }
        return null;
    }

    /**
     * Renders highlight overlays for selected and hovered objects.
     * 
     * @param g         The graphics context
     * @param transform The coordinate transformation
     * @param selected  The currently selected object
     * @param hovered   The currently hovered object
     */
    public void renderHighlight(GraphicsContext g, CoordinateTransform transform, Object selected, Object hovered) {
        if (selected != null) {
            highlightObject(g, transform, selected, Color.CYAN);
        }
        if (hovered != null && hovered != selected) {
            highlightObject(g, transform, hovered, Color.YELLOW);
        }
    }

    private void highlightObject(GraphicsContext g, CoordinateTransform transform, Object obj, Color color) {
        // Use polymorphism with Renderable abstract class
        if (obj instanceof Renderable) {
            ((Renderable) obj).highlight(g, transform, color);
        }
    }

    /**
     * Renders all simulation objects in layered order.
     * 
     * @param g         The graphics context
     * @param transform The coordinate transformation
     */
    public void render(GraphicsContext g, CoordinateTransform transform) {
        // Render edges
        for (Edge edge : edges) {
            edge.render(g, transform);
        }
        
        // Render congestion overlays if enabled
        if (showCongestionOverlay) {
            for (CongestionHotspot hotspot : congestionHotspots.values()) {
                hotspot.render(g, transform);
            }
        }

        // Render junctions
        for (Junction junction : junctions) {
            junction.render(g, transform);
        }

        // Render traffic lights
        for (TrafficLight trafficLight : trafficLights) {
            trafficLight.render(g, transform);
        }

        // Render vehicles
        for (Vehicle vehicle : vehicles.values()) {
            vehicle.render(g, transform);
        }
    }

    // Getters
    /**
     * Returns the list of all edges in the network.
     * 
     * @return List of Edge objects
     */
    public List<Edge> getEdges() {
        return edges;
    }

    /**
     * Returns the map of all active vehicles.
     * 
     * @return Map from vehicle ID to Vehicle object
     */
    public Map<String, Vehicle> getVehicles() {
        return vehicles;
    }

    /**
     * Returns the list of all junctions in the network.
     * 
     * @return List of Junction objects
     */
    public List<Junction> getJunctions() {
        return junctions;
    }

    /**
     * Returns the list of all traffic lights in the network.
     * 
     * @return List of TrafficLight objects
     */
    public List<TrafficLight> getTrafficLights() {
        return trafficLights;
    }

    /**
     * Returns the network edge ID at the given screen position.
     * 
     * @param screenX   Screen X coordinate
     * @param screenY   Screen Y coordinate
     * @param transform Coordinate transformation
     * @return The edge ID, or null
     */
    public String getEdgeIdAt(double screenX, double screenY, CoordinateTransform transform) {
        for (Edge edge : edges) {
            Lane lane = edge.getLaneAt(screenX, screenY, transform);
            if (lane != null) {
                // Return the network edge ID
                return edge.getNetworkEdge().id;
            }
        }
        return null;
    }

    /**
     * Returns the Edge object at the given screen position.
     * 
     * @param screenX   Screen X coordinate
     * @param screenY   Screen Y coordinate
     * @param transform Coordinate transformation
     * @return The Edge object, or null if none at that position
     */
    public Edge getEdgeAt(double screenX, double screenY, CoordinateTransform transform) {
        for (Edge edge : edges) {
            Lane lane = edge.getLaneAt(screenX, screenY, transform);
            if (lane != null) {
                return edge;
            }
        }
        return null;
    }

    /**
     * Finds an edge by its network ID.
     * 
     * @param edgeId The edge ID to search for
     * @return The Edge object, or null if not found
     */
    public Edge getEdgeById(String edgeId) {
        for (Edge edge : edges) {
            if (edge.getNetworkEdge().id.equals(edgeId)) {
                return edge;
            }
        }
        return null;
    }
    
    /**
     * Returns the map of all congestion hotspots currently tracked.
     * 
     * @return Map from edge ID to CongestionHotspot
     */
    public Map<String, CongestionHotspot> getCongestionHotspots() {
        return congestionHotspots;
    }
    
    /**
     * Toggles the congestion overlay visualization.
     * 
     * @param show true to show congestion overlays, false to hide
     */
    public void setShowCongestionOverlay(boolean show) {
        this.showCongestionOverlay = show;
    }
    
    /**
     * Returns whether congestion overlay is currently visible.
     * 
     * @return true if congestion overlay is enabled
     */
    public boolean isShowCongestionOverlay() {
        return showCongestionOverlay;
    }
    
    /**
     * Returns the top N most congested edges sorted by severity.
     * 
     * @param n Number of top hotspots to return
     * @return List of top congestion hotspots
     */
    public List<CongestionHotspot> getTopCongestionHotspots(int n) {
        return congestionHotspots.values().stream()
            .filter(CongestionHotspot::isCongested)
            .sorted((a, b) -> Double.compare(b.getCongestionScore(), a.getCongestionScore()))
            .limit(n)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Returns the list of all completed travel times (in seconds).
     * 
     * @return List of travel times for vehicles that have exited the simulation
     */
    public List<Double> getCompletedTravelTimes() {
        return new ArrayList<>(completedTravelTimes);
    }
    
    /**
     * Clears the travel time history.
     */
    public void clearTravelTimeHistory() {
        completedTravelTimes.clear();
    }
    
    /**
     * Returns the list of all completed travel distances (in meters).
     * 
     * @return List of travel distances for vehicles that have exited the simulation
     */
    public List<Double> getCompletedTravelDistances() {
        return new ArrayList<>(completedTravelDistances);
    }

    /**
     * Clears the travel distance history.
     */
    public void clearTravelDistanceHistory() {
        completedTravelDistances.clear();
    }

}