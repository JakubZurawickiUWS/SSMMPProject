package ApiGateway;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

public class MicroserviceManager {
    private static MicroserviceManager instance;
    private final Map<String, Integer> microservicePorts;
    private final Integer portApi;

    // Private constructor to ensure singleton pattern
    private MicroserviceManager() {
        microservicePorts = new HashMap<>();
        this.portApi = 3000; // Set the API port to 3000 by default
    }

    // Singleton pattern: get an instance of MicroserviceManager
    public static MicroserviceManager getInstance() {
        if (instance == null) {
            instance = new MicroserviceManager();
        }
        return instance;
    }

    // Add a microservice with a given service name
    public void addMicroservice(String serviceName) {
        registerMicroservice(serviceName);
    }

    // Delete a microservice with a given service name
    public void deleteMicroservice(String serviceName) {
        unregisterMicroservice(serviceName); // Unregister the microservice
        Agent.getInstance().stopMicroservice(serviceName); // Stop the microservice using Agent
    }

    // Get the port of a microservice by its service name
    public int getMicroservicePort(String serviceName) {
        return microservicePorts.getOrDefault(serviceName, -1); // Return the port or -1 if not found
    }

    // Get the API port
    public int getApiPort() {
        return portApi; // Return the predefined API port
    }

    // Register a microservice with a unique port and start it
    private void registerMicroservice(String serviceName) {
        if(!microservicePorts.containsKey(serviceName)) {
            int port = findAvailablePort(); // Find an available port
            microservicePorts.put(serviceName, port); // Store the service name and its port
            Agent.getInstance().startMicroservice(serviceName); // Start the microservice using Agent
        }
    }

    // Unregister a microservice by its service name
    private void unregisterMicroservice(String serviceName) {
        microservicePorts.remove(serviceName); // Remove the microservice from the port map
    }

    // Find an available port for a microservice
    private int findAvailablePort() {
        int initialPort = 3000; // Initial port to start searching
        int port = initialPort;

        // Keep searching for an available port
        while (isPortOccupied(port)) {
            port = port + 1; // Increment port number if occupied
        }

        return port; // Return the first available port found
    }

    // Check if a port is occupied by attempting to create a ServerSocket
    private boolean isPortOccupied(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return false; // Port is not occupied if ServerSocket can be created
        } catch (IOException e) {
            return true; // Port is occupied if an IOException occurs when creating ServerSocket
        }
    }
}
