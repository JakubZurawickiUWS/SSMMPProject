package Czat;

import java.io.*;
import java.net.*;
import java.sql.*;

import ApiGateway.MicroserviceAgent;

public class Czat {

    static final String DB_URL = "jdbc:mysql://localhost/tst"; // Database URL
    static final String USER = "root"; // Database username
    static final String PASS = ""; // Database password

    public static void main(String[] args) {
        MicroserviceAgent microserviceAgent = MicroserviceAgent.getInstance(); // Create an instance of the Agent class

        try (ServerSocket server = new ServerSocket(microserviceAgent.getPort("Czat"))) {
            server.setReuseAddress(true); // Enable reusing the address
            System.out.println("The chat microservice runs on the port: " + microserviceAgent.getPort("Czat"));

            while (true) {

                try {
                    server.setSoTimeout(10000); // Set a timeout of 10 seconds
                    Socket client = server.accept(); // Accept a client connection within the timeout
                    server.setSoTimeout(0); // Reset the timeout to infinite

                    new Thread(new ClientHandler(client)).start(); // Handle the client in a new thread
                } catch (SocketTimeoutException ste) {
                    // Timeout occurred, no client connected within 10 seconds
                    break;

                } catch (IOException e) {
                    e.printStackTrace(); // Handle the exception appropriately
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            microserviceAgent.stopMicroservice("Czat");
        }
    }

    // Class for handling client communication
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket; // Socket to communicate with the client

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
            ) {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] result = line.split("~");

                    if (result[0].equals("type:czat")) {
                        String[] login = result[1].split(":");
                        String[] contents = result[2].split(":");

                        String updateQuery = "INSERT into posty (nick, tresc) values('" + login[1] + "', '" + contents[1] + "');"; // SQL query to insert chat message

                        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                             Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate(updateQuery); // Execute the SQL query
                            out.println("type:czat~status:OK"); // Send a response to the client
                            out.flush();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (!clientSocket.isClosed()) {
                        clientSocket.close(); // Ensure the client socket is closed
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
