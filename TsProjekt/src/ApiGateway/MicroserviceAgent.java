package ApiGateway;

import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.net.*;

/**
 * Klasa reprezentująca agenta mikroserwisów.
 */
public class MicroserviceAgent {
    private static MicroserviceAgent instance;
    private Map<String, Integer> microservicePorts; // Mapa przechowująca nazwy mikroserwisów i ich numery portów

    private MicroserviceAgent() {
        microservicePorts = new HashMap<>();

        // Inicjalizacja numerów portów w konstruktorze
        microservicePorts.put("Rejestracja", 2556);
        microservicePorts.put("Logowanie", 2551);
        microservicePorts.put("Czat", 2577);
        microservicePorts.put("Table", 2585);
        microservicePorts.put("FileServer", 1444);
    }

    /**
     * Pobierz instancję agenta mikroserwisów.
     *
     * @return Instancja agenta mikroserwisów.
     */
    public static MicroserviceAgent getInstance() {
        if (instance == null) {
            instance = new MicroserviceAgent();
        }
        return instance;
    }

    /**
     * Pobierz numer portu dla danego mikroserwisu.
     *
     * @param serviceName Nazwa mikroserwisu.
     * @return Numer portu dla mikroserwisu.
     */
    public int getMicroservicePort(String serviceName) {
        return microservicePorts.getOrDefault(serviceName, -1); // -1 jako wartość domyślna, gdy mikroserwis nie istnieje
    }

    // Metoda zatrzymująca wszystkie mikroserwisy
    public void stopAllMicroservices() {
        microservicePorts.forEach((serviceName, port) -> {
            try {
                // Wysłanie sygnału zatrzymania do mikroserwisu
                Socket socket = new Socket("localhost", port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("type:stop");
                socket.close();
            } catch (IOException e) {
                System.err.println("Błąd podczas zatrzymywania mikroserwisu " + serviceName);
            }
        });
    }
}
