package ApiGateway;

import java.io.*;
import java.net.*;

/**
 * Główna klasa reprezentująca bramę API.
 */
class ApiGateway {
    public static void main(String[] args) {
        System.out.println("Api Gateway");
        try (ServerSocket server = new ServerSocket(1234)) {
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
     * Wewnętrzna klasa obsługująca klienta.
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
                            System.out.println("Nieprawidłowe dane!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Metoda do przekazywania żądania do innego serwera.
         *
         * @param request     Żądanie do przekazania.
         * @param serviceName Nazwa mikroserwisu.
         * @param out         Strumień wyjścia do klienta.
         */
        private void forwardRequest(String request, String serviceName, PrintWriter out) {
            MicroserviceAgent agent = MicroserviceAgent.getInstance();
            int port = agent.getMicroservicePort(serviceName);

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
                }
            } else {
                System.err.println("Mikroserwis " + serviceName + " nie jest zarejestrowany.");
            }
        }
    }
}
