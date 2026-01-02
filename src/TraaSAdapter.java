
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
 * Adapter class for communicating with SUMO via the TraCI (Traffic Control Interface) protocol.
 * 
 * <p>This class wraps the TraaS (TraCI as a Service) library and provides simplified methods for:
 * <ul>
 *   <li>Querying simulation state (time, vehicle positions, angles, speeds, signals)</li>
 *   <li>Querying and controlling traffic lights (state, program)</li>
 *   <li>Adding vehicles and routes dynamically during simulation</li>
 *   <li>Finding valid routes between edges using SUMO's routing</li>
 * </ul>
 * 
 * <p>All methods may throw exceptions if the TraCI connection is not established
 * or if SUMO returns an error (e.g., invalid vehicle ID, route not found).</p>
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
     * @return Angle in degrees (0 = east, 90 = north, as per SUMO convention)
     * @throws Exception if TraCI communication fails or vehicle doesn't exist
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
     * Returns the signal state of a vehicle (turn signals, brake lights).
     * The result is a bit field where:
     * <ul>
     *   <li>Bit 0 (1): Right turn signal</li>
     *   <li>Bit 1 (2): Left turn signal</li>
     *   <li>Bit 2 (4): Emergency flashers (both turn signals)</li>
     *   <li>Bit 3 (8): Brake lights</li>
     * </ul>
     * 
     * @param id The vehicle identifier
     * @return Signal state as an integer bit field
     * @throws Exception if TraCI communication fails or vehicle doesn't exist
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
     * Returns the current state of a traffic light as a string of signal characters.
     * Each character represents one signal: 'r'/'R' = red, 'y'/'Y' = yellow, 'g'/'G' = green, 'o' = off.
     * 
     * @param tlId The traffic light (junction) identifier
     * @return State string (e.g., "GrGr" for a 4-signal light)
     * @throws Exception if TraCI communication fails or traffic light doesn't exist
     */
    public String getTrafficLightState(String tlId) throws Exception {
        return (String) conn.do_job_get(Trafficlight.getRedYellowGreenState(tlId));
    }

    /**
     * Sets the state of a traffic light manually.
     * This overrides automatic control until setTrafficLightProgram is called.
     * 
     * @param tlId The traffic light (junction) identifier
     * @param state State string (e.g., "GGrr") where each character controls one signal
     * @throws Exception if TraCI communication fails or state is invalid
     */
    public void setTrafficLightState(String tlId, String state) throws Exception {
        conn.do_job_set(Trafficlight.setRedYellowGreenState(tlId, state));
    }

    /**
     * Sets the traffic light program, typically used to return to automatic control.
     * 
     * @param tlId The traffic light (junction) identifier
     * @param programId The program ID ("0" is typically the default automatic program)
     * @throws Exception if TraCI communication fails or program doesn't exist
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
     * Routes define a sequence of edges that vehicles can follow.
     * 
     * @param routeId Unique route identifier
     * @param edges List of edge IDs that form the route
     * @throws Exception if TraCI communication fails or route is invalid
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
     * The vehicle will appear at the start of the specified route at the next simulation step.
     * 
     * @param vehicleId Unique vehicle identifier
     * @param routeId The route ID for this vehicle to follow
     * @param vehicleClass The vehicle class (passenger, truck, bus, motorcycle, emergency)
     * @throws Exception if TraCI communication fails, route doesn't exist, or vehicle ID is duplicate
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
     * Finds a valid route between two edges using SUMO's built-in routing algorithm.
     * This respects the network topology and returns the shortest path.
     * 
     * @param fromEdge Starting edge ID
     * @param toEdge Destination edge ID
     * @return List of edge IDs forming the route, or null if no route exists
     * @throws Exception if TraCI communication fails or edges don't exist
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
     * Returns the default total duration of the currently active phase in seconds.
     * 
     * @param tlId The traffic light identifier
     * @return Phase duration in seconds
     * @throws Exception if TraCI communication fails
     */
    public double getCurrentPhaseDuration(String tlId) throws Exception {
        return (double) conn.do_job_get(Trafficlight.getPhaseDuration(tlId));
    }

    /**
     * Returns the time until the next phase switching
     * 
     * @param tlId The traffic light identifier
     * @return Time in seconds until next switch
     * @throws Exception if TraCI communication fails
     */
    public double getNextSwitch(String tlId) throws Exception {
        return (double) conn.do_job_get(Trafficlight.getNextSwitch(tlId));
    }
}
