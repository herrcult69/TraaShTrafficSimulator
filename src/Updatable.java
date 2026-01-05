/**
 * Interface for objects that receive periodic updates from the SUMO simulation.
 * Implemented by Vehicle and TrafficLight for real-time state synchronization.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see Vehicle
 */
public interface Updatable {

    /**
     * Updates this object's state with new data from SUMO.
     * 
     * @param data The update data from SUMO
     */
    void updateFromSimulation(Object data);

    /**
     * Returns the unique identifier for this updatable object.
     * Used to match simulation data with the correct object instance.
     * 
     * @return The object's unique ID
     */
    String getUpdateId();
}
