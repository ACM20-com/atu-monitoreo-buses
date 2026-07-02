import java.io.*;
import java.net.*;
import java.util.Random;

/**
 * ClienteBus - Simulador de Bus con GPS
 * Proyecto: Sistema de Monitoreo de Buses en Tiempo Real - ATU
 *
 * Simula un bus que envia su posicion GPS al servidor central
 * cada 2 segundos via socket TCP/IP.
 *
 * Uso: java ClienteBus <IP_SERVIDOR> <PUERTO> <BUS_ID> <CORREDOR>
 * Ejemplo: java ClienteBus 192.168.1.10 5000 BUS-R01 ROJO
 *
 * Corredores disponibles: ROJO, AZUL, AMARILLO
 */
public class ClienteBus {

    // Rutas reales de los corredores por Lima (coordenadas GPS)
    // Corredor Rojo: Av. Javier Prado / La Marina
    static final double[][] RUTA_ROJO = {
        {-12.0931, -77.0531}, {-12.0920, -77.0510}, {-12.0908, -77.0488},
        {-12.0895, -77.0462}, {-12.0880, -77.0435}, {-12.0865, -77.0405},
        {-12.0850, -77.0375}, {-12.0835, -77.0345}, {-12.0820, -77.0315},
        {-12.0805, -77.0285}, {-12.0795, -77.0260}, {-12.0780, -77.0235}
    };

    // Corredor Azul: Av. Universitaria / Brasil
    static final double[][] RUTA_AZUL = {
        {-11.9950, -77.0830}, {-11.9980, -77.0815}, {-12.0010, -77.0800},
        {-12.0040, -77.0785}, {-12.0070, -77.0770}, {-12.0100, -77.0755},
        {-12.0130, -77.0740}, {-12.0160, -77.0725}, {-12.0190, -77.0710},
        {-12.0220, -77.0695}, {-12.0250, -77.0680}, {-12.0280, -77.0665}
    };

    // Corredor Amarillo: Av. Tupac Amaru / Independencia
    static final double[][] RUTA_AMARILLO = {
        {-11.9700, -77.0580}, {-11.9730, -77.0590}, {-11.9760, -77.0600},
        {-11.9790, -77.0610}, {-11.9820, -77.0620}, {-11.9850, -77.0630},
        {-11.9880, -77.0640}, {-11.9910, -77.0650}, {-11.9940, -77.0660},
        {-11.9970, -77.0670}, {-12.0000, -77.0680}, {-12.0030, -77.0690}
    };

    public static void main(String[] args) {
        // Configuracion por defecto si no se pasan argumentos
        String ipServidor = args.length > 0 ? args[0] : "127.0.0.1";
        int puerto        = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        String idBus      = args.length > 2 ? args[2] : "BUS-R01";
        String corredor   = args.length > 3 ? args[3].toUpperCase() : "ROJO";

        double[][] ruta = switch (corredor) {
            case "AZUL"     -> RUTA_AZUL;
            case "AMARILLO" -> RUTA_AMARILLO;
            default         -> RUTA_ROJO; // ROJO por defecto
        };

        System.out.println("=================================================");
        System.out.println("  ATU - Bus Cliente: " + idBus);
        System.out.println("  Corredor: " + corredor);
        System.out.println("  Conectando a: " + ipServidor + ":" + puerto);
        System.out.println("=================================================");

        Random rnd = new Random();
        int parada = 0;

        while (true) {
            try (Socket socket = new Socket(ipServidor, puerto);
                 PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String bienvenida = entrada.readLine();
                System.out.println("[CONECTADO] " + bienvenida);

                while (true) {
                    double[] coords = ruta[parada % ruta.length];
                    // Agregar variacion pequena para simular movimiento real
                    double lat = coords[0] + (rnd.nextDouble() - 0.5) * 0.0005;
                    double lng = coords[1] + (rnd.nextDouble() - 0.5) * 0.0005;
                    int velocidad = 20 + rnd.nextInt(50); // 20-70 km/h

                    String mensaje = String.format("%s|%.6f|%.6f|%s|%d",
                        idBus, lat, lng, corredor, velocidad);

                    salida.println(mensaje);
                    System.out.println("[ENVIADO] " + mensaje);

                    String respuesta = entrada.readLine();
                    System.out.println("[RESPUESTA] " + respuesta);

                    parada++;
                    Thread.sleep(2000); // Envia posicion cada 2 segundos
                }

            } catch (ConnectException e) {
                System.out.println("[ERROR] No se pudo conectar al servidor. Reintentando en 5s...");
                try { Thread.sleep(5000); } catch (InterruptedException ie) { break; }
            } catch (IOException | InterruptedException e) {
                System.out.println("[ERROR] Conexion interrumpida: " + e.getMessage());
                try { Thread.sleep(3000); } catch (InterruptedException ie) { break; }
            }
        }
    }
}
