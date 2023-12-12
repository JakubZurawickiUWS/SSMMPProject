package ApiGateway;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MicroserviceAgent {
    private static MicroserviceAgent instance;
    private final Map<String, Integer> microservicePorts; // Map holding microservice names and their port numbers
    private final AtomicInteger portCounter; // Port counter for allocation

    private final Map<String, Thread> microserviceThreads;

    // Private constructor, initializes the microservice agent
    private MicroserviceAgent() {
        microservicePorts = new HashMap<>();
        portCounter = new AtomicInteger(3000); // Initial port number
        microserviceThreads = new HashMap<>();
    }

    /**
     * Get an instance of the microservice agent.
     *
     * @return An instance of the microservice agent.
     */
    public static MicroserviceAgent getInstance() {
        if (instance == null) {
            instance = new MicroserviceAgent();
        }
        return instance;
    }

    /**
     * Get the port number for a given microservice or register a new microservice.
     *
     * @param serviceName Microservice name.
     * @return Port number for the microservice.
     */
    public int getMicroservicePort(String serviceName) {
        return microservicePorts.getOrDefault(serviceName, -1); // -1 as the default value when the microservice does not exist
    }

    /**
     * Register a new microservice and dynamically allocate a port for it.
     *
     * @param serviceName Microservice name.
     */
    public void registerMicroservice(String serviceName) {
        int port = findAvailablePort();
        microservicePorts.put(serviceName, port);
    }

    public void addMicroservice(String serviceName) {
        registerMicroservice(serviceName);
        startMicroservice(serviceName);
    }

    public void deleteMicroservice(String serviceName) {
        microservicePorts.remove(serviceName);
        stopMicroservice(serviceName);
    }

    // Private method to find an available port
    private int findAvailablePort() {
        int initialPort = portCounter.getAndIncrement();
        int port = initialPort;

        while (isPortOccupied(port)) {
            port = portCounter.getAndIncrement();
            if (port == initialPort) {
                throw new RuntimeException("Unable to find an available port.");
            }
        }

        return port;
    }

    // Private method to check if a port is occupied
    private boolean isPortOccupied(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return false; // Port is available
        } catch (IOException e) {
            return true; // Port is occupied
        }
    }

    // Private method to start microservices in separate threads
    private void startMicroservice(String serviceName) {

        Thread microserviceThread = new Thread(() -> {
            try {
                // Call the main method of the respective microservice
                String className = serviceName + "." + serviceName;
                Class<?> clazz = Class.forName(className);
                clazz.getMethod("main", String[].class).invoke(null, (Object) null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        microserviceThreads.put(serviceName, microserviceThread);
        microserviceThread.start();
    }

    // Method to stop a specific microservice
    public void stopMicroservice(String serviceName) {
        Thread microserviceThread = microserviceThreads.get(serviceName);

        if (microserviceThread != null) {
            try {
                microserviceThread.interrupt(); // Interrupt the microservice thread
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
