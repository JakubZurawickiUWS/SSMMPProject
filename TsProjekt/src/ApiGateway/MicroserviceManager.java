package ApiGateway;

// Singleton class responsible for managing microservices
public class MicroserviceManager {
    // Singleton instance
    private static MicroserviceManager instance;

    // Managers for handling microservice ports and threads
    private final MicroservicePort portManager;
    private final Agent threadManager;

    // Private constructor to enforce singleton pattern
    private MicroserviceManager() {
        // Initialize microservice port and thread managers
        portManager = new MicroservicePort();
        threadManager = new Agent();
    }

    // Get the singleton instance of MicroserviceManager
    public static MicroserviceManager getInstance() {
        // Lazy initialization: create instance if null
        if (instance == null) {
            instance = new MicroserviceManager();
        }
        return instance;
    }

    // Add a new microservice with the given service name
    public void addMicroservice(String serviceName) {
        // Register microservice port
        portManager.registerMicroservice(serviceName);

        // Start microservice thread
        threadManager.startMicroservice(serviceName);
    }

    // Delete an existing microservice with the given service name
    public void deleteMicroservice(String serviceName) {
        // Unregister microservice port
        portManager.unregisterMicroservice(serviceName);

        // Stop microservice thread
        threadManager.stopMicroservice(serviceName);
    }

    // Get the port number associated with a microservice
    public int getMicroservicePort(String serviceName) {
        return portManager.getMicroservicePort(serviceName);
    }
}
