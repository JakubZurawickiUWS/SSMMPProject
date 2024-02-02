package ApiGateway;

import java.util.HashMap;
import java.util.Map;

public class MicroserviceAgent {
    private static MicroserviceAgent instance;
    private static Map<String, Thread> microserviceThreads = null;
    private static Map<String, Integer> microservicePorts = new HashMap<>();

    // Private constructor to ensure the singleton pattern
    private MicroserviceAgent() {
        microserviceThreads = new HashMap<>();
    }

    // Singleton pattern: get an instance of Agent
    public static MicroserviceAgent getInstance() {
        if (instance == null) {
            instance = new MicroserviceAgent();
        }
        return instance;
    }

    // Start a microservice with a given service name
    public static void startMicroservice(String serviceName) {
        System.out.println("Microservice " + serviceName + " is starting.\n");
        Thread microserviceThread = new Thread(() -> {
            try {
                // Construct the fully qualified class name and invoke its main method
                String className = serviceName + "." + serviceName;
                System.out.println(className);
                Class<?> clazz = Class.forName(className);
                clazz.getMethod("main", String[].class).invoke(null, (Object) null);
            } catch (Exception e) {
                System.out.println("While attempting to start the " + serviceName + " Microservice, an error occurred.\n");
                e.printStackTrace();
            }
        });

        microserviceThreads.put(serviceName, microserviceThread); // Store the microservice thread
        microserviceThread.start(); // Start the microservice thread
    }

    // Stop a microservice with a given service name
    public void stopMicroservice(String serviceName) {
        Thread microserviceThread = microserviceThreads.get(serviceName);
        System.out.println("Microservice " + serviceName + " is closing.\n");
        if (microserviceThread != null) {
            try {
                microserviceThread.interrupt(); // Interrupt the microservice thread to stop it
                System.out.println("Microservice " + serviceName + " stopped working.\n");
                System.out.println("The port " + microservicePorts.get(serviceName) + " has been released.");
                microservicePorts.remove(serviceName);
            } catch (Exception e) {
                System.out.println("An error occurred while closing the " + serviceName + " Microservice.\n");
                e.printStackTrace();
            }
        }
    }

    // Handle communication requests
    public static String communication(String request) {
        // Parsing the request
        String[] parts = request.split("~");

        // Extracting elements from the 'parts' array
        String type = parts[1];
        String serviceName = parts[3];

        // Selecting the appropriate method based on the request type
        switch (type) {
            case "getPort_Request":
                return handleGetPortRespond(serviceName);
            case "registerMicroservice_Request":
                int port = Integer.parseInt(parts[5]);
                return handleRegisterRespond(serviceName, port);
            default:
                return "Unknown request type";
        }
    }

    // Get the port of a microservice by its service name using the MicroserviceManager
    private static String handleGetPortRespond(String serviceName) {
        int port = getPort(serviceName);
        return "type:~getPort_Response~port:~" + port;
    }

    // Handle registration response
    private static String handleRegisterRespond(String serviceName, int portRequest) {
        int port = registerMicroservice(serviceName, portRequest);
        return "type:~getPort~Response~port:~" + port;
    }

    // Register a microservice with a given service name and port
    public static int registerMicroservice(String serviceName, int port) {
        if (!microservicePorts.containsKey(serviceName)) {
            microservicePorts.put(serviceName, port);
            startMicroservice(serviceName);
        }

        return microservicePorts.get(serviceName);
    }

    // Get the port of a microservice by its service name
    public static int getPort(String serviceName) {
        return microservicePorts.getOrDefault(serviceName, -1); // Delegate to MicroserviceManager
    }
}
