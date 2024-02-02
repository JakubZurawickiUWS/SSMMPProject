package ApiGateway;

import java.io.IOException;
import java.net.ServerSocket;

public class Manager {
    private static Manager instance;
    private MicroserviceAgent microserviceAgent;

    // Private constructor for enforcing the Singleton pattern
    private Manager() {
        this.microserviceAgent = MicroserviceAgent.getInstance();
    }

    // Method to get a single instance of the Manager (Singleton pattern)
    public static synchronized Manager getInstance() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    // Method for handling communication with MicroserviceAgent
    private String handleCommunication(String request) {
        // Send a request to MicroserviceAgent and receive a response
        String response = MicroserviceAgent.communication(request);
        return response;
    }

    // Modified method for registering a microservice
    public int registerMicroservice(String serviceName) {
        // Create a request string with the service name
        String request = "type:~getPort_Request~serviceName:~" + serviceName;

        // Send the request to Communication and receive a response
        String response = handleCommunication(request);

        int port = parsePortFromResponse(response);

        if (port == -1) {
            // If the requested port is not available, find an available port
            port = findAvailablePort();

            // Update the request string with the allocated port
            request = "type:~registerMicroservice_Request~serviceName:~" + serviceName + "~Port:~" + port;

            // Send the request to Communication and receive a response
            response = handleCommunication(request);

            // Parse the allocated port from the response
            port = parseRegisterFromResponse(response);

            // Return the allocated port
            return port;
        } else {
            // Return the available port if it was found in the response
            return port;
        }
    }

    // Method for parsing the port from the response string
    private int parsePortFromResponse(String response) {
        String[] res = response.split("~");
        int port = Integer.parseInt(res[3]);
        return port;
    }

    // Method for parsing the registered port from the response string
    private int parseRegisterFromResponse(String response) {
        String[] res = response.split("~");
        int port = Integer.parseInt(res[4]);
        return port;
    }

    // Find an available port to use
    private int findAvailablePort() {
        int port = 3000;
        System.out.println("Manager is looking for a free port.\n");

        // Check if the port is occupied; increment it until an available port is found
        while (isPortOccupied(port)) {
            port++;
        }

        System.out.println("Manager found a free port on port " + port + ".\n");
        return port;
    }

    // Check if a port is occupied by attempting to create a new server socket
    private boolean isPortOccupied(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            // If no IOException occurs, the port is not occupied
            return false;
        } catch (IOException e) {
            // If an IOException occurs, the port is occupied
            return true;
        }
    }
}
