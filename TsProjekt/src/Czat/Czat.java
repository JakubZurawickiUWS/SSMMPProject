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

    private static boolean shouldContinue = true;

    public static void main(String[] args) {
        // Inicjalizacja agenta mikroserwisu
        MicroserviceAgent microserviceAgent = MicroserviceAgent.getInstance();
        // Pobranie portu, na którym działa mikroserwis Czat
        int port = microserviceAgent.getMicroservicePort("Czat");

        if (port == -1) {
            // Jeśli mikroserwis nie jest zarejestrowany, wyświetl komunikat i zakończ program
            System.err.println("Mikroserwis Czat nie jest zarejestrowany.");
            return;
        }

        try (ServerSocket server = new ServerSocket(port)) {
            // Ustawienie opcji wielokrotnego użycia adresu portu
            server.setReuseAddress(true);
            System.out.println("Mikroserwis czat działa na porcie: " + port);

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
                    if (result[0].equals("type:czat")) {
                        String[] login = result[1].split(":");
                        String[] contents = result[2].split(":");

                        String updateQuery = "INSERT into posty (nick, tresc) values('" + login[1] + "', '" + contents[1] + "');";

                        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                             Statement stmt = conn.createStatement()) {
                            // Wykonaj zapytanie SQL w celu dodania wiadomości do bazy danych
                            stmt.executeUpdate(updateQuery);
                            // Wyślij odpowiedź do klienta o statusie operacji
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
