import it.polito.appeal.traci.SumoTraciConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the SUMO traffic simulation in a background thread and provides real-time updates.
 * 
 * <p>This class:
 * <ul>
 *   <li>Establishes and maintains a TraCI connection to SUMO</li>
 *   <li>Executes simulation time steps at ~10 Hz (100ms intervals)</li>
 *   <li>Periodically queries SUMO for vehicle positions, speeds, and traffic light states</li>
 *   <li>Provides thread-safe access to simulation data via concurrent maps</li>
 *   <li>Supports pause/resume and graceful shutdown</li>
 * </ul>
 * 
 * <p>The simulation runs independently in its own thread while the JavaFX rendering thread
 * reads from the shared concurrent maps at ~60 FPS for smooth visualization.</p>
 *
 * @author M A T^2 H Team
 * @version 2.0 
 * @see TraaSAdapter
 * @see TrafficLight
 */
public class SimulationRunner implements Runnable {

    /**
     * Callback interface for notification when the SUMO connection is established.
     */
    public interface ConnectionListener {
        /**
         * Called when the TraCI connection to SUMO is successfully established.
         * 
         * @param adapter The TraaSAdapter for communicating with SUMO
         */
        void onConnected(TraaSAdapter adapter);
    }

    private final String configFile;
    private final boolean gui;
    private final Map<String, double[]> vehiclePositions = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private TraaSAdapter adapter;
    private final Map<String, Double> vehicleSpeeds = new ConcurrentHashMap<>();
    private volatile double simulationTime = 0.0;
    private final Map<String, TrafficLight.TrafficLightData> trafficLightData = new ConcurrentHashMap<>();
    private final Map<String, String> vehicleEdges = new ConcurrentHashMap<>();
    private ConnectionListener connectionListener;

    private final Map<String, Integer> vehicleCountsByType = new ConcurrentHashMap<>();
    /**
     * Constructs a new simulation runner.
     * 
     * @param configFile Path to the SUMO configuration file (.sumocfg)
     * @param gui Whether to launch SUMO with GUI (true) or headless (false)
     */
    public SimulationRunner(String configFile, boolean gui) {
        this.configFile = configFile;
        this.gui = gui;
    }

    /**
     * Sets a listener to be notified when the SUMO connection is established.
     * 
     * @param listener The connection listener callback
     */
    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    /**
     * Returns a thread-safe map of vehicle speeds.
     * 
     * @return Map from vehicle ID to speed in m/s
     */
    public Map<String, Double> getVehicleSpeeds() {
        return vehicleSpeeds;
    }

    /**
     * Returns a thread-safe map of vehicle positions and states.
     * 
     * @return Map from vehicle ID to array [x, y, angle, signals]
     */
    public Map<String, double[]> getVehiclePositions() {
        return vehiclePositions;
    }

    /**
     * Returns the current simulation time in seconds.
     * 
     * @return Simulation time in seconds
     */
    public double getSimulationTime() {
        return simulationTime;
    }

    /**
     * Returns a thread-safe map of traffic light states.
     * 
     * @return Map from junction ID to traffic light data
     */
    public Map<String, TrafficLight.TrafficLightData> getTrafficLightData() {
        return trafficLightData;
    }
    
    /**
     * Returns a thread-safe map of vehicle road IDs.
     * 
     * @return Map from vehicle ID to edge ID
     */
    public Map<String, String> getVehicleEdges() {
        return vehicleEdges;
    }
    
    /**
     * Returns a thread-safe map of vehicle counts by type
     * @return Map from vehicle type to count
     */
    public Map<String, Integer> getVehicleCountsByType() {
        return vehicleCountsByType;
    }

    /**
     * Returns the TraCI adapter for direct SUMO communication.
     * 
     * @return The TraaSAdapter, or null if not yet connected
     */
    public TraaSAdapter getAdapter() {
        return adapter;
    }

    /**
     * Stops the simulation and closes the SUMO connection.
     */
    public void stop() {
        running = false;
    }

    /**
     * Pauses the simulation (stops time stepping but keeps connection alive).
     */
    public void pause() {
        paused = true;
    }

    /**
     * Resumes the simulation after being paused.
     */
    public void resume() {
        paused = false;
    }

    /**
     * Returns whether the simulation is currently paused.
     * 
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Returns whether the simulation is still running.
     * 
     * @return true if running, false if stopped
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Main simulation loop that runs in a background thread.
     * <p>
     * This method:
     * <ol>
     *   <li>Establishes TraCI connection to SUMO</li>
     *   <li>Notifies the connection listener</li>
     *   <li>Executes simulation time steps at 100ms intervals</li>
     *   <li>Updates vehicle positions, speeds, and traffic light states</li>
     *   <li>Continues until stop() is called</li>
     * </ol>
     */
    @Override
    public void run() {
        String binary = gui ? "sumo-gui" : "sumo";
        try {
            SumoTraciConnection conn = new SumoTraciConnection(binary, configFile);
            conn.addOption("start", "true");
            conn.runServer();
            adapter = new TraaSAdapter(conn);

            // Notify connection listener if set
            if (connectionListener != null) {
                connectionListener.onConnected(adapter);
            }

            while (running) {
                if (!paused) {
                    conn.do_timestep();
                    simulationTime = adapter.getSimulationTime();

                    // Update vehicles
                    List<String> ids = adapter.getVehicleIds();
                    vehiclePositions.keySet().removeIf(id -> !ids.contains(id));
                    vehicleSpeeds.keySet().removeIf(id -> !ids.contains(id));
                    vehicleEdges.keySet().removeIf(id -> !ids.contains(id));

                    // Reset counts
                    vehicleCountsByType.clear();
                    vehicleCountsByType.put("car", 0);
                    vehicleCountsByType.put("truck", 0);
                    vehicleCountsByType.put("bus", 0);
                    vehicleCountsByType.put("moto", 0);
                    vehicleCountsByType.put("emergency", 0);

                    for (String id : ids) {
                        double[] p = adapter.getVehiclePosition(id);
                        double ang = 0.0;
                        double speed = 0.0;
                        int signals = 0;
                        String edgeId = "";
                        try {
                            ang = adapter.getVehicleAngle(id);
                            speed = adapter.getVehicleSpeed(id);
                            signals = adapter.getVehicleSignals(id);
                            edgeId = adapter.getVehicleRoadID(id);
                        } catch (Exception ignore) {
                        }
                        vehiclePositions.put(id, new double[] { p[0], p[1], ang, (double) signals });
                        vehicleSpeeds.put(id, speed);
                        if (!edgeId.isEmpty()) {
                            vehicleEdges.put(id, edgeId);
                        }

                        //Count by type
                        if (id.startsWith("car")){
                            vehicleCountsByType.merge("car", 1, Integer::sum);
                        } else if (id.startsWith("truck")) {
                            vehicleCountsByType.merge("truck", 1, Integer::sum);
                        } else if (id.startsWith("bus")) {
                            vehicleCountsByType.merge("bus", 1, Integer::sum);
                        } else if (id.startsWith("moto")) {
                            vehicleCountsByType.merge("moto", 1, Integer::sum);
                        } else if (id.startsWith("ambu")) {
                            vehicleCountsByType.merge("emergency", 1, Integer::sum);
                        }
                    }

                    // Update traffic lights (only if not in manual mode)
                    List<String> tlIds = adapter.getTrafficLightIds();
                    for (String tlId : tlIds) {
                        String state = adapter.getTrafficLightState(tlId);
                        trafficLightData.put(tlId, new TrafficLight.TrafficLightData(state, null, 0));
                    }
                }
                Thread.sleep(100);
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
