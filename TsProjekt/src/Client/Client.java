package Client;

import java.io.*;
import java.net.*;
import java.util.*;

// Client class
class Client {

    // Helper method to display available commands
    static void pomoc() {
        String[] pomoct = {"Available commands:", "1. Register a new user",
                "2. Login",
                "3. Display the 10 latest posts",
                "4. Add posts",
                "5. Upload files to the cloud",
                "6. Download files from the cloud",
                "7. Logout",
                "8. Exit", "9. Help", "------------------------------------------------------------------------"};

        for (String s : pomoct) {
            System.out.println(s);
        }
    }

    public static void main(String[] args) {

        boolean cz = false; // Flag indicating whether the user is logged in
        String login = null; // Stores the username
        pomoc(); // View available commands to get started

        try (Socket socket = new Socket("localhost", 7777)) {

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            BufferedReader in
                    = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));

            Scanner sc = new Scanner(System.in);
            String line = null;

            while (!"8".equals(line)) { // Main loop, running until user selects option "8" (Exit)
                System.out.print("Select an option (1-8): ");
                line = sc.nextLine();

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
                        // Sending files to the cloud
                        if (!cz) {
                            System.out.println("Log in first");
                        } else {
                            System.out.print("Enter the name of the file to send: ");
                            String fileNameOut = sc.nextLine();
                            File fileOut = new File(fileNameOut);
                            if (fileOut.exists()) {
                                long fileSize = fileOut.length();
                                boolean isEmpty = fileSize == 0;
                                out.println("type:transfer~user_name:" + login + "~kind:out~file_name:" + fileNameOut + "~file_length:" + fileSize + "~is_empty:" + isEmpty);
                                out.flush();
                                if (!isEmpty) {
                                    FileInputStream fis = new FileInputStream(fileOut);
                                    BufferedInputStream bis = new BufferedInputStream(fis);
                                    int bytesRead;
                                    byte[] buffer = new byte[1024];
                                    while ((bytesRead = bis.read(buffer)) != -1) {
                                        out.println("type:transfer_out~data:" + Base64.getEncoder().encodeToString(Arrays.copyOfRange(buffer, 0, bytesRead)));
                                        out.flush();
                                    }
                                    bis.close();
                                }
                                out.println("type:transfer_out~status:END");
                                out.flush();
                                odp = in.readLine();
                                if ("type:transfer_out~status:OK".equals(odp)) {
                                    System.out.println("The file was successfully sent to the cloud.");
                                }
                            } else {
                                System.out.println("The specified file does not exist.");
                            }
                        }
                        break;

                    case "6":
                        // Downloading files from the cloud
                        if (!cz) {
                            System.out.println("Log in first");
                        } else {
                            System.out.print("Enter the name of the file to download: ");
                            String fileNameIn = sc.nextLine();
                            out.println("type:transfer~user_name:" + login + "~kind:in~file_name:" + fileNameIn);
                            out.flush();
                            odp = in.readLine();
                            if ("type:transfer_in~file_length:-1~first_byte_nr:0~offset:0~data:".equals(odp)) {
                                System.out.println("There is no such file on your cloud drive.");
                            } else {
                                File fileIn = new File(fileNameIn);
                                FileOutputStream fos = new FileOutputStream(fileIn);
                                BufferedOutputStream bos = new BufferedOutputStream(fos);
                                while (true) {
                                    odp = in.readLine();
                                    if ("type:transfer_in~status:END".equals(odp)) {
                                        break;
                                    }
                                    String data = odp.split("data:")[1];
                                    bos.write(Base64.getDecoder().decode(data));
                                }
                                bos.close();
                                System.out.println("The file was successfully downloaded from the cloud.");
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
}
