import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javafx.scene.canvas.GraphicsContext;

public class TrafficManager {
    private List<Junction> junctions;
    private List<Edge> edges;
    private Map<String, Vehicle> vehicles;
    private Map<String, NetworkParser.Junction> junctionIndex;
    private Map<String, Junction> visualJunctionIndex;

    public TrafficManager() {
        this.junctions = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.vehicles = new HashMap<>();
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

    public Object getElementAt(double screenX, double screenY, CoordinateTransform transform) {
        // Check vehicles first (top layer)
        for (Vehicle vehicle : vehicles.values()) {
            if (vehicle.contains(screenX, screenY, transform)) {
                return vehicle;
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

    /*
     * public void render(GraphicsContext g, CoordinateTransform transform) {
     * // Render edges (roads and lane markings)
     * for (Edge edge : edges) {
     * edge.render(g, transform);
     * }
     * 
     * // Render junctions (between roads and vehicles for proper layering) and also
     * traffic lights
     * for (Junction junction : junctions) {
     * junction.render(g, transform);
     * 
     * }
     * 
     * // Render vehicles
     * for (Vehicle vehicle : vehicles.values()) {
     * vehicle.render(g, transform);
     * }
     * }
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
}