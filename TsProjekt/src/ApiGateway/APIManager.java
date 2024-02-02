package ApiGateway;

import java.io.*;
import java.net.*;

public class APIManager {
    public static void main(String[] args) {
        startAPIManager();
    }

    private static void startAPIManager() {
        System.out.println("APIManager is working.\n");

        try (ServerSocket server = createServerSocket()) {
            while (true) {
                Socket client = acceptClientConnection(server);
                new Thread(new ClientHandler(client)).start(); // Start a new thread to handle each client
            }
        } catch (IOException e) {
            handleIOException(e, "Error during socket setup or client handling");
        }
    }

    private static ServerSocket createServerSocket() throws IOException {
        ServerSocket server = new ServerSocket(3000); // Create a server socket on port 3000
        server.setReuseAddress(true); // Allow reusing the address after the socket is closed
        return server;
    }

    private static Socket acceptClientConnection(ServerSocket server) throws IOException {
        Socket client = server.accept(); // Accept a client connection
        System.out.println("Connection has been established.\n");
        return client;
    }

    private static void handleIOException(IOException e, String errorMessage) {
        e.printStackTrace();
        System.err.println(errorMessage);
    }

    // Inner class to handle communication with a client
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
            ) {
                handleClientCommunication(out, in); // Handle communication with the client
            } catch (IOException e) {
                handleIOException(e, "Error during communication with the client");
            } finally {
                closeClientSocket(); // Close the client socket when done
            }
        }

        private void handleClientCommunication(PrintWriter out, BufferedReader in) throws IOException {
            String line;
            while ((line = in.readLine()) != null) { // Read messages from the client
                String[] result = line.split("~");
                String messageType = result[1];
                String serviceName = result[3];

                switch (messageType) {
                    case "Microservice_Port_Request":
                        sendMicroservicePortResponse(out, serviceName); // Respond to port request
                        break;
                    case "delete_Agent":
                        deleteMicroserviceAgent(out, serviceName); // Handle agent deletion request
                        break;
                    default:
                        sendError(out); // Respond with an error for unknown messages
                        break;
                }
            }
        }

        private void sendError(PrintWriter out) {
            String response = "type:~Error~Text~Invalid type name";
            out.println(response); // Send an error response to the client
        }

        private void sendMicroservicePortResponse(PrintWriter out, String serviceName) {
            String response = "type:~Microservice_Port_Response~ Port: ~" + Manager.getInstance().registerMicroservice(serviceName);
            out.println(response); // Send the microservice port response to the client
        }

        private void deleteMicroserviceAgent(PrintWriter out, String serviceName) {
            System.out.println("Microservice Agent '" + serviceName + "' has been unregistered.\n");
            // Perform any necessary cleanup when an agent is unregistered
        }

        private void closeClientSocket() {
            try {
                clientSocket.close(); // Close the client socket
            } catch (IOException e) {
                handleIOException(e, "Error while closing the client socket");
            }
        }
    }
}
