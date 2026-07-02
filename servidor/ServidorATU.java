import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * ServidorATU - Servidor Central de Monitoreo
 * Proyecto: Sistema de Monitoreo de Buses en Tiempo Real - ATU
 * Curso: Principios de los Sistemas de Software Distribuido
 *
 * Escucha conexiones TCP/IP de los buses cliente
 * y registra sus posiciones en tiempo real.
 */
public class ServidorATU {

    static final int PUERTO = 5000;
    // Mapa compartido: idBus -> ultimo mensaje recibido
    static final ConcurrentHashMap<String, String> posicionesBuses = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("=================================================");
        System.out.println("  ATU - Sistema de Monitoreo de Buses");
        System.out.println("  Servidor Central - Puerto TCP: " + PUERTO);
        System.out.println("=================================================");

        ServerSocket serverSocket = new ServerSocket(PUERTO);
        System.out.println("[OK] Servidor escuchando en el puerto " + PUERTO);
        System.out.println("[INFO] Esperando conexiones de buses...\n");

        // Hilo aparte que imprime el resumen cada 5 segundos
        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    if (!posicionesBuses.isEmpty()) {
                        System.out.println("\n--- RESUMEN DE FLOTA (" + new Date() + ") ---");
                        posicionesBuses.forEach((id, datos) ->
                            System.out.println("  " + id + " -> " + datos));
                        System.out.println("-------------------------------------------\n");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        monitor.setDaemon(true);
        monitor.start();

        // Loop principal: acepta cada conexion en su propio hilo virtual
        while (true) {
            Socket clienteSocket = serverSocket.accept();
            Thread.ofVirtual().start(() -> manejarCliente(clienteSocket));
        }
    }

    static void manejarCliente(Socket socket) {
        String direccionCliente = socket.getInetAddress().getHostAddress();
        System.out.println("[CONEXION] Nuevo bus conectado desde: " + direccionCliente);

        try (
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)
        ) {
            salida.println("BIENVENIDO:SERVIDOR_ATU");

            String linea;
            while ((linea = entrada.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;

                // Formato esperado: "BUS_ID|LAT|LNG|CORREDOR|VELOCIDAD"
                // Ejemplo:         "BUS-R01|-12.0931|-77.0531|ROJO|45"
                String[] partes = linea.split("\\|");
                if (partes.length >= 5) {
                    String idBus      = partes[0];
                    String lat        = partes[1];
                    String lng        = partes[2];
                    String corredor   = partes[3];
                    String velocidad  = partes[4];
                    String timestamp  = new Date().toString();

                    String registro = "lat=" + lat + " lng=" + lng +
                                      " corredor=" + corredor +
                                      " vel=" + velocidad + "km/h" +
                                      " hora=" + timestamp;

                    posicionesBuses.put(idBus, registro);
                    System.out.println("[DATA] " + idBus + " | " + corredor +
                                       " | lat:" + lat + " lng:" + lng +
                                       " | " + velocidad + "km/h");

                    salida.println("ACK:" + idBus + ":OK");
                } else {
                    System.out.println("[WARN] Formato invalido de: " + direccionCliente + " -> " + linea);
                    salida.println("ERROR:FORMATO_INVALIDO");
                }
            }
        } catch (IOException e) {
            System.out.println("[DESCONEXION] Bus desde " + direccionCliente + " se desconecto.");
        }
    }
}

