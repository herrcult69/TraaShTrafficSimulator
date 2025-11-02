import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Simulation;

public class TestTraaS {
    public static void main(String[] args) {
        try {
            // For Linux, use the Linux executable (no .exe extension)
            SumoTraciConnection conn = new SumoTraciConnection(
                "sumo-gui",  // or "sumo" for non-GUI version
                "resource/simulation.sumocfg"  // Full path to config
            );
            conn.addOption("start", "true");
            conn.runServer();
            
            System.out.println("Connected to SUMO!");
            
            for (int i = 0; i < 1000; i++) {
                conn.do_timestep();
                double time = (double) conn.do_job_get(Simulation.getTime());
                System.out.println("Step " + i + " - Time: " + time);
                Thread.sleep(100);  // 100 milliseconds
            }
            
            conn.close();
            System.out.println("Connection closed successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}