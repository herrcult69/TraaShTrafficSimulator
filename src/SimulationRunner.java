import it.polito.appeal.traci.SumoTraciConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Handles SUMO stepping in a background thread and updates shared maps. */
public class SimulationRunner implements Runnable {
    private final String configFile;
    private final boolean gui;
    private final Map<String, double[]> vehiclePositions = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private TraaSAdapter adapter;
    private final Map<String, Double> vehicleSpeeds = new ConcurrentHashMap<>();
    private volatile double simulationTime = 0.0;

    public SimulationRunner(String configFile, boolean gui) {
        this.configFile = configFile;
        this.gui = gui;
    }

    public Map<String, Double> getVehicleSpeeds(){return vehicleSpeeds;}

    public Map<String,double[]> getVehiclePositions(){return vehiclePositions;}
    
    public double getSimulationTime(){return simulationTime;}

    public void stop() {
        running = false;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isRunning() {
        return running;
    }
    
    private String interpretSignalChar(char c) {
        switch (c) {
            case 'r': return "RED (stop)";
            case 'g': return "GREEN (go, yield)";
            case 'G': return "GREEN (go, priority)";
            case 'y': return "YELLOW (prepare to stop)";

            default: return "UNKNOWN (" + c + ")";
        }
    }

    @Override
    public void run() {
        String binary = gui ? "sumo-gui" : "sumo";
        try {
            SumoTraciConnection conn = new SumoTraciConnection(binary, configFile);
            conn.addOption("start", "true");
            conn.runServer();
            adapter = new TraaSAdapter(conn);
            
            // Debug: Print traffic light IDs once at start
            try {
                List<String> tlIds = adapter.getTrafficLightIds();
                System.out.println("\n=== TRAFFIC LIGHTS DETECTED ===");
                System.out.println("Total traffic lights: " + tlIds.size());
                for (String tlId : tlIds) {
                    System.out.println("  - Traffic Light ID: " + tlId);
                }
                System.out.println("================================\n");
            } catch (Exception e) {
                System.out.println("No traffic lights or error getting TL IDs: " + e.getMessage());
            }
            
            int stepCount = 0;
            while (running) {
                if (!paused) {
                    conn.do_timestep();
                    simulationTime = adapter.getSimulationTime();
                    stepCount++;
                    
                    // Debug: Print traffic light states every 20 steps (1 second if step-length=0.05)
                    if (stepCount % 20 == 0) {
                        try {
                            List<String> tlIds = adapter.getTrafficLightIds();
                            if (!tlIds.isEmpty()) {
                                System.out.println("\n======= Traffic Light States at t=" + String.format("%.1f", simulationTime) + "s =======");
                                for (String tlId : tlIds) {
                                    String state = adapter.getTrafficLightState(tlId);
                                    System.out.println("\nTraffic Light: " + tlId);
                                    System.out.println("  State String: '" + state + "' (length=" + state.length() + ")");
                                    System.out.println("  Note: Each character = one link/movement through intersection");
                                    System.out.println();
                                    
                                    // Print character breakdown with color interpretation
                                    System.out.println("  Signal Breakdown (each signal controls one movement):");
                                    for (int i = 0; i < state.length(); i++) {
                                        char c = state.charAt(i);
                                        String color = interpretSignalChar(c);
                                        System.out.println("    [" + String.format("%2d", i) + "] = '" + c + "' → " + color);
                                    }
        
                                }
                                System.out.println();
                            }
                        } catch (Exception e) {
                            System.out.println("Error getting TL states: " + e.getMessage());
                        }
                    }
                    
                    List<String> ids = adapter.getVehicleIds();
                    // remove vehicles no longer present
                    vehiclePositions.keySet().removeIf(id -> !ids.contains(id));
                    vehicleSpeeds.keySet().removeIf(id -> !ids.contains(id));
                    for(String id: ids){
                        double[] p = adapter.getVehiclePosition(id);
                        double ang = 0.0;
                        double speed = 0.0;
                        try { 
                            ang = adapter.getVehicleAngle(id); 
                            speed = adapter.getVehicleSpeed(id);
                        } catch (Exception ignore) {}
                        vehiclePositions.put(id, new double[]{p[0], p[1], ang});
                        vehicleSpeeds.put(id, speed); 
                    }
                }
                Thread.sleep(50); // 10 steps per second approx if SUMO step-length=1
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
