package Rejestracja;

import java.io.*;
import java.net.*;
import java.sql.*;

import ApiGateway.MicroserviceManager;

public class Rejestracja {
    static final String DB_URL = "jdbc:mysql://localhost/tst"; // Database URL
    static final String USER = "root"; // Database username
    static final String PASS = ""; // Database password

    public static void main(String[] args) {
        MicroserviceManager managerAgent = MicroserviceManager.getInstance(); // Get instance of ManagerAgent
        int port = managerAgent.getMicroservicePort("Rejestracja"); // Retrieve the port number for this microservice

        if (port == -1) {
            System.err.println("The Registration microservice is not registered.");
            return;
        }

        try (ServerSocket server = new ServerSocket(port)) {
            server.setReuseAddress(true); // Enable reusing the address
            System.out.println("The registration microservice runs on the port: " + port);

            try {
                Socket client = server.accept(); // Accept a client connection
                new Thread(new ClientHandler(client)).start(); // Handle the client in a new thread
            } catch (SocketException e) {
                System.out.println("Server Socket is closed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
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

                    if (result[0].equals("type:register")) {
                        String[] login = result[1].split(":");
                        String[] haslo = result[2].split(":");
                        String QUERY = "SELECT id From uzytkownicy where login='" + login[1] + "';"; // SQL query to check user existence

                        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                             Statement stmt = conn.createStatement()) {

                            ResultSet rs = stmt.executeQuery(QUERY); // Execute the query

                            if (!rs.next()) {
                                // If user does not exist, insert new user
                                String Update = "INSERT into uzytkownicy (login,haslo) values('" + login[1] + "','" + haslo[1] + "');";
                                stmt.executeUpdate(Update);
                                out.println("type:register~status:OK");
                            } else {
                                // If user exists, send an error message
                                out.println("type:register~status:BLAD");
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
