import java.io.*;
import java.net.*;
import java.util.*;

public class BusCliente {

    // Rutas reales de los 3 corredores ATU por Lima (mismas coordenadas que el proyecto de monitoreo por corredor)
    static final Map<String, double[][]> RUTAS = new HashMap<>();
    static final Map<String, String> CORREDOR_DE_RUTA = new HashMap<>();
    static {
        // Corredor ROJO: Óv. La Perla (San Miguel) -> La Marina -> Sucre -> Salaverry -> Javier Prado -> La Molina
double[][] RUTA_ROJO = {
    {-12.073769, -77.100052}, {-12.074265, -77.100081}, {-12.077638, -77.093014},
    {-12.078421, -77.080660}, {-12.083679, -77.065786}, {-12.086697, -77.062950},
    {-12.094777, -77.050759}, {-12.086350, -76.989735}, {-12.085131, -76.984883},
    {-12.083745, -76.969911}, {-12.081168, -76.966919}, {-12.075670, -76.963638},
    {-12.074235, -76.962140}, {-12.071042, -76.955348}, {-12.068382, -76.946672},
    {-12.065008, -76.939858}, {-12.064065, -76.938986}, {-12.062220, -76.938148},
    {-12.050788, -76.937696}, {-12.045811, -76.935348}, {-12.044325, -76.933639},
    {-12.042201, -76.925416}, {-12.040783, -76.924051}, {-12.039198, -76.923930},
    {-12.033599, -76.925944}
};
double[][] RUTA_AZUL = {
    {-12.026048, -77.034019}, {-12.026276, -77.034228}, {-12.031945, -77.027879},
    {-12.048625, -77.039166}, {-12.068251, -77.036999}, {-12.098463, -77.032521},
    {-12.118367, -77.029149}, {-12.119317, -77.029256}, {-12.119492, -77.029020},
    {-12.119174, -77.028876}, {-12.119032, -77.029182}, {-12.119648, -77.029287},
    {-12.123396, -77.032135}, {-12.125428, -77.033113}, {-12.126871, -77.029530},
    {-12.131467, -77.030051}, {-12.135254, -77.025234}, {-12.134833, -77.023773},
    {-12.135071, -77.023218}, {-12.136064, -77.022819}, {-12.141157, -77.022243},
    {-12.141948, -77.017583}, {-12.143186, -77.016106}
};
double[][] RUTA_AMARILLO = {
    {-11.972788, -77.086861}, {-11.973935, -77.088317}, {-11.976015, -77.085490},
    {-11.977930, -77.077707}, {-11.984401, -77.078769}, {-11.991386, -77.083099},
    {-11.990628, -77.064521}, {-12.028685, -77.058447}, {-12.032580, -77.054853},
    {-12.039105, -77.045233}, {-12.039269, -77.038096}, {-12.043445, -77.029358},
    {-12.043533, -77.018328}, {-12.033930, -77.008568}, {-12.029270, -76.996204},
    {-12.032779, -76.991096}, {-12.042441, -76.986651}, {-12.052005, -76.976379},
    {-12.065101, -76.971430}, {-12.078578, -76.976320}, {-12.085415, -76.982300},
    {-12.127029, -76.977032}, {-12.150120, -76.983529}, {-12.156288, -76.983540},
    {-12.191949, -76.972811}, {-12.192512, -76.960848}, {-12.232430, -76.937262},
    {-12.234445, -76.934513}, {-12.229859, -76.929544}
};

        // Dos buses por corredor (ida y vuelta), como en las rutas reales de la ATU
        RUTAS.put("BUS-ROJO-01", RUTA_ROJO);       CORREDOR_DE_RUTA.put("BUS-ROJO-01", "ROJO");
        RUTAS.put("BUS-ROJO-02", RUTA_ROJO);       CORREDOR_DE_RUTA.put("BUS-ROJO-02", "ROJO");
        RUTAS.put("BUS-AZUL-01", RUTA_AZUL);       CORREDOR_DE_RUTA.put("BUS-AZUL-01", "AZUL");
        RUTAS.put("BUS-AZUL-02", RUTA_AZUL);       CORREDOR_DE_RUTA.put("BUS-AZUL-02", "AZUL");
        RUTAS.put("BUS-AMARILLO-01", RUTA_AMARILLO); CORREDOR_DE_RUTA.put("BUS-AMARILLO-01", "AMARILLO");
        RUTAS.put("BUS-AMARILLO-02", RUTA_AMARILLO); CORREDOR_DE_RUTA.put("BUS-AMARILLO-02", "AMARILLO");
    }

    public static void main(String[] args) throws Exception {
        String idBus = (args.length > 0) ? args[0] : "BUS-ROJO-01";
        String host  = (args.length > 1) ? args[1] : "localhost";
        int    port  = (args.length > 2) ? Integer.parseInt(args[2]) : 5050;

        System.out.println("[" + idBus + "] Conectando a " + host + ":" + port);

        double[][] ruta = RUTAS.getOrDefault(idBus, RUTAS.get("BUS-ROJO-01"));
        String corredor = CORREDOR_DE_RUTA.getOrDefault(idBus, "ROJO");
        int totalPuntos = ruta.length;

        // Los buses "-02" arrancan en el extremo opuesto de la ruta y avanzan al revés,
        // para simular que van en sentido contrario dentro del mismo corredor
        boolean esRetorno = idBus.endsWith("-02");
        int indicePunto  = esRetorno ? totalPuntos - 2 : 0;
        int direccion    = esRetorno ? -1 : 1; // 1 = avanzar, -1 = retroceder (ida y vuelta)

        // Posición inicial = primer punto de su recorrido
        double lat = esRetorno ? ruta[totalPuntos - 1][0] : ruta[0][0];
        double lon = esRetorno ? ruta[totalPuntos - 1][1] : ruta[0][1];

        try (Socket socket = new Socket(host, port);
             PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[" + idBus + "] Conectado. Siguiendo ruta con " + totalPuntos + " puntos...");

            while (true) {
                double destinoLat = ruta[indicePunto][0];
                double destinoLon = ruta[indicePunto][1];

                // Mover gradualmente hacia el siguiente punto de la ruta
                double dLat = destinoLat - lat;
                double dLon = destinoLon - lon;
                double distancia = Math.sqrt(dLat * dLat + dLon * dLon);

                double paso = 0.0015; // ~150 metros por tick
                if (distancia < paso) {
                    lat = destinoLat;
                    lon = destinoLon;
                    // Avanzar al siguiente punto
                    indicePunto += direccion;
                    if (indicePunto >= totalPuntos) {
                        indicePunto = totalPuntos - 2;
                        direccion = -1;
                    } else if (indicePunto < 0) {
                        indicePunto = 1;
                        direccion = 1;
                    }
                } else {
                    lat += (dLat / distancia) * paso;
                    lon += (dLon / distancia) * paso;
                }

                String mensaje = idBus + "|" + String.format(Locale.US, "%.6f", lat) + "|" + String.format(Locale.US, "%.6f", lon) + "|" + corredor;
                pw.println(mensaje);
                System.out.println("[" + idBus + "] Enviado: " + mensaje);

                String respuesta = br.readLine();
                System.out.println("[" + idBus + "] Respuesta: " + respuesta);

                Thread.sleep(3000);
            }
        } catch (ConnectException e) {
            System.err.println("[" + idBus + "] No se pudo conectar: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[" + idBus + "] Error: " + e.getMessage());
        }
    }
}
