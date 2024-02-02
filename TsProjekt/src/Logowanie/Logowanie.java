package Logowanie;

import java.io.*;
import java.net.*;
import java.sql.*;

import ApiGateway.MicroserviceAgent;

public class Logowanie {
    static final String DB_URL = "jdbc:mysql://localhost/tst"; // Database URL
    static final String USER = "root"; // Database username
    static final String PASS = ""; // Database password

    public static void main(String[] args) {
        MicroserviceAgent microserviceAgent = MicroserviceAgent.getInstance(); // Create an instance of the Agent class

        try (ServerSocket server = new ServerSocket(microserviceAgent.getPort("Logowanie"))) {
            server.setReuseAddress(true); // Enable reusing the address
            System.out.println("The login microservice runs on the port: " + microserviceAgent.getPort("Logowanie"));

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
            microserviceAgent.stopMicroservice("Logowanie");
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

                    if (result[0].equals("type:login")) {
                        String[] login = result[1].split(":");
                        String[] password = result[2].split(":");
                        String QUERY = "SELECT id FROM uzytkownicy WHERE login='" + login[1] + "' AND haslo='" + password[1] + "';"; // SQL query for user verification

                        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                             Statement stmt = conn.createStatement()) {
                            ResultSet rs = stmt.executeQuery(QUERY); // Execute the query

                            if (!rs.next()) {
                                // If no user is found or password is incorrect, send an error message
                                out.println("type:login~status:BLAD");
                            } else {
                                // If login credentials are correct, confirm successful login
                                out.println("type:login~status:OK");
                            }
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
