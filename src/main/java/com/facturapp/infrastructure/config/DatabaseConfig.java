package com.facturapp.infrastructure.config;

import com.facturapp.application.AuthUseCase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Configuración y ciclo de vida de la base de datos H2 embebida.
 * Inicializa el esquema y carga datos de demostración al arrancar.
 */
public class DatabaseConfig {

    private static final Logger log = Logger.getLogger(DatabaseConfig.class.getName());

    // Base de datos en el directorio home del usuario, auto-servidor para múltiples conexiones
    private static final String DB_URL =
        "jdbc:h2:" + System.getProperty("user.home") + "/facturapp/facturapp_db;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static Connection sharedConnection;

    /** Obtiene la conexión compartida (singleton) */
    public static synchronized Connection getConnection() throws SQLException {
        if (sharedConnection == null || sharedConnection.isClosed()) {
            sharedConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            sharedConnection.setAutoCommit(true);
            log.info("Conexión H2 establecida en: " + DB_URL);
        }
        return sharedConnection;
    }

    /** Inicializa el esquema de base de datos leyendo schema.sql */
    public static void initDatabase() {
        log.info("Inicializando base de datos H2...");
        try {
            Connection conn = getConnection();
            String sql = leerScript("/database/schema.sql");
            try (Statement stmt = conn.createStatement()) {
                // Ejecutar cada sentencia separada por ';'
                for (String sentencia : sql.split(";")) {
                    String sentenciaLimpia = sentencia.trim();
                    if (!sentenciaLimpia.isEmpty()) {
                        stmt.execute(sentenciaLimpia);
                    }
                }
            }
            log.info("Esquema de base de datos creado/verificado correctamente.");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error inicializando la base de datos", e);
            throw new RuntimeException("No se pudo inicializar la base de datos.", e);
        }
    }

    /** Inserta datos de demostración si la base de datos está vacía */
    public static void insertarDatosDemostracion() {
        try {
            Connection conn = getConnection();
            // Solo insertar si no hay usuarios
            var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM usuarios");
            rs.next();
            if (rs.getInt(1) == 0) {
                log.info("Insertando datos de demostración...");
                String sql = leerScript("/database/demo_data.sql");
                try (Statement stmt = conn.createStatement()) {
                    for (String sentencia : sql.split(";")) {
                        String s = sentencia.trim();
                        if (!s.isEmpty()) {
                            stmt.execute(s);
                        }
                    }
                }
                log.info("Datos de demostración insertados.");
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Error insertando datos de demostración (no crítico)", e);
        }
    }

    /** Lee un script SQL del classpath */
    private static String leerScript(String ruta) throws Exception {
        try (var is = DatabaseConfig.class.getResourceAsStream(ruta)) {
            Objects.requireNonNull(is, "Script SQL no encontrado: " + ruta);
            try (var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines()
                    .filter(linea -> !linea.trim().startsWith("--")) // ignorar comentarios
                    .collect(Collectors.joining("\n"));
            }
        }
    }

    /** Cierra la conexión compartida al finalizar la aplicación */
    public static void cerrarConexion() {
        if (sharedConnection != null) {
            try {
                sharedConnection.close();
                log.info("Conexión H2 cerrada.");
            } catch (SQLException e) {
                log.warning("Error cerrando conexión H2: " + e.getMessage());
            }
        }
    }
}
