package ApiGateway;

public class AgentKlient {
    private static AgentKlient instance;
    private static MicroserviceManager microserviceManager;

    // Private constructor to ensure singleton pattern
    private AgentKlient() {
        // Initialize the microserviceManager when the instance is created
        microserviceManager = MicroserviceManager.getInstance(); // Initialize MicroserviceManager
    }

    // Singleton pattern: get an instance of AgentKlient
    public static AgentKlient getInstance() {
        if (instance == null) {
            instance = new AgentKlient();
        }
        return instance;
    }

    // Get the API port using the MicroserviceManager
    public int getPort() {
        return microserviceManager.getApiPort(); // Delegate to MicroserviceManager to get API port
    }
}
