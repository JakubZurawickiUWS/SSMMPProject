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
            server.setReuseAddress(true);

            while (true) {
                Socket client = server.accept();
                new Thread(new ClientHandler(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inner class handling the client.
     */
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
                String line;
                while ((line = in.readLine()) != null) {
                    String[] result = line.split("~");

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
                        case "type:transfer_out":
                        case "type:transfer_in":
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
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Method for forwarding a request to another server.
         *
         * @param request     The request to be forwarded.
         * @param serviceName Microservice name.
         * @param out         Output stream to the client.
         */
        private void forwardRequest(String request, String serviceName, PrintWriter out) throws InterruptedException {
            MicroserviceAgent agent = MicroserviceAgent.getInstance();
            agent.addMicroservice(serviceName);
            int port = agent.getMicroservicePort(serviceName);

            Thread.sleep(100);
            if (port != -1) {
                try (Socket socket = new Socket("localhost", port)) {
                    PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    serverOut.println(request);
                    serverOut.flush();
                    String response = serverIn.readLine();

                    out.println(response);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally{
                    agent.deleteMicroservice(serviceName);
                }
            } else {
                System.err.println("Microservice " + serviceName + " is not registered.");
            }
        }
    }
}
