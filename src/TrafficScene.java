import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javafx.scene.canvas.GraphicsContext;

public class TrafficScene {
    private List<VisualEdge> edges;
    private Map<String, VisualVehicle> vehicles;
    private Map<String, NetworkParser.Junction> junctionIndex;
    
    public TrafficScene() {
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
                VisualEdge visualEdge = new VisualEdge(edge, from, to);
                edges.add(visualEdge);
            }
        }
    }
    
    public void updateVehicles(Map<String, double[]> vehiclePositions) {
        // Update existing vehicles and create new ones
        for (Map.Entry<String, double[]> entry : vehiclePositions.entrySet()) {
            String vehicleId = entry.getKey();
            double[] position = entry.getValue();
            
            VisualVehicle vehicle = vehicles.get(vehicleId);
            if (vehicle == null) {
                // Create new vehicle
                vehicle = new VisualVehicle(vehicleId, position[0], position[1], 
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
        for (VisualVehicle vehicle : vehicles.values()) {
            if (vehicle.contains(screenX, screenY, transform)) {
                return vehicle;
            }
        }
        
        // Check lanes
        for (VisualEdge edge : edges) {
            VisualLane lane = edge.getLaneAt(screenX, screenY, transform);
            if (lane != null) {
                return lane;
            }
        }
        
        // Check edges
        for (VisualEdge edge : edges) {
            if (edge.contains(screenX, screenY, transform)) {
                return edge;
            }
        }
        
        return null;
    }
    
    public void render(GraphicsContext g, CoordinateTransform transform) {
        // Render edges (roads and lane markings)
        for (VisualEdge edge : edges) {
            edge.render(g, transform);
        }
        
        // Render vehicles
        for (VisualVehicle vehicle : vehicles.values()) {
            vehicle.render(g, transform);
        }
    }
    
    // Getters
    public List<VisualEdge> getEdges() { return edges; }
    public Map<String, VisualVehicle> getVehicles() { return vehicles; }
}