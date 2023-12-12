package Czat;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import ApiGateway.MicroserviceAgent;

public class Czat {

    static final String DB_URL = "jdbc:mysql://localhost/tst";
    static final String USER = "root";
    static final String PASS = "";


    public static void main(String[] args) {
        // Initialization of the microservice agent
        MicroserviceAgent microserviceAgent = MicroserviceAgent.getInstance();
        // Downloading the port on which the Chat microservice runs
        int port = microserviceAgent.getMicroservicePort("Czat");

        if (port == -1) {
            // If the microservice is not registered, display a message and exit the program
            System.err.println("The Chat microservice is not registered.");
            return;
        }

        try (ServerSocket server = new ServerSocket(port)) {
            // Set the port address reuse option
            server.setReuseAddress(true);
            System.out.println("The chat microservice runs on the port: " + port);

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
                    if (result[0].equals("type:czat")) {
                        String[] login = result[1].split(":");
                        String[] contents = result[2].split(":");

                        String updateQuery = "INSERT into posty (nick, tresc) values('" + login[1] + "', '" + contents[1] + "');";

                        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                             Statement stmt = conn.createStatement()) {
                            // Execute a SQL query to add the message to the database
                            stmt.executeUpdate(updateQuery);
                            // Send a reply to the client about the status of the operation
                            out.println("type:czat~status:OK");
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
