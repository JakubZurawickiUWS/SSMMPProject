package ApiGateway;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ManagerAgent {
    private static ManagerAgent instance; // Singleton instance of ManagerAgent
    private final Map<String, Integer> microservicePorts; // Stores microservice names and their port numbers
    private final AtomicInteger portCounter; // Counter for allocating new port numbers
    private final Map<String, Thread> microserviceThreads; // Stores threads associated with each microservice

    // Private constructor for singleton pattern
    private ManagerAgent() {
        microservicePorts = new HashMap<>();
        portCounter = new AtomicInteger(3000); // Starting port number
        microserviceThreads = new HashMap<>();
    }

    /**
     * Singleton method to get an instance of ManagerAgent.
     *
     * @return Instance of ManagerAgent.
     */
    public static ManagerAgent getInstance() {
        if (instance == null) {
            instance = new ManagerAgent();
        }
        return instance;
    }

    /**
     * Retrieves the port number for a given microservice.
     *
     * @param serviceName Name of the microservice.
     * @return Port number for the microservice.
     */
    public int getMicroservicePort(String serviceName) {
        // Returns the port number or -1 if the service is not registered
        return microservicePorts.getOrDefault(serviceName, -1);
    }

    /**
     * Registers a new microservice with a dynamically allocated port.
     *
     * @param serviceName Name of the microservice.
     */
    public void registerMicroservice(String serviceName) {
        int port = findAvailablePort(); // Find a free port
        microservicePorts.put(serviceName, port); // Register the service with the port
    }

    public void addMicroservice(String serviceName) {
        registerMicroservice(serviceName); // Register and allocate port
        startMicroservice(serviceName); // Start the microservice
    }

    public void deleteMicroservice(String serviceName) {
        microservicePorts.remove(serviceName); // Remove the service from the registry
        stopMicroservice(serviceName); // Stop the microservice
    }

    // Finds an available port
    private int findAvailablePort() {
        int initialPort = portCounter.getAndIncrement(); // Get current port and increment for the next use
        int port = initialPort;

        // Loop until an unoccupied port is found
        while (isPortOccupied(port)) {
            port = portCounter.getAndIncrement();
            if (port == initialPort) {
                throw new RuntimeException("Unable to find an available port.");
            }
        }

        return port;
    }

    // Checks if a port is already in use
    private boolean isPortOccupied(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return false; // Port is available if no exception is thrown
        } catch (IOException e) {
            return true; // Port is occupied
        }
    }

    // Starts a microservice in a new thread
    private void startMicroservice(String serviceName) {
        Thread microserviceThread = new Thread(() -> {
            try {
                // Dynamically load and start the microservice class
                String className = serviceName + "." + serviceName;
                Class<?> clazz = Class.forName(className);
                clazz.getMethod("main", String[].class).invoke(null, (Object) null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        microserviceThreads.put(serviceName, microserviceThread); // Store the thread
        microserviceThread.start(); // Start the thread
    }

    // Stops a specific microservice
    public void stopMicroservice(String serviceName) {
        Thread microserviceThread = microserviceThreads.get(serviceName);

        if (microserviceThread != null) {
            try {
                microserviceThread.interrupt(); // Attempt to interrupt the thread
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
