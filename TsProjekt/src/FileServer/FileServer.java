package FileServer;

import java.io.*;
import java.net.*;
import java.util.Base64;

import ApiGateway.MicroserviceAgent;

public class FileServer {

    private static boolean shouldContinue = true;

    public static void main(String[] args) {
        // Inicjalizacja agenta mikroserwisu
        MicroserviceAgent microserviceAgent = MicroserviceAgent.getInstance();
        // Pobranie portu, na którym działa mikroserwis FileServer
        int port = microserviceAgent.getMicroservicePort("FileServer");

        if (port == -1) {
            // Jeśli mikroserwis nie jest zarejestrowany, wyświetl komunikat i zakończ program
            System.err.println("Mikroserwis FileServer nie jest zarejestrowany.");
            return;
        }

        try (ServerSocket server = new ServerSocket(port)) {
            // Ustawienie opcji wielokrotnego użycia adresu portu
            server.setReuseAddress(true);
            System.out.println("Mikroserwis fileserver działa na porcie: " + port);

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

                    if (result[2].equals("kind:out")) {
                        // Obsługa przesyłania pliku od klienta na serwer

                        String odp = line;
                        String[] u = result[1].split(":");
                        String[] plikn = result[3].split(":");
                        File file = new File(u[1] + "/" + plikn[1]);
                        File directory = new File(u[1]);

                        if (!directory.exists()) {
                            directory.mkdir();
                        }

                        if (file.exists()) {
                            file.delete();
                        }

                        try (
                                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))
                        ) {

                            byte[] data;

                            String tmp;

                            String[] result2 = odp.split("~");
                            int offset;
                            long dl;
                            long pb;

                            if (!odp.contains("file_length:0")) {

                                dl = Long.parseLong(result2[4].split(":")[1]);
                                pb = Long.parseLong(odp.split("~")[5].split(":")[1]);
                                offset = Integer.parseInt(odp.split("~")[6].split(":")[1]);

                                tmp = odp.split("data:")[1];
                                {
                                    data = Base64.getDecoder().decode(tmp);

                                    bos.write(data);
                                }

                                boolean ostatni = pb + offset == dl;

                                while (!ostatni) {

                                    odp = in.readLine();

                                    dl = Long.parseLong(result2[4].split(":")[1]);
                                    pb = Long.parseLong(odp.split("~")[5].split(":")[1]);
                                    offset = Integer.parseInt(odp.split("~")[6].split(":")[1]);

                                    if (pb + offset == dl) {
                                        ostatni = true;
                                    }

                                    tmp = odp.split("data:")[1];
                                    data = Base64.getDecoder().decode(tmp);
                                    bos.write(data);

                                }
                            }
                            out.println("type:transfer_out~status:OK");
                        }

                    } else if (result[2].equals("kind:in")) {
                        // Obsługa przesyłania pliku z serwera do klienta

                        String[] u = result[1].split(":");
                        String[] plikn = result[3].split(":");

                        File file = new File(u[1] + "/" + plikn[1]);

                        if (file.exists()) {

                            long rozmiar = file.length();

                            if (rozmiar > 0) {
                                FileInputStream fis = new FileInputStream(file);
                                BufferedInputStream bis = new BufferedInputStream(fis);

                                int k;
                                long ostatniewyslane = 0;
                                byte[] data = new byte[512];
                                long liczbajty = 0;

                                while ((k = bis.read(data)) != -1) {
                                    String s = Base64.getEncoder().encodeToString(data);
                                    out.println("type:transfer_in~file_length:" + rozmiar + "~first_byte_nr:" + ostatniewyslane + "~offset:" + k + "~data:" + s);
                                    out.flush();

                                    for (int i = 0; i < 512; i++) {
                                        data[i] = 0;
                                    }

                                    liczbajty = liczbajty + k;
                                    ostatniewyslane = liczbajty;
                                }
                                bis.close();
                            } else {
                                out.println("type:transfer_in~file_length:0~first_byte_nr:0~offset:0~data:");
                                out.flush();
                            }
                        } else {
                            out.println("type:transfer_in~file_length:-1~first_byte_nr:0~offset:0~data:");
                            out.flush();
                        }

                        out.println("type:transfer_in~satus:END");
                        out.flush();
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
