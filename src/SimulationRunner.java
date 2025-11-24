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

    @Override
    public void run() {
        String binary = gui ? "sumo-gui" : "sumo";
        try {
            SumoTraciConnection conn = new SumoTraciConnection(binary, configFile);
            conn.addOption("start", "true");
            conn.runServer();
            adapter = new TraaSAdapter(conn);
            while (running) {
                if (!paused) {
                    conn.do_timestep();
                    simulationTime = adapter.getSimulationTime();
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
