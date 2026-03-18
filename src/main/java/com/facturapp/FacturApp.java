package com.facturapp;

import com.facturapp.infrastructure.config.DatabaseConfig;
import com.facturapp.presentation.util.AppContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Punto de entrada de la aplicación FacturApp.
 * Inicializa la base de datos y muestra la pantalla de login.
 */
public class FacturApp extends Application {

    private static final Logger log = Logger.getLogger(FacturApp.class.getName());

    @Override
    public void start(Stage stage) throws Exception {
        // Inicializar base de datos H2
        DatabaseConfig.initDatabase();
        DatabaseConfig.insertarDatosDemostracion();

        // Inicializar el contexto de la aplicación (inyección de dependencias manual)
        AppContext.getInstance();

        // Cargar la pantalla de login
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/facturapp/fxml/login.fxml")
        );
        Scene scene = new Scene(loader.load(), 1100, 700);

        // Aplicar stylesheet global
        String css = getClass().getResource("/com/facturapp/css/styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("FacturApp - Sistema de Facturación");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.centerOnScreen();
        stage.show();

        log.info("FacturApp iniciada correctamente.");
    }

    @Override
    public void stop() {
        // Cerrar conexión a la base de datos al salir
        DatabaseConfig.cerrarConexion();
        log.info("FacturApp cerrada.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
