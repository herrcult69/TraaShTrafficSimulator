/**
 * Helper class for vehicle type operations.
 */
public class VehicleTypeHelper {
    /**
     * Determines vehicle type from vehicle ID.
     * 
     * @param vehicleId The vehicle ID
     * @return Vehicle type (car, truck, bus, moto, emergency, or unknown)
     */
    public static String getVehicleType(String vehicleId) {
        if (vehicleId.startsWith("car")) return "car";
        if (vehicleId.startsWith("truck")) return "truck";
        if (vehicleId.startsWith("bus")) return "bus";
        if (vehicleId.startsWith("moto")) return "moto";
        if (vehicleId.startsWith("ambu")) return "emergency";
        return "unknown";
    }
}
