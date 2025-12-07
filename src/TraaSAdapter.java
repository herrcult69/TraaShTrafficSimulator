
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.objects.SumoLinkList;
import de.tudresden.sumo.objects.SumoPosition2D;

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

}
