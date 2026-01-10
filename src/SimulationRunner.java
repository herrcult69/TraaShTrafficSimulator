import it.polito.appeal.traci.SumoTraciConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the SUMO simulation in a background thread.
 * Executes simulation steps and provides real-time updates via concurrent maps.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see TraaSAdapter
 */
public class SimulationRunner implements Runnable {

    /**
     * Callback interface for SUMO connection establishment.
     */
    public interface ConnectionListener {
        /**
         * Called when TraCI connection is established.
         * 
         * @param adapter TraaSAdapter for SUMO communication
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
     * Constructs a simulation runner.
     * 
     * @param configFile SUMO configuration file path
     * @param gui Enable SUMO GUI
     */
    public SimulationRunner(String configFile, boolean gui) {
        this.configFile = configFile;
        this.gui = gui;
    }

    /**
     * Sets connection establishment listener.
     * 
     * @param listener Connection callback
     */
    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    /** Returns vehicle speeds (thread-safe). */
    public Map<String, Double> getVehicleSpeeds() {
        return vehicleSpeeds;
    }

    /** Returns vehicle positions and states (thread-safe). */
    public Map<String, double[]> getVehiclePositions() {
        return vehiclePositions;
    }

    /** Returns current simulation time in seconds. */
    public double getSimulationTime() {
        return simulationTime;
    }

    /** Returns traffic light states (thread-safe). */
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

    /** Returns TraCI adapter for SUMO communication. */
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
     * Establishes TraCI connection and executes simulation steps.
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
                Thread.sleep(80);
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
