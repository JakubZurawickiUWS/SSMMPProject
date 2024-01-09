package ApiGateway;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// Manages microservice ports, ensuring unique ports for each registered microservice
public class MicroservicePort {
    // Map to store microservice names and their corresponding ports
    private final Map<String, Integer> microservicePorts;

    // Counter to generate unique ports
    private final AtomicInteger portCounter;

    // Constructor initializes the data structures
    public MicroservicePort() {
        microservicePorts = new HashMap<>();
        portCounter = new AtomicInteger(3000); // Starting port number
    }

    // Get the port associated with a microservice
    public int getMicroservicePort(String serviceName) {
        return microservicePorts.getOrDefault(serviceName, -1);
    }

    // Register a microservice with a unique port
    public void registerMicroservice(String serviceName) {
        int port = findAvailablePort();
        microservicePorts.put(serviceName, port);
    }

    // Unregister a microservice by removing it from the map
    public void unregisterMicroservice(String serviceName) {
        microservicePorts.remove(serviceName);
    }

    // Find an available port by incrementing the port counter
    private int findAvailablePort() {
        int initialPort = portCounter.getAndIncrement();
        int port = initialPort;

        // Loop until an available port is found
        while (isPortOccupied(port)) {
            port = portCounter.getAndIncrement();

            // Check if all ports have been checked
            if (port == initialPort) {
                throw new RuntimeException("Unable to find an available port.");
            }
        }

        return port;
    }

    // Check if a port is occupied by attempting to open a ServerSocket
    private boolean isPortOccupied(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return false; // Port is not occupied
        } catch (IOException e) {
            return true; // Port is occupied
        }
    }
}
