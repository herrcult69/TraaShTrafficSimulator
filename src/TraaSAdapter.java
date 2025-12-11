
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Route;
import de.tudresden.sumo.cmd.Edge;
import de.tudresden.sumo.objects.SumoLinkList;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoStringList;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class TraaSAdapter {
    private final SumoTraciConnection conn;

    public TraaSAdapter(SumoTraciConnection conn) {
        this.conn = conn;
    }

    public double getSimulationTime() throws Exception {
        return (double) conn.do_job_get(Simulation.getTime());
    }

    public List<String> getVehicleIds() throws Exception {
        return (List<String>) conn.do_job_get(Vehicle.getIDList());
    }

    public double[] getVehiclePosition(String id) throws Exception {
        SumoPosition2D p = (SumoPosition2D) conn.do_job_get(Vehicle.getPosition(id));
        return new double[] { p.x, p.y };
    }

    public double getVehicleAngle(String id) throws Exception {
        // Angle in degrees as provided by SUMO (0 = east, 90 = north)
        return ((Number) conn.do_job_get(Vehicle.getAngle(id))).doubleValue();
    }

    public double getVehicleSpeed(String id) throws Exception {
        return ((Number) conn.do_job_get(Vehicle.getSpeed(id))).doubleValue();
    }

    public int getVehicleSignals(String id) throws Exception {
        return ((Number) conn.do_job_get(Vehicle.getSignals(id))).intValue();
    }

    // Get Traffic Light Ids
    public List<String> getTrafficLightIds() throws Exception {
        return (List<String>) conn.do_job_get(Trafficlight.getIDList());
    }

    // get the state
    public String getTrafficLightState(String tlId) throws Exception {
        return (String) conn.do_job_get(Trafficlight.getRedYellowGreenState(tlId));
    }

    // set the state
    public void setTrafficLightState(String tlId, String state) throws Exception {
        conn.do_job_set(Trafficlight.setRedYellowGreenState(tlId, state));
    }

    // set the program (return to automatic control)
    public void setTrafficLightProgram(String tlId, String programId) throws Exception {
        conn.do_job_set(Trafficlight.setProgram(tlId, programId));
    }

    public SumoLinkList getControlledLinks(String trafficLightId) throws Exception {
        return (SumoLinkList) conn.do_job_get(
                Trafficlight.getControlledLinks(trafficLightId));
    }

    // Vehicle Injection and Route Management

    /**
     * Add a new route to SUMO
     * 
     * @param routeId Unique route identifier
     * @param edges   List of edge IDs that form the route
     */
    public void addRoute(String routeId, List<String> edges) throws Exception {
        SumoStringList edgeList = new SumoStringList();
        for (String edge : edges) {
            edgeList.add(edge);
        }
        conn.do_job_set(Route.add(routeId, edgeList));
    }

    /**
     * Add a new vehicle to the simulation
     * 
     * @param vehicleId    Unique vehicle identifier
     * @param routeId      The route ID for this vehicle to follow
     * @param vehicleClass The vehicle class (passenger, truck, bus, motorcycle,
     *                     emergency)
     */
    public void addVehicle(String vehicleId, String routeId, String vehicleClass)
            throws Exception {
        // Add vehicle with the specified route
        // Use "DEFAULT_VEHTYPE" as the type - SUMO's built-in default vehicle type
        // The vehicle class is already encoded in the ID prefix for our visual rendering
        double currentTime = getSimulationTime();
        conn.do_job_set(Vehicle.add(
                vehicleId,              // vehicle ID
                "DEFAULT_VEHTYPE",      // use SUMO's default vehicle type
                routeId,                // route ID
                (int) currentTime + 1,  // depart time (next simulation step)
                0.0,                    // depart position (0 = start of route)
                0.0,                    // depart speed (0 = use default)
                (byte) 0                // depart lane (0 = first lane)
        ));
    }

    /**
     * Get list of all edge IDs in the network
     */
    public List<String> getEdgeIds() throws Exception {
        return (List<String>) conn.do_job_get(Edge.getIDList());
    }

    /**
     * Get list of all route IDs
     */
    public List<String> getRouteIds() throws Exception {
        return (List<String>) conn.do_job_get(Route.getIDList());
    }

    /**
     * Find a valid route between two edges using SUMO's routing
     * 
     * @param fromEdge Starting edge ID
     * @param toEdge   Destination edge ID
     * @return List of edge IDs forming the route, or null if no route found
     */
    public List<String> findRoute(String fromEdge, String toEdge) throws Exception {
        // Use Simulation.findRoute to get a valid path
        de.tudresden.sumo.objects.SumoStage stage = (de.tudresden.sumo.objects.SumoStage) conn
                .do_job_get(Simulation.findRoute(fromEdge, toEdge, "DEFAULT_VEHTYPE",
                        getSimulationTime(), 0));

        // Extract edge IDs from the stage
        if (stage != null && stage.edges != null && !stage.edges.isEmpty()) {
            return new ArrayList<>(stage.edges);
        }
        return null;
    }

}
