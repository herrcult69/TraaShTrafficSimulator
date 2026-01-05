
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

/**
 * Adapter class for communicating with SUMO via TraCI protocol.
 * Provides simplified methods for querying simulation state and controlling
 * traffic.
 * 
 * @author M A T^2 H Team
 * @version 2.0
 * @see SimulationRunner
 */
@SuppressWarnings("unchecked")
public class TraaSAdapter {
    private final SumoTraciConnection conn;

    /**
     * Constructs a new TraaSAdapter for the given TraCI connection.
     * 
     * @param conn The active TraCI connection to SUMO
     */
    public TraaSAdapter(SumoTraciConnection conn) {
        this.conn = conn;
    }

    /**
     * Returns the current simulation time in seconds.
     * 
     * @return The simulation time
     * @throws Exception if TraCI communication fails
     */
    public double getSimulationTime() throws Exception {
        return (double) conn.do_job_get(Simulation.getTime());
    }

    /**
     * Returns the list of all vehicle IDs currently in the simulation.
     * 
     * @return List of vehicle identifiers
     * @throws Exception if TraCI communication fails
     */
    public List<String> getVehicleIds() throws Exception {
        return (List<String>) conn.do_job_get(Vehicle.getIDList());
    }

    /**
     * Returns the 2D position of a vehicle in world coordinates.
     * 
     * @param id The vehicle identifier
     * @return Array containing [x, y] coordinates in meters
     * @throws Exception if TraCI communication fails or vehicle doesn't exist
     */
    public double[] getVehiclePosition(String id) throws Exception {
        SumoPosition2D p = (SumoPosition2D) conn.do_job_get(Vehicle.getPosition(id));
        return new double[] { p.x, p.y };
    }

    /**
     * Returns the orientation angle of a vehicle.
     * 
     * @param id The vehicle identifier
     * @return Angle in degrees
     * @throws Exception if TraCI communication fails
     */
    public double getVehicleAngle(String id) throws Exception {
        // Angle in degrees as provided by SUMO (0 = east, 90 = north)
        return ((Number) conn.do_job_get(Vehicle.getAngle(id))).doubleValue();
    }

    /**
     * Returns the current speed of a vehicle.
     * 
     * @param id The vehicle identifier
     * @return Speed in meters per second
     * @throws Exception if TraCI communication fails or vehicle doesn't exist
     */
    public double getVehicleSpeed(String id) throws Exception {
        return ((Number) conn.do_job_get(Vehicle.getSpeed(id))).doubleValue();
    }

    /**
     * Returns the waiting time of a vehicle in seconds.
     * 
     * @param id The vehicle identifier
     * @return Waiting time in seconds
     * @throws Exception if TraCI communication fails
     */
    public double getVehicleWaitingTime(String id) throws Exception {
        return ((Number) conn.do_job_get(Vehicle.getWaitingTime(id))).doubleValue();
    }

    /**
     * Returns the signal state of a vehicle (turn signals, brake lights).
     * 
     * @param id The vehicle identifier
     * @return Signal state as an integer bit field
     * @throws Exception if TraCI communication fails
     */
    public int getVehicleSignals(String id) throws Exception {
        return ((Number) conn.do_job_get(Vehicle.getSignals(id))).intValue();
    }

    /**
     * Returns the list of all traffic light junction IDs in the network.
     * 
     * @return List of traffic light identifiers
     * @throws Exception if TraCI communication fails
     */
    public List<String> getTrafficLightIds() throws Exception {
        return (List<String>) conn.do_job_get(Trafficlight.getIDList());
    }

    /**
     * Returns the current state of a traffic light.
     * 
     * @param tlId The traffic light identifier
     * @return State string (e.g., "GrGr")
     * @throws Exception if TraCI communication fails
     */
    public String getTrafficLightState(String tlId) throws Exception {
        return (String) conn.do_job_get(Trafficlight.getRedYellowGreenState(tlId));
    }

    /**
     * Sets the state of a traffic light manually.
     * 
     * @param tlId  The traffic light identifier
     * @param state State string (e.g., "GGrr")
     * @throws Exception if TraCI communication fails
     */
    public void setTrafficLightState(String tlId, String state) throws Exception {
        conn.do_job_set(Trafficlight.setRedYellowGreenState(tlId, state));
    }

    /**
     * Sets the traffic light program.
     * 
     * @param tlId      The traffic light identifier
     * @param programId The program ID
     * @throws Exception if TraCI communication fails
     */
    public void setTrafficLightProgram(String tlId, String programId) throws Exception {
        conn.do_job_set(Trafficlight.setProgram(tlId, programId));
    }

    /**
     * Returns the list of links (connections) controlled by a traffic light.
     * 
     * @param trafficLightId The traffic light (junction) identifier
     * @return List of controlled links
     * @throws Exception if TraCI communication fails
     */
    public SumoLinkList getControlledLinks(String trafficLightId) throws Exception {
        return (SumoLinkList) conn.do_job_get(
                Trafficlight.getControlledLinks(trafficLightId));
    }

    // Vehicle Injection and Route Management

    /**
     * Adds a new route to the simulation.
     * 
     * @param routeId Unique route identifier
     * @param edges   List of edge IDs
     * @throws Exception if TraCI communication fails
     */
    public void addRoute(String routeId, List<String> edges) throws Exception {
        SumoStringList edgeList = new SumoStringList();
        for (String edge : edges) {
            edgeList.add(edge);
        }
        conn.do_job_set(Route.add(routeId, edgeList));
    }

    /**
     * Adds a new vehicle to the simulation.
     * 
     * @param vehicleId    Unique vehicle identifier
     * @param routeId      The route ID
     * @param vehicleClass The vehicle class
     * @throws Exception if TraCI communication fails
     */
    public void addVehicle(String vehicleId, String routeId, String vehicleClass)
            throws Exception {
        addVehicle(vehicleId, routeId, vehicleClass, 0.0);
    }

        /**
         * Adds a new vehicle to the simulation with an explicit departure speed.
         *
         * @param vehicleId Unique vehicle identifier
         * @param routeId The route ID for this vehicle to follow
         * @param vehicleClass The vehicle class (passenger, truck, bus, motorcycle, emergency)
         * @param departSpeed Departure speed in m/s (0 = use default)
         * @throws Exception if TraCI communication fails, route doesn't exist, or vehicle ID is duplicate
         */
        public void addVehicle(String vehicleId, String routeId, String vehicleClass, double departSpeed)
            throws Exception {
        // Add vehicle with the specified route
        // Use "DEFAULT_VEHTYPE" as the type - SUMO's built-in default vehicle type
        // The vehicle class is already encoded in the ID prefix for our visual
        // rendering
        double currentTime = getSimulationTime();
        double speed = (Double.isFinite(departSpeed) && departSpeed >= 0.0) ? departSpeed : 0.0;

        conn.do_job_set(Vehicle.add(
                vehicleId,              // vehicle ID
                "DEFAULT_VEHTYPE",      // use SUMO's default vehicle type
                routeId,                // route ID
                (int) currentTime + 1,  // depart time (next simulation step)
                0.0,                    // depart position (0 = start of route)
                speed,                  // depart speed
                (byte) 0                // depart lane (0 = first lane)
        ));
        }

    /**
     * Returns the list of all edge IDs in the network.
     * 
     * @return List of edge identifiers
     * @throws Exception if TraCI communication fails
     */
    public List<String> getEdgeIds() throws Exception {
        return (List<String>) conn.do_job_get(Edge.getIDList());
    }

    /**
     * Returns the list of all route IDs currently defined in the simulation.
     * 
     * @return List of route identifiers
     * @throws Exception if TraCI communication fails
     */
    public List<String> getRouteIds() throws Exception {
        return (List<String>) conn.do_job_get(Route.getIDList());
    }

    /**
     * Finds a valid route between two edges using SUMO's routing.
     * 
     * @param fromEdge Starting edge ID
     * @param toEdge   Destination edge ID
     * @return List of edge IDs forming the route, or null
     * @throws Exception if TraCI communication fails
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

    // Traffic Light Phase Information

    /**
     * Returns the current phase index for a traffic light.
     * 
     * @param tlId The traffic light identifier
     * @return Current phase index (0-last phase)
     * @throws Exception if TraCI communication fails
     */
    public int getCurrentPhase(String tlId) throws Exception {
        return (int) conn.do_job_get(Trafficlight.getPhase(tlId));
    }

    /**
     * Returns the current phase duration in seconds.
     * 
     * @param tlId The traffic light identifier
     * @return Phase duration in seconds
     * @throws Exception if TraCI communication fails
     */
    public double getCurrentPhaseDuration(String tlId) throws Exception {
        return (double) conn.do_job_get(Trafficlight.getPhaseDuration(tlId));
    }

    /**
     * Returns the time until the next phase switching.
     * 
     * @param tlId The traffic light identifier
     * @return Time in seconds until next switch
     * @throws Exception if TraCI communication fails
     */
    public double getNextSwitch(String tlId) throws Exception {
        return (double) conn.do_job_get(Trafficlight.getNextSwitch(tlId));
    }

    /**
     * Sets the duration of the currently active phase.
     * 
     * @param tlId     The traffic light identifier
     * @param duration New duration in seconds
     * @throws Exception if TraCI communication fails
     */
    public void setPhaseDuration(String tlId, double duration) throws Exception {
        conn.do_job_set(Trafficlight.setPhaseDuration(tlId, duration));
    }

    /**
     * Sets a specific phase as the current active phase.
     * 
     * @param tlId       The traffic light identifier
     * @param phaseIndex The phase index to switch to
     * @throws Exception if TraCI communication fails
     */
    public void setPhase(String tlId, int phaseIndex) throws Exception {
        conn.do_job_set(Trafficlight.setPhase(tlId, phaseIndex));
    }

    /**
     * Returns the TraCI connection for advanced operations.
     * 
     * @return The SUMO TraCI connection
     */
    public SumoTraciConnection getConnection() {
        return conn;
    }
}
