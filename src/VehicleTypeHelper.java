/**
 * Helper class for vehicle type operations.
 */
public class VehicleTypeHelper {
    /**
     * Determines vehicle type from vehicle ID.
     * Handles both regular IDs (e.g., "car_1") and stress test IDs (e.g., "stress_car_123").
     * 
     * @param vehicleId The vehicle ID
     * @return Vehicle type (car, truck, bus, moto, emergency, or unknown)
     */
    public static String getVehicleType(String vehicleId) {
        // Handle stress test prefix (format: "stress_<type>_...")
        String idToCheck = vehicleId;
        if (vehicleId.startsWith("stress_")) {
            idToCheck = vehicleId.substring(7); // Remove "stress_" prefix
        }
        
        if (idToCheck.startsWith("car")) return "car";
        if (idToCheck.startsWith("truck")) return "truck";
        if (idToCheck.startsWith("bus")) return "bus";
        if (idToCheck.startsWith("moto")) return "moto";
        if (idToCheck.startsWith("motorcycle")) return "moto";
        if (idToCheck.startsWith("ambu")) return "emergency";
        if (idToCheck.startsWith("emergency")) return "emergency";
        return "unknown";
    }
}
