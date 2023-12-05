package Client;

import java.io.*;
import java.net.*;
import java.util.*;

import ApiGateway.MicroserviceAgent;

// Klasa klienta
class Client {

    // Metoda pomocnicza do wyświetlania dostępnych poleceń
    static void pomoc() {
        String[] pomoct = {"Dostępne polecenia:", "1. Rejestracja nowego użytkownika",
                "2. Logowanie",
                "3. Wyświetlanie 10 ostatnich postów",
                "4. Dodawanie postów",
                "5. Wysyłanie plików do chmury",
                "6. Pobieranie plików z chmury",
                "7. Wylogowywanie",
                "8. Wyjście", "9. Pomoc", "------------------------------------------------------------------------"};
        for (String s : pomoct) {
            System.out.println(s);
        }
    }

    public static void main(String[] args) {

        boolean cz = false; // Flaga oznaczająca, czy użytkownik jest zalogowany
        String login = null; // Przechowuje nazwę użytkownika
        pomoc(); // Wyświetlanie dostępnych poleceń na początek

        try (Socket socket = new Socket("localhost", 1234)) {

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            BufferedReader in
                    = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));

            Scanner sc = new Scanner(System.in);
            String line = null;

            while (!"8".equals(line)) { // Pętla główna, działająca dopóki użytkownik nie wybierze opcji "8" (Wyjście)
                System.out.print("Wybierz opcję (1-8): ");
                line = sc.nextLine();

                switch (line) {
                    case "1":
                        // Rejestracja nowego użytkownika
                        System.out.print("Podaj nazwę użytkownika: ");
                        String username = sc.nextLine();
                        System.out.print("Podaj hasło: ");
                        String password = sc.nextLine();
                        out.println("type:register~user_name:" + username + "~password:" + password);
                        out.flush();
                        String odp = in.readLine();
                        if ("type:register~status:OK".equals(odp)) {
                            System.out.println("Zarejestrowano poprawnie użytkownika " + username);
                        } else {
                            System.out.println("Ten użytkownik już istnieje");
                        }
                        break;

                    case "2":
                        // Logowanie
                        System.out.print("Podaj nazwę użytkownika: ");
                        username = sc.nextLine();
                        System.out.print("Podaj hasło: ");
                        password = sc.nextLine();
                        out.println("type:login~user_name:" + username + "~password:" + password);
                        out.flush();
                        odp = in.readLine();
                        if ("type:login~status:OK".equals(odp)) {
                            login = username;
                            cz = true;
                            System.out.println("Zalogowano jako " + login);
                        } else {
                            System.out.println("Niepoprawne dane logowania");
                        }
                        break;

                    case "3":
                        // Wyświetlanie 10 ostatnich postów
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
                        // Dodawanie postów
                        if (!cz) {
                            System.out.println("Najpierw się zaloguj");
                        } else {
                            System.out.print("Wpisz post: ");
                            String postContents = sc.nextLine();
                            out.println("type:czat~login:" + login + "~contents:" + postContents);
                            out.flush();
                            odp = in.readLine();
                            if ("type:czat~status:OK".equals(odp)) {
                                System.out.println("Post został poprawnie dodany do tablicy.");
                            }
                        }
                        break;

                    case "5":
                        // Wysyłanie plików do chmury
                        if (!cz) {
                            System.out.println("Najpierw się zaloguj");
                        } else {
                            System.out.print("Podaj nazwę pliku do wysłania: ");
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
                                    System.out.println("Plik został poprawnie wysłany do chmury.");
                                }
                            } else {
                                System.out.println("Podany plik nie istnieje.");
                            }
                        }
                        break;

                    case "6":
                        // Pobieranie plików z chmury
                        if (!cz) {
                            System.out.println("Najpierw się zaloguj");
                        } else {
                            System.out.print("Podaj nazwę pliku do pobrania: ");
                            String fileNameIn = sc.nextLine();
                            out.println("type:transfer~user_name:" + login + "~kind:in~file_name:" + fileNameIn);
                            out.flush();
                            odp = in.readLine();
                            if ("type:transfer_in~file_length:-1~first_byte_nr:0~offset:0~data:".equals(odp)) {
                                System.out.println("Na Twoim dysku w chmurze nie ma takiego pliku.");
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
                                System.out.println("Plik został poprawnie pobrany z chmury.");
                            }
                        }
                        break;

                    case "7":
                        // Wylogowywanie
                        if (!cz) {
                            System.out.println("Najpierw się zaloguj");
                        } else {
                            login = null;
                            cz = false;
                            System.out.println("Wylogowano pomyślnie");
                        }
                        break;

                    case "8":
                        // Wyjście
                        login = null;
                        cz = false;
                        socket.close();
                        MicroserviceAgent agent = MicroserviceAgent.getInstance();
                        agent.stopMicroservices(); // Zamykanie mikroserwisów
                        if (agent.areAllMicroservicesStopped()) {
                            System.out.println("Wszystkie mikroserwisy zostały zamknięte pomyślnie.");
                        } else {
                            System.out.println("Nie udało się zamknąć wszystkich mikroserwisów.");
                        }
                        System.out.println("Do widzenia");
                        System.exit(0);
                        break;

                    case "9":
                        // Wyświetlanie pomocy
                        pomoc();
                        break;

                    default:
                        System.out.println("Niepoprawna opcja. Wybierz liczbę od 1 do 8 lub wpisz 9 aby wyświetlić pomoc.");
                }
            }
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
