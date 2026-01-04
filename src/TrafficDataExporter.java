import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles exporting traffic simulation data to CSV files.
 * 
 * <p>This class provides functionality to export current simulation state including:
 * <ul>
 *   <li>Timestamp and simulation time</li>
 *   <li>Total vehicle count</li>
 *   <li>Vehicle breakdown by type (cars, trucks, buses, motorcycles, emergency)</li>
 *   <li>Average vehicle speed</li>
 * </ul>
 * 
 * @author M A T^2 H Team
 * @version 1.0
 */
public class TrafficDataExporter {
    /**
     * Exports current simulation data to a CSV file.
     * 
     * @param csvPath The file path where CSV should be saved
     * @param runner The simulation runner to get current simulation time
     * @param trafficManager The traffic manager containing vehicle data
     * @throws IOException if file writing fails
     */
    public static class Snapshot {
        public long timeStamp;
        public double simulationTime;
        public int totalVehicles;
        public int cars;
        public int trucks;
        public int buses;
        public int motorcycles;
        public int emergency;
        public double avgSpeed;

        public String toCSVRow() {
            return String.format("%d, %.2f, %d, %d, %d, %d, %d, %d, %.2f\n", timeStamp, simulationTime,totalVehicles, cars, trucks, buses, motorcycles, emergency, avgSpeed);
        }
        @Override
        public String toString() {
            return String.format("Time: %.2fs - Total: %d (Cars: %d, Trucks: %d, Buses: %d, Motorcycles: %d, Emergency: %d) - Avg Speed: %.2f", timeStamp, simulationTime,totalVehicles, cars, trucks, buses, motorcycles, emergency, avgSpeed);
        }
    }
    
    public static void exportToCSV(String csvPath, SimulationRunner runner, TrafficManager trafficManager) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath))) {
            writer.write("timestamp, simulation_time, total_vehicles, cars, trucks, buses, motorcycles, emergency, avg_speed\n ");
            //Get current simulation data 
            long timeStamp = System.currentTimeMillis();
            double simTime;
            if (runner != null){
                simTime = runner.getSimulationTime();
            }
            else { simTime = 0.0;}

            //Get vehicle speeds from runner

            java.util.Map<String, Double> speeds = runner.getVehicleSpeeds();
        int totalVehicles = speeds.size();

        int cars = 0, trucks = 0, buses = 0, motorcycles = 0, emergency = 0;
        double totalSpeed = 0;
        
        for (java.util.Map.Entry<String, Double> entry : speeds.entrySet()) {
            String id = entry.getKey();
            totalSpeed += entry.getValue();
            
            // Count by vehicle ID prefix (same as TrafficSimulatorApp)
            if (id.startsWith("car")) cars++;
            else if (id.startsWith("truck")) trucks++;
            else if (id.startsWith("bus")) buses++;
            else if (id.startsWith("moto")) motorcycles++;
            else if (id.startsWith("ambu")) emergency++;
        }
            double avgSpeed;
            if (totalVehicles > 0) {
                avgSpeed = totalSpeed / totalVehicles;
            }
            else {
                avgSpeed = 0.0;
            }
            // Write data row
            writer.write(String.format("%d,%.2f,%d,%d,%d,%d,%d,%d,%.2f\n",
                timeStamp, simTime, totalVehicles, cars, trucks, buses, motorcycles, emergency, avgSpeed));
            
            writer.flush();
        }
    }
}
