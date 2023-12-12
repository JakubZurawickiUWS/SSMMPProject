package Rejestracja;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ApiGateway.MicroserviceAgent;

public class Rejestracja {
    static final String DB_URL = "jdbc:mysql://localhost/tst";
    static final String USER = "root";
    static final String PASS = "";

    public static void main(String[] args) {
        // Initialization of the microservice agent
        MicroserviceAgent microserviceAgent = MicroserviceAgent.getInstance();
        // Downloading the port on which the Registration microservice runs
        int port = microserviceAgent.getMicroservicePort("Rejestracja");

        if (port == -1) {
            // If the microservice is not registered, display a message and exit the program
            System.err.println("The Registration microservice is not registered.");
            return;
        }

        try (ServerSocket server = new ServerSocket(port)) {
            // Set the port address reuse option
            server.setReuseAddress(true);
            System.out.println("The registration microservice runs on the port: " + port);

                try {
                    // Accept calls from customers
                    Socket client = server.accept();
                    // Start a new customer support thread
                    new Thread(new ClientHandler(client)).start();
                } catch (SocketException e) {
                    System.out.println("Server Socket is closed.");
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Customer service class
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                String line;
                while ((line = in.readLine()) != null) {

                    String[] result = line.split("~");
                    if (result[0].equals("type:register")) {
                        String[] login = result[1].split(":");
                        String[] haslo = result[2].split(":");
                        String QUERY = "SELECT id From uzytkownicy where login='" + login[1] + "';";

                        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                             Statement stmt = conn.createStatement()) {

                            ResultSet rs = stmt.executeQuery(QUERY);

                            if (!rs.next()) {
                                // If the user does not exist, add him to the database
                                String Update = "INSERT into uzytkownicy (login,haslo) values('" + login[1] + "','" + haslo[1] + "');";
                                stmt.executeUpdate(Update);
                                out.println("type:register~status:OK");
                            } else {
                                // If the user already exists, return the appropriate status
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
                        // Close the client socket
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
