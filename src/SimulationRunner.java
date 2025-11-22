import it.polito.appeal.traci.SumoTraciConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Handles SUMO stepping in a background thread and updates shared maps. */
public class SimulationRunner implements Runnable {
    private final String configFile;
    private final boolean gui;
    private final Map<String,double[]> vehiclePositions = new ConcurrentHashMap<>();
    private final Map<String,String> trafficLightColors = new ConcurrentHashMap<>();
    // Store current phase index and timer per traffic light
    private Map<String, Integer> trafficLightPhaseIndex = new ConcurrentHashMap<>();
    private Map<String, Double> trafficLightPhaseTimer = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private TraaSAdapter adapter;

    public SimulationRunner(String configFile, boolean gui){
        this.configFile = configFile; this.gui = gui;
    }

    public Map<String,double[]> getVehiclePositions(){return vehiclePositions;}
    public Map<String,String> getTrafficLightColors(){return trafficLightColors;}

    public void stop(){running = false;}

    private final Map<String, String> trafficLightStates = new ConcurrentHashMap<>();


    @Override
    public void run() {
        String binary = gui ? "sumo-gui" : "sumo";
        try {
            SumoTraciConnection conn = new SumoTraciConnection(binary, configFile);
            conn.addOption("start","true");
            conn.runServer();
            adapter = new TraaSAdapter(conn);
            List<String> tlIds = adapter.getTrafficLightIds();
            while(running){
                conn.do_timestep();
                List<String> ids = adapter.getVehicleIds();
                // remove vehicles no longer present
                vehiclePositions.keySet().removeIf(id -> !ids.contains(id));
                for(String id: ids){
                    double[] p = adapter.getVehiclePosition(id);
                    double ang = 0.0;
                    try { ang = adapter.getVehicleAngle(id); } catch (Exception ignore) {}
                    vehiclePositions.put(id, new double[]{p[0], p[1], ang});
                }
                // update traffic lights
                for(String tl: tlIds){
                    String state = adapter.getTrafficLightState(tl);
                    trafficLightColors.put(tl, TraaSAdapter.interpretTrafficLightColor(state));
                }
                Thread.sleep(100); // 10 steps per second approx if SUMO step-length=1
            }
            conn.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
    public Map<String, String> getTrafficLightStates() {
    return trafficLightStates;
}

    public String getTrafficLightState(String junctionId) {
        return trafficLightStates.get(junctionId);
    }
}
