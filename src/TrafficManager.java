import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Manages all traffic simulation objects including junctions, edges, vehicles, and traffic lights.
 * 
 * <p>This class serves as the central manager for the visual traffic network, handling:
 * <ul>
 *   <li>Creating visual objects from parsed network data</li>
 *   <li>Updating vehicle positions from SUMO simulation data</li>
 *   <li>Updating traffic light states from SUMO</li>
 *   <li>Rendering all simulation objects in proper layered order</li>
 *   <li>Hit detection for user interaction (clicking on objects)</li>
 *   <li>Highlighting selected and hovered objects</li>
 * </ul>
 * 
 * <p>The manager maintains several collections:
 * <ul>
 *   <li>Visual junctions and edges created from network topology</li>
 *   <li>Dynamic vehicles updated each simulation step</li>
 *   <li>Traffic lights with multiple signals per junction</li>
 *   <li>Index maps for fast lookup by ID</li>
 * </ul>
 * 
 * @author M A T^2 H Team
 * @version 2.0
 * @see NetworkParser
 * @see Junction
 * @see Edge
 * @see Vehicle
 * @see TrafficLight
 */
public class TrafficManager {
    private List<Junction> junctions;
    private List<Edge> edges;
    private Map<String, Vehicle> vehicles;
    private List<TrafficLight> trafficLights; // Changed to List for multiple lights per junction
    private Map<String, NetworkParser.Junction> junctionIndex;
    private Map<String, Junction> visualJunctionIndex;

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
    }

    /**
     * Initializes all visual objects from parsed network data.
     * Creates junctions, edges, lanes, and traffic lights from SUMO network topology.
     * 
     * @param network The parsed network data containing junctions, edges, and connections
     */
    public void initializeFromNetwork(NetworkParser.NetworkData network) {
        // Build junction index and create visual junctions
        for (NetworkParser.Junction junction : network.junctions) {
            junctionIndex.put(junction.id, junction);
            Junction visualJunction = new Junction(junction);
            junctions.add(visualJunction);
            visualJunctionIndex.put(junction.id, visualJunction);
        }
        System.out.println("Created " + junctions.size() + " junctions");

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
        System.out.println("Created " + edges.size() + " edges");

        // Initialize traffic lights from connections
        initializeTrafficLightsFromConnections(network.connections);
    }

    /**
     * Creates traffic light objects from network connections.
     * Groups connections by junction and incoming edge to create separate traffic lights
     * for each incoming road at a junction.
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
                System.out.println("WARNING: Junction not found for traffic light: " + junctionId);
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
                    conn.dir
                );
                trafficLight.addSignal(signal);
            }

            // Calculate positions for all signals
            trafficLight.calculatePositions(edges);
            trafficLights.add(trafficLight);
        }

        System.out.println("Initialized " + trafficLights.size() + " traffic lights (one per incoming edge) with " + 
                         connections.stream().filter(c -> c.tl != null).count() + " total signals");
    }

    /**
     * Updates vehicle positions and states from SUMO simulation data.
     * Creates new vehicles as they appear and removes vehicles that have left the simulation.
     * 
     * @param vehiclePositions Map from vehicle ID to array [x, y, angle, signals]
     */
    public void updateVehicles(Map<String, double[]> vehiclePositions) {
        // Update existing vehicles and create new ones
        for (Map.Entry<String, double[]> entry : vehiclePositions.entrySet()) {
            String vehicleId = entry.getKey();
            double[] position = entry.getValue();

            Vehicle vehicle = vehicles.get(vehicleId);
            if (vehicle == null) {
                // Create new vehicle
                vehicle = new Vehicle(vehicleId, position[0], position[1],
                        position.length > 2 ? position[2] : 0.0);
                vehicles.put(vehicleId, vehicle);
            } else {
                // Update existing vehicle
                vehicle.updatePosition(position);
            }
        }

        // Remove vehicles that are no longer in SUMO
        vehicles.entrySet().removeIf(entry -> !vehiclePositions.containsKey(entry.getKey()));
    }

    /**
     * Updates traffic light states from SUMO simulation data.
     * Only updates lights not in manual control mode.
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
     * Sets manual mode for ALL traffic lights at a junction.
     * This ensures the entire junction is synchronized in manual or automatic control.
     * 
     * @param junctionId The junction ID
     * @param manualMode true for manual control, false for automatic
     * @param state Optional state string to set (can be null)
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
     * Performs hit detection to find the simulation object at the given screen coordinates.
     * Checks layers in order: vehicles (top), traffic lights, junctions, lanes (bottom).
     * 
     * @param screenX The X coordinate in screen space
     * @param screenY The Y coordinate in screen space
     * @param transform The coordinate transformation
     * @return The object at that position, or null if none
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
     * Selected objects are highlighted in cyan, hovered in yellow.
     * 
     * @param g The graphics context
     * @param transform The coordinate transformation
     * @param selected The currently selected object (can be null)
     * @param hovered The currently hovered object (can be null)
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
        if (obj instanceof Vehicle) {
            ((Vehicle) obj).highlight(g, transform, color);
        } else if (obj instanceof Lane) {
            ((Lane) obj).highlight(g, transform, color);
        } else if (obj instanceof Junction) {
            ((Junction) obj).highlight(g, transform, color);
        } else if (obj instanceof TrafficLight) {
            ((TrafficLight) obj).highlight(g, transform, color);
        }
    }

    /**
     * Renders all simulation objects in proper layered order.
     * Order: edges (bottom), junctions, traffic lights, vehicles (top).
     * 
     * @param g The graphics context
     * @param transform The coordinate transformation
     */
    public void render(GraphicsContext g, CoordinateTransform transform) {
        // Render edges
        for (Edge edge : edges) {
            edge.render(g, transform);
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
     * Used for route selection when clicking on edges.
     * 
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @param transform Coordinate transformation
     * @return The edge ID, or null if no edge at that position
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
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
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
}