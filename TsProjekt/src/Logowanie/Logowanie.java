package Logowanie;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ApiGateway.MicroserviceAgent;

public class Logowanie {
    static final String DB_URL = "jdbc:mysql://localhost/tst";
    static final String USER = "root";
    static final String PASS = "";

    private static boolean shouldContinue = true;

    public static void main(String[] args) {
        // Inicjalizacja agenta mikroserwisu
        MicroserviceAgent microserviceAgent = MicroserviceAgent.getInstance();
        // Pobranie portu, na którym działa mikroserwis Logowanie
        int port = microserviceAgent.getMicroservicePort("Logowanie");

        if (port == -1) {
            // Jeśli mikroserwis nie jest zarejestrowany, wyświetl komunikat i zakończ program
            System.err.println("Mikroserwis Logowanie nie jest zarejestrowany.");
            return;
        }

        try (ServerSocket server = new ServerSocket(port)) {
            // Ustawienie opcji wielokrotnego użycia adresu portu
            server.setReuseAddress(true);
            System.out.println("Mikroserwis logowanie działa na porcie: " + port);

            while (shouldContinue) {
                try {
                    // Akceptuj połączenia od klientów
                    Socket client = server.accept();
                    // Uruchom nowy wątek do obsługi klienta
                    new Thread(new ClientHandler(client)).start();
                } catch (SocketException e) {
                    System.out.println("Server Socket jest zamknięty.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Klasa obsługująca klienta
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
                    if ("type:stop".equals(line)) {
                        // Jeśli otrzymaliśmy sygnał zatrzymania, ustaw shouldContinue na false i przerwij pętlę
                        shouldContinue = false;
                        break;
                    }

                    String[] result = line.split("~");
                    if (result[0].equals("type:login")) {
                        String[] login = result[1].split(":");
                        String[] haslo = result[2].split(":");
                        String QUERY = "SELECT id FROM uzytkownicy WHERE login='" + login[1] + "' AND haslo='" + haslo[1] + "';";

                        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                             Statement stmt = conn.createStatement()) {
                            ResultSet rs = stmt.executeQuery(QUERY);

                            if (!rs.next()) {
                                // Jeśli użytkownik nie istnieje lub hasło jest nieprawidłowe, zwróć odpowiedni status
                                out.println("type:login~status:BLAD");
                            } else {
                                // Jeśli użytkownik istnieje i hasło jest prawidłowe, zwróć odpowiedni status
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
                        // Zamknij gniazdo klienta
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
