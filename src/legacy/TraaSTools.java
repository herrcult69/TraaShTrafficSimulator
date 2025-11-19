package legacy;
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.objects.SumoPosition2D;
import java.util.List;


public class TraaSTools {
    // Vehicle methods
    @SuppressWarnings("unchecked")
    public static List<String> getAllVehicleIds(SumoTraciConnection conn) throws Exception {
        return (List<String>) conn.do_job_get(Vehicle.getIDList());
    }

    public static double[] getVehiclePosition(SumoTraciConnection conn, String vID) throws Exception {
        SumoPosition2D pos = (SumoPosition2D) conn.do_job_get(Vehicle.getPosition(vID));
        return new double[]{pos.x, pos.y};
    }

    // Traffic light methods
    @SuppressWarnings("unchecked")
    public static List<String> getTrafficLightIds(SumoTraciConnection conn) throws Exception {
        return (List<String>) conn.do_job_get(Trafficlight.getIDList());
    }

    public static String getTrafficLightState(SumoTraciConnection conn, String tlId) throws Exception {
        return (String) conn.do_job_get(Trafficlight.getRedYellowGreenState(tlId));
    }

    /** Utility to derive a simple color from a TL state string */
    public static String interpretTrafficLightColor(String state) {
        // If any green -> green, else if any yellow -> yellow, else red
        if (state == null || state.isEmpty()) return "RED";
        if (state.indexOf('g') >= 0 || state.indexOf('G') >= 0) return "GREEN";
        if (state.indexOf('y') >= 0 || state.indexOf('Y') >= 0) return "YELLOW";
        return "RED";
    }
    



  
    
}