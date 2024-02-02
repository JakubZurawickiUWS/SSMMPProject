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

    static final String DB_URL = "jdbc:mysql://localhost/tst"; // Database URL
    static final String USER = "root"; // Database username
    static final String PASS = ""; // Database password

    public static void main(String[] args) {
        MicroserviceAgent microserviceAgent = MicroserviceAgent.getInstance(); // Create an instance of the Agent class

        try (ServerSocket server = new ServerSocket(microserviceAgent.getPort("Table"))) {
            server.setReuseAddress(true); // Enable reusing the address
            System.out.println("The table microservice runs on the port: " + microserviceAgent.getPort("Table"));

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
            microserviceAgent.stopMicroservice("Table");
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
