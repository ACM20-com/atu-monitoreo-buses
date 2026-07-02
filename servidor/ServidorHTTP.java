import java.io.*;
import java.net.*;
import java.util.*;

/**
 * ServidorHTTP - Puente HTTP para la Web
 * Expone las posiciones de los buses como JSON en el puerto 8080
 * para que el mapa web pueda consultarlas.
 *
 * Se ejecuta junto al ServidorATU (en segundo plano).
 * Uso: java ServidorHTTP
 */
public class ServidorHTTP {

    static final int PUERTO_HTTP = 8080;

    public static void main(String[] args) throws IOException {
        System.out.println("[HTTP] Servidor web iniciado en puerto " + PUERTO_HTTP);

        ServerSocket server = new ServerSocket(PUERTO_HTTP);

        while (true) {
            Socket cliente = server.accept();
            Thread.ofVirtual().start(() -> manejarPeticion(cliente));
        }
    }

    static void manejarPeticion(Socket socket) {
        try (
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream salida = socket.getOutputStream()
        ) {
            // Leer la primera linea del HTTP request
            String linea = entrada.readLine();
            if (linea == null) return;

            // Consumir el resto del header
            while (entrada.ready()) entrada.readLine();

            String json;
            String contentType;

            if (linea.contains("GET /buses")) {
                // Endpoint principal: devuelve posiciones de todos los buses
                json = buildBusesJson();
                contentType = "application/json";
            } else if (linea.contains("GET /")) {
                // Raiz: devuelve la pagina HTML del mapa
                json = buildIndexHTML();
                contentType = "text/html; charset=utf-8";
            } else {
                json = "{\"error\":\"ruta no encontrada\"}";
                contentType = "application/json";
            }

            byte[] cuerpo = json.getBytes("UTF-8");
            String header = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + contentType + "\r\n" +
                            "Content-Length: " + cuerpo.length + "\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Connection: close\r\n\r\n";

            salida.write(header.getBytes("UTF-8"));
            salida.write(cuerpo);
            salida.flush();

        } catch (IOException e) {
            // Ignorar errores de conexion cerrada
        }
    }

    static String buildBusesJson() {
        StringBuilder sb = new StringBuilder("[");
        boolean primero = true;

        for (Map.Entry<String, String> entry : ServidorATU.posicionesBuses.entrySet()) {
            if (!primero) sb.append(",");
            primero = false;

            String id = entry.getKey();
            String datos = entry.getValue();

            // Parsear "lat=X lng=Y corredor=Z vel=Wkm/h hora=..."
            String lat = extraer(datos, "lat=", " ");
            String lng = extraer(datos, "lng=", " ");
            String corredor = extraer(datos, "corredor=", " ");
            String vel = extraer(datos, "vel=", " ");

            sb.append(String.format(
                "{\"id\":\"%s\",\"lat\":%s,\"lng\":%s,\"corredor\":\"%s\",\"velocidad\":\"%s\"}",
                id, lat, lng, corredor, vel
            ));
        }

        sb.append("]");
        return sb.toString();
    }

    static String extraer(String texto, String inicio, String fin) {
        int i = texto.indexOf(inicio);
        if (i == -1) return "0";
        i += inicio.length();
        int j = texto.indexOf(fin, i);
        return j == -1 ? texto.substring(i) : texto.substring(i, j);
    }

    static String buildIndexHTML() {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
               "<title>ATU Monitor</title></head><body>" +
               "<h2>ATU - Monitor de Buses</h2>" +
               "<p>Abre <strong>mapa.html</strong> en tu navegador para ver el mapa interactivo.</p>" +
               "<p>O consulta el endpoint de datos: <a href='/buses'>/buses</a></p>" +
               "</body></html>";
    }
}
