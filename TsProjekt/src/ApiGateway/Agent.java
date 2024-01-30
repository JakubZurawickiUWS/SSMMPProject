package ApiGateway;

import java.util.HashMap;
import java.util.Map;

public class Agent {
    private static Agent instance;
    private final Map<String, Thread> microserviceThreads;
    private final MicroserviceManager microserviceManager;

    // Private constructor to ensure singleton pattern
    private Agent() {
        microserviceThreads = new HashMap<>();
        microserviceManager = MicroserviceManager.getInstance(); // Initialize the MicroserviceManager
    }

    // Singleton pattern: get an instance of Agent
    public static Agent getInstance() {
        if (instance == null) {
            instance = new Agent();
        }
        return instance;
    }

    // Start a microservice with a given service name
    public void startMicroservice(String serviceName) {
        Thread microserviceThread = new Thread(() -> {
            try {
                // Construct the fully qualified class name and invoke its main method
                String className = serviceName + "." + serviceName;
                Class<?> clazz = Class.forName(className);
                clazz.getMethod("main", String[].class).invoke(null, (Object) null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        microserviceThreads.put(serviceName, microserviceThread); // Store the microservice thread
        microserviceThread.start(); // Start the microservice thread
    }

    // Stop a microservice with a given service name
    public void stopMicroservice(String serviceName) {
        Thread microserviceThread = microserviceThreads.get(serviceName);

        if (microserviceThread != null) {
            try {
                microserviceThread.interrupt(); // Interrupt the microservice thread to stop it
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Get the port of a microservice by its service name using the MicroserviceManager
    public int getPort(String serviceName) {
        return microserviceManager.getMicroservicePort(serviceName); // Delegate to MicroserviceManager
    }
}
