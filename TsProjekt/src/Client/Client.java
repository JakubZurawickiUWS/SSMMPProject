package Client;

import ApiGateway.AgentKlient;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

// Client class
class Client {

    // Helper method to display available commands
    static void pomoc() {
        // Array of help commands
        String[] pomoct = {
                "╔══════════════════════════════════════════════════════════════════╗",
                "║                       Available Commands:                        ║",
                "╟──────────────────────────────────────────────────────────────────╢",
                "║  1. Register a new user                                          ║",
                "║  2. Login                                                        ║",
                "║  3. Display the 10 latest posts                                  ║",
                "║  4. Add posts                                                    ║",
                "║  5. Upload files to the cloud                                    ║",
                "║  6. Download files from the cloud                                ║",
                "║  7. Logout                                                       ║",
                "║  8. Exit                                                         ║",
                "║  9. Help                                                         ║",
                "╚══════════════════════════════════════════════════════════════════╝"
        };

        for (String s : pomoct) {
            System.out.println(s);
        }
    }

    public static void main(String[] args) {
        boolean cz = false; // Flag indicating whether the user is logged in
        String login = null; // Stores the username
        pomoc(); // Display available commands

        AgentKlient agentKlient = AgentKlient.getInstance();

        try (Socket socket = new Socket("localhost", agentKlient.getPort())) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner sc = new Scanner(System.in);
            String line = null;

            while (!"8".equals(line)) { // Main loop, exits when user selects "8" (Exit)
                System.out.print("Select an option (1-8): ");
                line = sc.nextLine();

                // Switch statement to handle different user inputs
                switch (line) {
                    case "1":
                        // New user registration
                        System.out.print("Enter your username: ");
                        String username = sc.nextLine();
                        System.out.print("Enter the password: ");
                        String password = sc.nextLine();
                        out.println("type:register~user_name:" + username + "~password:" + password);
                        out.flush();
                        String odp = in.readLine();
                        if ("type:register~status:OK".equals(odp)) {
                            System.out.println("User registered successfully " + username);
                        } else {
                            System.out.println("This user already exists");
                        }
                        break;

                    case "2":
                        // Login
                        System.out.print("Enter your username: ");
                        username = sc.nextLine();
                        System.out.print("Enter the password: ");
                        password = sc.nextLine();
                        out.println("type:login~user_name:" + username + "~password:" + password);
                        out.flush();
                        odp = in.readLine();
                        if ("type:login~status:OK".equals(odp)) {
                            login = username;
                            cz = true;
                            System.out.println("Logged in as " + login);
                        } else {
                            System.out.println("Incorrect login details");
                        }
                        break;

                    case "3":
                        // Showing the last 10 posts
                        out.println("type:table");
                        out.flush();
                        odp = in.readLine();
                        String[] result2 = odp.split("contents:");
                        String[] zaw = result2[1].split(";");
                        for (int i = 0; i < zaw.length; i++) {
                            System.out.println(zaw[i]);
                            if (i % 3 == 2) System.out.println();
                        }
                        break;

                    case "4":
                        // Adding posts
                        if (!cz) {
                            System.out.println("Log in first");
                        } else {
                            System.out.print("Enter post: ");
                            String postContents = sc.nextLine();
                            out.println("type:czat~login:" + login + "~contents:" + postContents);
                            out.flush();
                            odp = in.readLine();
                            if ("type:czat~status:OK".equals(odp)) {
                                System.out.println("The post has been successfully added to the board.");
                            }
                        }
                        break;

                    case "5":
                        System.out.println("The option selected is: File transfer - File upload");
                        if (!cz) {
                            return;
                        }

                        System.out.println("Enter the file path:");
                        String filePath = sc.nextLine();

                        File plik = new File(filePath);


                        if (plik.exists()) {

                            String fileName = Paths.get(filePath).getFileName().toString();


                            // We read and encode file data to Base64
                            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
                            String encodedData = Base64.getEncoder().encodeToString(fileBytes);

                            // We send Base64 encoded data to the server
                            out.println(formatSentFileRequest(login, fileName, encodedData));

                            // We are waiting for a response from the server
                            out.flush();

                            String respond = in.readLine();
                            if ("type:transfer_out~status:OK".equals(respond)) {
                                System.out.println("The file was successfully sent to the cloud.");
                            }
                        } else {
                            System.out.println("The file does not exist in the specified path.");
                        }
                        break;

                    case "6":
                        // Downloading files from the cloud
                        if (!cz) {
                            System.out.println("Log in first");
                        } else {
                            System.out.print("Enter the name of the file to download: ");
                            String fileNameIn = sc.nextLine();
                            System.out.println("Enter the name you want to save the file under:");
                            String fileNameSave = sc.nextLine();

                            out.println(formatDownloadFileRequest(login, fileNameIn));
                            out.flush();
                            String respond = in.readLine();
                            String[] strTemp = splitW(respond);
                            String stat = strTemp[2];
                            if (stat.equals("200")) {
                                String daneRes = strTemp[4];

                                // We decode the Base64 data into bytes
                                byte[] fileBytes = Base64.getDecoder().decode(daneRes);

                                // We write the bytes to the file
                                try (FileOutputStream fos = new FileOutputStream(fileNameSave)) {
                                    fos.write(fileBytes);
                                    System.out.println("The file was successfully downloaded from the cloud.");

                                } catch (IOException e) {
                                    System.out.println("Error reading data");
                                }
                            } else {
                                System.out.println("There is no such file on your cloud drive.");

                            }
                        }
                        break;

                    case "7":
                        // Logging out
                        if (!cz) {
                            System.out.println("Log in first");
                        } else {
                            login = null;
                            cz = false;
                            System.out.println("You have logged out successfully");
                        }
                        break;

                    case "8":
                        // Exit
                        login = null;
                        cz = false;
                        socket.close();
                        System.out.println("Goodbye");
                        System.exit(0);
                        break;

                    case "9":
                        // View help
                        pomoc();
                        break;

                    default:
                        System.out.println("Invalid option. Select a number from 1 to 8 or enter 9 to view help.");
                }
            }
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to format file upload request
    private static String formatSentFileRequest(String login, String fileNameOut, String dane) {
        return ("type:transfer_out~user_name:" + login + "~kind:out~file_name:" + fileNameOut + "~file_length:" + 540 + "~is_empty:" + true + "~Dane~" + dane);
    }

    // Method to format file download request
    private static String formatDownloadFileRequest(String login, String fileNameIn) {
        return "type:transfer_in~user_name:" + login + "~kind:in~file_name:" + fileNameIn;
    }

    // Method to split response string
    private static String[] splitW(String respond) {
        return respond.split("~");
    }
}
