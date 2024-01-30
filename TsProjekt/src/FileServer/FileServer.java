package FileServer;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import ApiGateway.Agent;

public class FileServer {

    public static void main(String[] args) {
        Agent agent = Agent.getInstance(); // Create an instance of the Agent class

        try (ServerSocket server = new ServerSocket(agent.getPort("FileServer"))) {
            server.setReuseAddress(true); // Enable reusing the address
            System.out.println("The fileserver microservice runs on the port: " + agent.getPort("FileServer"));

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
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
            ) {
                String line = in.readLine();
                String[] result = line.split("~");

                if (result[2].equals("kind:out")) {
                    handleFileTransferFromClient(result[7], result[3].split(":")[1], out); // Handle incoming file from client
                } else if (result[2].equals("kind:in")) {
                    handleFileTransferFromServer(result, out); // Handle outgoing file to client
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

        private void handleFileTransferFromClient(String data, String fileName, PrintWriter out) {
            String folderName = "SentFile";
            Path folderPath = Paths.get(System.getProperty("user.home"), folderName);
            if (!Files.exists(folderPath)) {
                try {
                    Files.createDirectories(folderPath); // Create directory if it doesn't exist
                } catch (IOException e) {
                    System.err.println("Error " + e.getMessage());
                }
            }
            Path filePath = folderPath.resolve(fileName);
            try {
                byte[] fileBytes = Base64.getDecoder().decode(data); // Decode Base64 data to bytes
                Files.write(filePath, fileBytes); // Save bytes to file
                sendResponse(out, "type:transfer_out~status:OK");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleFileTransferFromServer(String[] result, PrintWriter out) {
            String fileName = result[3].split(":")[1];
            Path folderPath = Paths.get(System.getProperty("user.home"), "SentFile");
            Path filePath = folderPath.resolve(fileName);
            File file = new File(filePath.toString());

            try {
                if (file.exists()) {
                    byte[] fileBytes = Files.readAllBytes(filePath);
                    String encodedData = Base64.getEncoder().encodeToString(fileBytes); // Encode file data to Base64
                    sendResponse(out, "type:transfer_in~status:~200~data~" + encodedData);
                } else {
                    sendResponse(out, "type:transfer_in~status:~401");
                }
            } catch (IOException e) {
                e.printStackTrace();
                sendResponse(out, "type:transfer_in~status:~400");
            }
        }

        private void sendResponse(PrintWriter out, String response) {
            out.println(response);
            out.close();
        }
    }
}
