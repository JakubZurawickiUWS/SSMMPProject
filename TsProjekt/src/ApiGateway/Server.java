package ApiGateway;

import java.io.*;
import java.net.*;

/**
 * Main class representing the API gateway.
 */
class ApiGateway {
    public static void main(String[] args) {
        System.out.println("Api Gateway");
        try (ServerSocket server = new ServerSocket(7777)) {
            server.setReuseAddress(true); // Allow the socket to be bound even if a previous connection is in TIME_WAIT state

            // Continuously listen for client connections
            while (true) {
                Socket client = server.accept(); // Accepts a connection from a client
                new Thread(new ClientHandler(client)).start(); // Start a new thread for each client
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inner class handling the client.
     */
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket; // Client connection

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
            ) {
                String line;
                // Read requests from the client
                while ((line = in.readLine()) != null) {
                    String[] result = line.split("~");

                    // Determine the type of request and forward it to the respective microservice
                    switch (result[0]) {
                        case "type:register":
                            forwardRequest(line, "Rejestracja", out);
                            break;
                        case "type:login":
                            forwardRequest(line, "Logowanie", out);
                            break;
                        case "type:czat":
                            forwardRequest(line, "Czat", out);
                            break;
                        case "type:table":
                            forwardRequest(line, "Table", out);
                            break;
                        case "type:transfer_out", "type:transfer_in":
                            forwardRequest(line, "FileServer", out);
                            break;
                        default:
                            System.out.println("Invalid data!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    clientSocket.close(); // Close client connection
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Method for forwarding a request to another server.
         *
         * @param request     The request to be forwarded.
         * @param serviceName Name of the microservice to which the request is forwarded.
         * @param out         Output stream to the client.
         */
        private void forwardRequest(String request, String serviceName, PrintWriter out) throws InterruptedException {
            MicroserviceManager agent = MicroserviceManager.getInstance(); // Get the singleton instance of ManagerAgent
            agent.addMicroservice(serviceName); // Register and start the microservice
            int port = agent.getMicroservicePort(serviceName); // Get the port of the microservice

            Thread.sleep(100); // Sleep for a short period (usually for ensuring the microservice is up and running)
            if (port != -1) {
                try (Socket socket = new Socket("localhost", port)) {
                    PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    serverOut.println(request); // Forward the request to the microservice
                    serverOut.flush();
                    String response = serverIn.readLine(); // Read the response

                    out.println(response); // Send the response back to the client
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    agent.deleteMicroservice(serviceName); // Stop and unregister the microservice
                }
            } else {
                System.err.println("Microservice " + serviceName + " is not registered.");
            }
        }
    }
}
