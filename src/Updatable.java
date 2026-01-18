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
     * Updates state with SUMO data.
     * 
     * @param data Update data from SUMO
     */
    void updateFromSimulation(Object data);

    /** Returns unique identifier for simulation updates. */
    String getUpdateId();
}
