package com.facturapp.presentation.controller;

import com.facturapp.application.FacturaUseCase;
import com.facturapp.application.ClienteUseCase;
import com.facturapp.application.ProductoUseCase;
import com.facturapp.presentation.util.AlertUtil;
import com.facturapp.presentation.util.AppContext;
import com.facturapp.presentation.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controlador principal del Dashboard.
 * Gestiona la navegación entre secciones y muestra estadísticas globales.
 */
public class DashboardController implements Initializable {

    private static final Logger log = Logger.getLogger(DashboardController.class.getName());

    @FXML private BorderPane mainPane;
    @FXML private StackPane contentArea;
    @FXML private Label lblUsuario;
    @FXML private Label lblRol;

    // KPI Cards
    @FXML private Label lblTotalClientes;
    @FXML private Label lblTotalProductos;
    @FXML private Label lblTotalFacturas;
    @FXML private Label lblFacturacionMes;

    // Botones de navegación lateral
    @FXML private Button btnDashboard;
    @FXML private Button btnClientes;
    @FXML private Button btnProductos;
    @FXML private Button btnFacturas;
    @FXML private Button btnLogos;
    @FXML private Button btnMiEmpresa;
    @FXML private Button btnUsuarios;

    private AppContext ctx;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ctx = AppContext.getInstance();
        var usuario = SessionManager.getInstance().getUsuarioActual();

        if (usuario != null) {
            lblUsuario.setText(usuario.getUsername());
            lblRol.setText(usuario.getRol().getDescripcion());
        }

        // Ocultar sección usuarios si no es admin
        if (btnUsuarios != null) {
            btnUsuarios.setVisible(SessionManager.getInstance().esAdmin());
            btnUsuarios.setManaged(SessionManager.getInstance().esAdmin());
        }
        // Logos visible para todos los usuarios autenticados
        // (cualquiera puede gestionar logos de empresa)

        actualizarKPIs();
        mostrarInicioDashboard();
    }

    /** Actualiza los indicadores KPI del panel principal */
    private void actualizarKPIs() {
        try {
            lblTotalClientes.setText(String.valueOf(ctx.getClienteUseCase().totalClientes()));
            lblTotalProductos.setText(String.valueOf(ctx.getProductoUseCase().totalProductos()));
            lblTotalFacturas.setText(String.valueOf(ctx.getFacturaUseCase().totalFacturas()));

            // Facturación del mes actual
            YearMonth mes = YearMonth.now();
            BigDecimal totalMes = ctx.getFacturaUseCase().totalFacturadoEntre(
                mes.atDay(1), mes.atEndOfMonth()
            );
            lblFacturacionMes.setText(String.format("%,.2f €", totalMes));
        } catch (Exception e) {
            log.warning("Error actualizando KPIs: " + e.getMessage());
        }
    }

    // --- Acciones de navegación ---

    @FXML
    private void mostrarInicioDashboard() {
        cargarVista("/com/facturapp/fxml/inicio.fxml");
        resaltarBoton(btnDashboard);
    }

    @FXML
    private void mostrarClientes() {
        cargarVista("/com/facturapp/fxml/cliente.fxml");
        resaltarBoton(btnClientes);
    }

    @FXML
    private void mostrarProductos() {
        cargarVista("/com/facturapp/fxml/producto.fxml");
        resaltarBoton(btnProductos);
    }

    @FXML
    private void mostrarFacturas() {
        cargarVista("/com/facturapp/fxml/factura.fxml");
        resaltarBoton(btnFacturas);
    }

    @FXML
    private void mostrarLogos() {
        cargarVista("/com/facturapp/fxml/logo.fxml");
        resaltarBoton(btnLogos);
    }

    @FXML
    private void mostrarMiEmpresa() {
        cargarVista("/com/facturapp/fxml/empresa_config.fxml");
        resaltarBoton(btnMiEmpresa);
    }

    @FXML
    private void mostrarUsuarios() {
        if (!SessionManager.getInstance().esAdmin()) {
            AlertUtil.mostrarAdvertencia("Acceso denegado", "Solo los administradores pueden gestionar usuarios.");
            return;
        }
        cargarVista("/com/facturapp/fxml/usuario.fxml");
        resaltarBoton(btnUsuarios);
    }

    @FXML
    private void cerrarSesion() {
        boolean confirmar = AlertUtil.mostrarConfirmacion(
            "Cerrar sesión", "¿Deseas cerrar la sesión actual?"
        );
        if (confirmar) {
            SessionManager.getInstance().cerrarSesion();
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/facturapp/fxml/login.fxml")
                );
                Scene scene = new Scene(loader.load(), 1100, 700);
                scene.getStylesheets().add(
                    getClass().getResource("/com/facturapp/css/styles.css").toExternalForm()
                );
                Stage stage = (Stage) mainPane.getScene().getWindow();
                stage.setMaximized(false);
                stage.setScene(scene);
                stage.setTitle("FacturApp - Login");
                stage.centerOnScreen();
            } catch (Exception e) {
                AlertUtil.mostrarError("Error", "No se pudo regresar al login.");
            }
        }
    }

    /** Carga una vista FXML en el área de contenido central */
    private void cargarVista(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node vista = loader.load();
            contentArea.getChildren().setAll(vista);
            actualizarKPIs();
        } catch (Exception e) {
            log.severe("Error cargando vista " + fxmlPath + ": " + e.getMessage());
            AlertUtil.mostrarErrorDetallado("Error de navegación",
                "No se pudo cargar la sección solicitada.", e);
        }
    }

    /** Aplica la clase CSS 'activo' al botón seleccionado */
    private void resaltarBoton(Button botonActivo) {
        Button[] botones = {btnDashboard, btnClientes, btnProductos, btnFacturas, btnLogos, btnMiEmpresa, btnUsuarios};
        for (Button btn : botones) {
            if (btn != null) btn.getStyleClass().remove("nav-btn-activo");
        }
        if (botonActivo != null) botonActivo.getStyleClass().add("nav-btn-activo");
    }
}
