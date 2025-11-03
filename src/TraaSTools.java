import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Edge;
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
    



  
    
}