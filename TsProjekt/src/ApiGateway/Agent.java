package ApiGateway;

import java.util.HashMap;
import java.util.Map;

// Manages the lifecycle of microservice threads (start and stop)
public class Agent {
    // Map to store microservice names and their corresponding threads
    private final Map<String, Thread> microserviceThreads;

    // Constructor initializes the data structure
    public Agent() {
        microserviceThreads = new HashMap<>();
    }

    // Start a new thread for the specified microservice
    public void startMicroservice(String serviceName) {
        // Create a new thread for the microservice
        Thread microserviceThread = new Thread(() -> {
            try {
                // Dynamically load and invoke the main method of the microservice class
                String className = serviceName + "." + serviceName;
                Class<?> clazz = Class.forName(className);
                clazz.getMethod("main", String[].class).invoke(null, (Object) null);
            } catch (Exception e) {
                e.printStackTrace(); // Handle exceptions during microservice thread startup
            }
        });

        // Store the microservice thread in the map and start the thread
        microserviceThreads.put(serviceName, microserviceThread);
        microserviceThread.start();
    }

    // Stop the thread associated with the specified microservice
    public void stopMicroservice(String serviceName) {
        // Retrieve the microservice thread from the map
        Thread microserviceThread = microserviceThreads.get(serviceName);

        // Check if the thread exists
        if (microserviceThread != null) {
            try {
                // Interrupt the microservice thread to stop its execution
                microserviceThread.interrupt();
            } catch (Exception e) {
                e.printStackTrace(); // Handle exceptions during microservice thread stopping
            }
        }
    }
}
