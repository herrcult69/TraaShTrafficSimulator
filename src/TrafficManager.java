import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javafx.scene.canvas.GraphicsContext;

public class TrafficManager {
    private List<Edge> edges;
    private Map<String, Vehicle> vehicles;
    private Map<String, NetworkParser.Junction> junctionIndex;
    
    public TrafficManager() {
        this.edges = new ArrayList<>();
        this.vehicles = new HashMap<>();
        this.junctionIndex = new HashMap<>();
    }
    
    public void initializeFromNetwork(NetworkParser.NetworkData network) {
        // Build junction index
        for (NetworkParser.Junction junction : network.junctions) {
            junctionIndex.put(junction.id, junction);
        }
        
        // Create visual edges
        for (NetworkParser.Edge edge : network.edges) {
            NetworkParser.Junction from = junctionIndex.get(edge.from);
            NetworkParser.Junction to = junctionIndex.get(edge.to);
            if (from != null && to != null) {
                Edge visualEdge = new Edge(edge, from, to);
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
        
        // Check edges
        for (Edge edge : edges) {
            if (edge.contains(screenX, screenY, transform)) {
                return edge;
            }
        }
        
        return null;
    }
    
    public void render(GraphicsContext g, CoordinateTransform transform) {
        // Render edges (roads and lane markings)
        for (Edge edge : edges) {
            edge.render(g, transform);
        }
        
        // Render vehicles
        for (Vehicle vehicle : vehicles.values()) {
            vehicle.render(g, transform);
        }
    }
    
    // Getters
    public List<Edge> getEdges() { return edges; }
    public Map<String, Vehicle> getVehicles() { return vehicles; }
}