# 🚌 Sistema de Monitoreo de Buses ATU en Tiempo Real

Sistema distribuido de monitoreo de buses desarrollado como Proyecto Integrador del
curso **Principios de los Sistemas de Software Distribuido**, para la Autoridad de
Transporte Urbano de Lima y Callao (ATU). Simula el envío de la ubicación GPS de
6 buses (2 por cada uno de los corredores Rojo, Azul y Amarillo) hacia un servidor
central mediante sockets TCP/IP, con persistencia en MariaDB y visualización en un
mapa interactivo en tiempo real.

## 🏗️ Arquitectura
[BusCliente x6] --TCP:5050--> [ServidorATU] --JDBC--> [MariaDB]
                                     |
                                HTTP:8080 (API REST /buses)
                                     |
                              [dashboard.html / dashboard2.html]

## 📂 Estructura del repositorio

### Código fuente

| Archivo | Descripción detallada |
|---|---|
| **`ServidorATU.java`** | Componente central del sistema. Levanta dos servicios en paralelo: (1) un servidor de **sockets TCP** en el puerto 5050 que recibe tramas de texto con formato `ID_BUS\|latitud\|longitud\|corredor`, las valida y las guarda en la base de datos MariaDB mediante JDBC; y (2) un **servidor HTTP** embebido en el puerto 8080 que expone tres endpoints: `/buses` (última posición conocida de cada bus, en JSON), `/stats` (métricas del servidor: mensajes recibidos, uptime, buses activos) y `/health` (chequeo de disponibilidad). Soporta múltiples buses conectados simultáneamente gracias a un pool de hilos (`ExecutorService`). |
| **`ServidorATU.class`** | Bytecode de `ServidorATU.java` ya compilado con `javac`, listo para ejecutarse directamente con `java -cp .:mysql-connector-j.jar ServidorATU`. |
| **`BusCliente.java`** | Simula el dispositivo GPS de un bus real. Cada instancia recibe un identificador (por ejemplo `BUS-ROJO-01`), se conecta por socket TCP al servidor y recorre gradualmente una ruta predefinida (coordenadas reales de los corredores de la ATU en Lima), enviando su posición cada 3 segundos y esperando la confirmación `OK` del servidor. Los buses terminados en `-02` recorren la misma ruta en sentido contrario, simulando ida y vuelta. |
| **`BusCliente.class`** | Bytecode compilado de `BusCliente.java`. |
| **`dashboard.html`** | Interfaz web principal del sistema. Usa la librería **Leaflet.js** sobre mapas de OpenStreetMap para mostrar, en tiempo real, la posición de cada bus sobre un mapa de Lima/Callao. Dibuja el trazado de los 3 corredores como líneas de colores, muestra una estela con los últimos 20 puntos recorridos por cada bus y actualiza los datos automáticamente cada 3 segundos consultando la API `/buses` del servidor. |
| **`dashboard2.html`** | Panel de control técnico complementario. Muestra métricas en tiempo real (buses activos, mensajes TCP recibidos, tiempo de actividad del servidor), un diagrama del estado de la arquitectura distribuida, una tabla detallada por bus y un log de eventos del sistema — útil para diagnóstico y demostración técnica ante el docente. |
| **`iniciar_atu.sh`** | Script de shell que automatiza el arranque de todos los servicios del sistema (base de datos, servidor, dashboard y los 6 buses) en el orden correcto. |
| **`detener_atu.sh`** | Script de shell que detiene de forma ordenada todos los procesos del sistema (buses, servidor y dashboard). |
| **`.gitignore`** | Excluye del control de versiones los archivos generados automáticamente (`.class`, carpeta `logs/`) que no deben versionarse. |

### Documentación

| Archivo | Descripción detallada |
|---|---|
| **`INFORME_ATU.md` / `INFORME_ATU.docx`** | Informe técnico del proyecto integrador: objetivo, arquitectura del sistema, pasos de instalación con comandos y resultados esperados, pruebas de funcionamiento realizadas y conclusiones, siguiendo el formato exigido por la universidad. |
| **`EXPLICACION_CODIGO.md` / `EXPLICACION_CODIGO.docx`** | Explicación pedagógica, línea por línea, del funcionamiento del código fuente (`ServidorATU.java` y `BusCliente.java`), pensada para lectores sin experiencia previa en programación de sockets en Java. |

### Carpetas

| Carpeta | Descripción detallada |
|---|---|
| **`leaflet/`** | Copia local de la librería JavaScript **Leaflet.js** (versión 1.9.x) y sus recursos (CSS, íconos de marcador), usada por `dashboard.html` para renderizar el mapa interactivo sin depender de una CDN externa. |
| **`logs/`** | Archivos `.log` generados durante la ejecución del servidor y de los clientes bus, usados como evidencia de las pruebas de comunicación TCP y persistencia en base de datos. |
| **`servidor/`** | Versión inicial del proyecto correspondiente al primer commit del equipo. |

## ⚙️ Requisitos previos

- Java Development Kit 17 o superior (probado con OpenJDK 21)
- MariaDB (o MySQL) en ejecución
- Python 3 (para servir los archivos estáticos del dashboard)

## 🚀 Cómo ejecutar el proyecto

La guía paso a paso completa —incluyendo la creación de la base de datos, compilación,
comandos exactos y solución de problemas comunes— está disponible en `INFORME_ATU.docx`,
sección **"Manual de instalación y ejecución"**.

Resumen rápido:

```bash
# 1. Crear base de datos (ver detalle completo en el informe)
sudo mariadb -u root -p -e "CREATE DATABASE atu_monitoreo CHARACTER SET utf8mb4; ..."

# 2. Compilar
javac -cp .:mysql-connector-j.jar ServidorATU.java
javac -cp .:mysql-connector-j.jar BusCliente.java

# 3. Levantar el servidor
java -cp .:mysql-connector-j.jar ServidorATU &

# 4. Levantar el dashboard
python3 -m http.server 7070 --bind 0.0.0.0 &

# 5. Levantar los 6 buses simulados
java -cp .:mysql-connector-j.jar BusCliente BUS-ROJO-01 localhost 5050 &
java -cp .:mysql-connector-j.jar BusCliente BUS-ROJO-02 localhost 5050 &
java -cp .:mysql-connector-j.jar BusCliente BUS-AZUL-01 localhost 5050 &
java -cp .:mysql-connector-j.jar BusCliente BUS-AZUL-02 localhost 5050 &
java -cp .:mysql-connector-j.jar BusCliente BUS-AMARILLO-01 localhost 5050 &
java -cp .:mysql-connector-j.jar BusCliente BUS-AMARILLO-02 localhost 5050 &
```

Luego abrir en el navegador: `http://<IP_DEL_SERVIDOR>:7070/dashboard.html`

## 👥 Integrantes del equipo
- Rojas Bautista Jimy Juniors — Líder del proyecto
- Hinostroza Naccha, Misael Marcell
- Ortega Rivas Alex Marco
- Paredes Marquez Jorge Jesús
- Cabeza Molina Alejandra Constanza

## 📚 Curso
Principios de los Sistemas de Software Distribuido — Docente: Ronald Miguel Serrano Hernandez
