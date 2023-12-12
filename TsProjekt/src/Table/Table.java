package Table;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ApiGateway.MicroserviceAgent;

public class Table {

    static final String DB_URL = "jdbc:mysql://localhost/tst";
    static final String USER = "root";
    static final String PASS = "";


    public static void main(String[] args) {
        // Initialization of the microservice agent
        MicroserviceAgent microserviceAgent = MicroserviceAgent.getInstance();
        // Getting the port on which the Table microservice is running
        int port = microserviceAgent.getMicroservicePort("Table");

        if (port == -1) {
            // If the microservice is not registered, display a message and exit the program
            System.err.println("The Table microservice is not registered.");
            return;
        }

        try (ServerSocket server = new ServerSocket(port)) {
            // Set the port address reuse option
            System.out.println("The table microservice runs on a port: " + port);

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

                    if (line.startsWith("type:table")) {
                        String QUERY = "SELECT nick, tresc, data FROM posty ORDER BY id DESC LIMIT 10";
                        StringBuilder response = new StringBuilder("type:table~contents:");
                        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                             Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(QUERY)) {

                            if (!rs.next()) {
                                response.append("BRAK");
                            } else {
                                do {
                                    // Create a response containing data from the database
                                    response.append("data-").append(rs.getString("data")).append(";nick-").append(rs.getString("nick")).append(";tresc-").append(rs.getString("tresc")).append(";");
                                } while (rs.next());
                            }
                            out.println(response);
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