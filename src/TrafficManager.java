import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import de.tudresden.sumo.objects.SumoLinkList;
import de.tudresden.sumo.objects.SumoLink;

public class TrafficManager {
    private List<Junction> junctions;
    private List<Edge> edges;
    private Map<String, Vehicle> vehicles;
    private List<TrafficLight> trafficLights; // Changed to List for multiple lights per junction
    private Map<String, NetworkParser.Junction> junctionIndex;
    private Map<String, Junction> visualJunctionIndex;

    public TrafficManager() {
        this.junctions = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.vehicles = new HashMap<>();
        this.trafficLights = new ArrayList<>(); // Initialize traffic lights
        this.junctionIndex = new HashMap<>();
        this.visualJunctionIndex = new HashMap<>();
    }

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
            String fromEdge = parts[1];
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
     * This method is now deprecated - traffic lights are initialized from network connections
     * Keep it for backward compatibility but it won't create new traffic lights
     */
    @Deprecated
    public void initializeTrafficLightsFromSUMO(TraaSAdapter adapter) {
        System.out.println("Traffic lights are now initialized from network connections in initializeFromNetwork()");
    }

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
     * Set manual mode for ALL traffic lights at a junction
     * This ensures the entire junction is synchronized
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
    public List<Edge> getEdges() {
        return edges;
    }

    public Map<String, Vehicle> getVehicles() {
        return vehicles;
    }

    public List<Junction> getJunctions() {
        return junctions;
    }

    public List<TrafficLight> getTrafficLights() {
        return trafficLights;
    }
}