package ApiGateway;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MicroserviceAgent {
    private static MicroserviceAgent instance;
    private Map<String, Integer> microservicePorts; // Mapa przechowująca nazwy mikroserwisów i ich numery portów
    private AtomicInteger portCounter; // Licznik portów do przydzielania

    private Map<String, Thread> microserviceThreads;

    private boolean allMicroservicesStopped;


    private MicroserviceAgent() {
        microservicePorts = new HashMap<>();
        portCounter = new AtomicInteger(3000); // Początkowy numer portu

        // Inicjalizacja numerów portów w konstruktorze
        registerMicroservice("Rejestracja");
        registerMicroservice("Logowanie");
        registerMicroservice("Czat");
        registerMicroservice("Table");
        registerMicroservice("FileServer");

        // Uruchomienie mikroserwisów
        startMicroservices();
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
     * Pobierz numer portu dla danego mikroserwisu lub zarejestruj nowy mikroserwis.
     *
     * @param serviceName Nazwa mikroserwisu.
     * @return Numer portu dla mikroserwisu.
     */
    public int getMicroservicePort(String serviceName) {
        return microservicePorts.getOrDefault(serviceName, -1); // -1 jako wartość domyślna, gdy mikroserwis nie istnieje
    }

    /**
     * Zarejestruj nowy mikroserwis i przydziel mu port dynamicznie.
     *
     * @param serviceName Nazwa mikroserwisu.
     */
    public void registerMicroservice(String serviceName) {
        int port = findAvailablePort();
        microservicePorts.put(serviceName, port);
    }

    private int findAvailablePort() {
        int initialPort = portCounter.getAndIncrement();
        int port = initialPort;

        while (isPortOccupied(port)) {
            port = portCounter.getAndIncrement();
            if (port == initialPort) {
                throw new RuntimeException("Nie można znaleźć dostępnego portu.");
            }
        }

        return port;
    }

    private boolean isPortOccupied(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return false; // Port jest dostępny
        } catch (IOException e) {
            return true; // Port jest zajęty
        }
    }

    private void startMicroservices() {
        microserviceThreads = new HashMap<>();

        microservicePorts.forEach((serviceName, port) -> {
            try {
                Thread microserviceThread = new Thread(() -> {
                    try {
                        // Wywołaj metodę main danego mikroserwisu
                        String className = serviceName + "." + serviceName;
                        Class<?> clazz = Class.forName(className);
                        clazz.getMethod("main", String[].class).invoke(null, (Object) null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                microserviceThreads.put(serviceName, microserviceThread);
                microserviceThread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    public void stopMicroservices() {
        microserviceThreads.forEach((serviceName, thread) -> {
            try {
                thread.interrupt(); // Przerwij wątek mikroserwisu
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        allMicroservicesStopped = true;
    }


    public synchronized boolean areAllMicroservicesStopped() {
        return allMicroservicesStopped;
    }

}
