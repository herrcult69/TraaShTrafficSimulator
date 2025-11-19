package legacy;
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Simulation;
import java.util.List;

public class TestTraaS {
    
    public static void main(String[] args) {
        try {
            // Initialize connection to SUMO with GUI and configuration file
            SumoTraciConnection conn = new SumoTraciConnection(
                "sumo-gui", // change to "sumo for headless (no-gui) otw "sumo-gui"
                "resource/simulation.sumocfg" // path to .sumoconfig
            );
            
            // Set SUMO to start immediately
            conn.addOption("start", "true");
            
            // Start the SUMO server
            // Output: SUMO server starts and waits for commands
            conn.runServer();
            
            System.out.println("Connected to SUMO!");
            
            // Run simulation for 1000 steps
            for (int i = 0; i < 1000; i++) {
                conn.do_timestep();
                
                // Get current simulation time
                // Output: Returns current simulation time in seconds (double)
                double time = (double) conn.do_job_get(Simulation.getTime());
                
                // Retrieve all vehicle IDs in the simulation
                // Output: Returns List<String> containing all active vehicle IDs
                List<String> vehicleIds = TraaSTools.getAllVehicleIds(conn);
                
                // Display step information
                System.out.println("Step " + i + " - Time: " + time + 
                                 " - Vehicles: " + vehicleIds.size());
                
                // Iterate through all vehicles and get their positions
                for (int j = 0; j < vehicleIds.size(); j++) {
                    // Output: Prints vehicle ID (e.g., "veh0", "veh1")
                    System.out.println("Vehicle ID: " + vehicleIds.get(j) + " ");
                    
                    // Get vehicle position coordinates
                    // Output: Returns double[] with [x, y] coordinates in meters
                    double[] pos = TraaSTools.getVehiclePosition(conn, vehicleIds.get(j));
                    double x = pos[0];
                    double y = pos[1];
                    
                    // Output: Prints position (e.g., "Positions: x=123.45 y=67.89")
                    System.out.println("Positions: x=" + x + " y=" + y);
                }   
                
                // TODO: Implement real-time vehicle plotting
                // - Create a visualization panel/window 
                // - Plot vehicle positions (x, y) on a 2D map
                // - Update the plot each simulation step
                // - Use different colors/shapes for different vehicles
                // - 
                // - Add road network overlay from SUMO network file .net.xml
                
                // Pause between steps for visualization
                Thread.sleep(100);
            }
            
            // Close connection to SUMO
            conn.close();
            System.out.println("Connection closed successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }  
    }
}