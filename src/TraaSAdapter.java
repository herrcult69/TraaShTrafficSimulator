
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.objects.SumoLinkList;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoStringList;

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
    
    // Get Traffic Light Ids
    public List<String> getTrafficLightIds() throws Exception {
        return (List<String>) conn.do_job_get(Trafficlight.getIDList());
    }
    // Get the lanes that the junction controlled
    public List<String> getControlledLanes(String trafficLightId) throws Exception {
        SumoStringList laneList = (SumoStringList) conn.do_job_get(
            Trafficlight.getControlledLanes(trafficLightId)
        );
        return laneList;
    }
    // Get the number of links
    public int getControlledLinksCount(String trafficLightId) throws Exception {
        SumoLinkList links = (SumoLinkList) conn.do_job_get(
            Trafficlight.getControlledLinks(trafficLightId)
        );
        return links.size();
    }
    // get the state
    public String getTrafficLightState(String tlId) throws Exception {
        return (String) conn.do_job_get(Trafficlight.getRedYellowGreenState(tlId));
    }
    
    // set the state
    public void setTrafficLightState(String tlId, String state) throws Exception {
        conn.do_job_set(Trafficlight.setRedYellowGreenState(tlId, state));
    }

    public SumoLinkList getControlledLinks(String trafficLightId) throws Exception {
        return (SumoLinkList) conn.do_job_get(
            Trafficlight.getControlledLinks(trafficLightId)
        );
    }

    public static String interpretTrafficLightColor(String state) {
        if (state == null || state.isEmpty())
            return "RED";
        // Precedence: any GREEN -> GREEN; else any RED -> RED; else any YELLOW ->
        // YELLOW; else RED
        if (state.indexOf('g') >= 0 || state.indexOf('G') >= 0)
            return "GREEN";
        if (state.indexOf('r') >= 0 || state.indexOf('R') >= 0)
            return "RED";
        if (state.indexOf('y') >= 0 || state.indexOf('Y') >= 0)
            return "YELLOW";
        return "RED";
    }
}
