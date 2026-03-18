package com.facturapp.presentation.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import java.util.Optional;

/**
 * Utilidad para mostrar diálogos de alerta estilizados en JavaFX.
 */
public class AlertUtil {

    private AlertUtil() {}

    /** Muestra un diálogo de información */
    public static void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        aplicarEstilo(alert);
        alert.showAndWait();
    }

    /** Muestra un diálogo de éxito */
    public static void mostrarExito(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText("✓ " + titulo);
        alert.setContentText(mensaje);
        aplicarEstilo(alert);
        alert.showAndWait();
    }

    /** Muestra un diálogo de error */
    public static void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText("Error");
        alert.setContentText(mensaje);
        aplicarEstilo(alert);
        alert.showAndWait();
    }

    /** Muestra un diálogo de advertencia */
    public static void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        aplicarEstilo(alert);
        alert.showAndWait();
    }

    /**
     * Muestra un diálogo de confirmación.
     * @return true si el usuario pulsó Aceptar, false si canceló.
     */
    public static boolean mostrarConfirmacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        aplicarEstilo(alert);
        Optional<ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    /** Muestra un diálogo de error con detalle de excepción expandible */
    public static void mostrarErrorDetallado(String titulo, String mensaje, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(mensaje);
        alert.setContentText(e.getMessage());

        TextArea area = new TextArea(obtenerStackTrace(e));
        area.setEditable(false);
        area.setMaxWidth(Double.MAX_VALUE);
        area.setMaxHeight(Double.MAX_VALUE);
        alert.getDialogPane().setExpandableContent(area);

        aplicarEstilo(alert);
        alert.showAndWait();
    }

    /** Aplica el CSS personalizado al diálogo si está disponible */
    private static void aplicarEstilo(Alert alert) {
        try {
            var css = AlertUtil.class.getResource("/com/facturapp/css/styles.css");
            if (css != null) {
                alert.getDialogPane().getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {}
    }

    private static String obtenerStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n\n");
        for (var element : e.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
