package com.facturapp.presentation.controller;

import com.facturapp.application.AuthUseCase;
import com.facturapp.domain.model.Usuario;
import com.facturapp.presentation.util.AlertUtil;
import com.facturapp.presentation.util.AppContext;
import com.facturapp.presentation.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controlador de la pantalla de Login + Registro.
 * Permite iniciar sesión con credenciales existentes
 * o crear una cuenta nueva directamente desde esta pantalla.
 */
public class LoginController implements Initializable {

    private static final Logger log = Logger.getLogger(LoginController.class.getName());

    // --- Panel Login ---
    @FXML private VBox panelLogin;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblError;

    // --- Panel Registro ---
    @FXML private VBox panelRegistro;
    @FXML private TextField txtRegUsername;
    @FXML private TextField txtRegEmail;
    @FXML private PasswordField txtRegPassword;
    @FXML private PasswordField txtRegPasswordConfirm;
    @FXML private Label lblRegError;

    private AuthUseCase authUseCase;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        authUseCase = AppContext.getInstance().getAuthUseCase();
        ocultarError(lblError);
        ocultarError(lblRegError);

        // Enter en usuario → pasar a contraseña (el botón con defaultButton="true" maneja Enter en password)
        txtUsername.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) txtPassword.requestFocus();
        });

        Platform.runLater(() -> txtUsername.requestFocus());
    }

    // =========================================================
    //  PANEL LOGIN
    // =========================================================

    @FXML
    private void onLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        ocultarError(lblError);

        if (username.isEmpty() || password.isEmpty()) {
            mostrarError(lblError, "Por favor, introduce usuario y contraseña.");
            return;
        }

        try {
            Optional<Usuario> resultado = authUseCase.login(username, password);
            if (resultado.isPresent()) {
                SessionManager.getInstance().iniciarSesion(resultado.get());
                log.info("Sesión iniciada: " + username);
                abrirDashboard();
            } else {
                mostrarError(lblError, "Usuario o contraseña incorrectos.");
                txtPassword.clear();
                txtPassword.requestFocus();
            }
        } catch (Exception e) {
            log.severe("Error en login: " + e.getMessage());
            mostrarError(lblError, "Error de conexión con la base de datos.");
        }
    }

    @FXML
    private void mostrarRegistro() {
        panelLogin.setVisible(false);
        panelLogin.setManaged(false);
        panelRegistro.setVisible(true);
        panelRegistro.setManaged(true);
        ocultarError(lblRegError);
        limpiarRegistro();
        Platform.runLater(() -> txtRegUsername.requestFocus());
    }

    // =========================================================
    //  PANEL REGISTRO
    // =========================================================

    @FXML
    private void onRegistrar() {
        String username  = txtRegUsername.getText().trim();
        String email     = txtRegEmail.getText().trim();
        String password  = txtRegPassword.getText();
        String password2 = txtRegPasswordConfirm.getText();

        ocultarError(lblRegError);

        if (username.isEmpty() || password.isEmpty()) {
            mostrarError(lblRegError, "Usuario y contraseña son obligatorios.");
            return;
        }
        if (password.length() < 6) {
            mostrarError(lblRegError, "La contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if (!password.equals(password2)) {
            mostrarError(lblRegError, "Las contraseñas no coinciden.");
            txtRegPasswordConfirm.clear();
            return;
        }

        try {
            // Primer usuario del sistema → ADMIN. Los siguientes → USUARIO
            long totalUsuarios = authUseCase.listarUsuarios().size();
            Usuario.Rol rol = (totalUsuarios == 0) ? Usuario.Rol.ADMIN : Usuario.Rol.USUARIO;

            Usuario nuevo = authUseCase.crearUsuario(
                username, password,
                email.isEmpty() ? null : email,
                rol
            );

            String mensaje = rol == Usuario.Rol.ADMIN
                ? "Cuenta de administrador creada. Ya puedes iniciar sesión."
                : "Cuenta creada como Usuario. Un administrador puede cambiar tu rol.";

            AlertUtil.mostrarExito("Cuenta creada", mensaje);
            log.info("Nuevo usuario registrado: " + nuevo.getUsername() + " (" + rol + ")");

            // Volver al login con el username relleno
            mostrarLogin();
            txtUsername.setText(username);
            txtPassword.requestFocus();

        } catch (IllegalArgumentException e) {
            mostrarError(lblRegError, e.getMessage());
        } catch (Exception e) {
            log.severe("Error en registro: " + e.getMessage());
            mostrarError(lblRegError, "Error al crear la cuenta. Inténtalo de nuevo.");
        }
    }

    @FXML
    private void mostrarLogin() {
        panelRegistro.setVisible(false);
        panelRegistro.setManaged(false);
        panelLogin.setVisible(true);
        panelLogin.setManaged(true);
        ocultarError(lblError);
        Platform.runLater(() -> txtUsername.requestFocus());
    }

    // =========================================================
    //  UTILS
    // =========================================================

    private void abrirDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/facturapp/fxml/dashboard.fxml")
            );
            Scene scene = new Scene(loader.load(), 1200, 750);
            scene.getStylesheets().add(
                getClass().getResource("/com/facturapp/css/styles.css").toExternalForm()
            );
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("FacturApp - Dashboard");
            stage.setMaximized(true);
        } catch (Exception e) {
            log.severe("Error cargando dashboard: " + e.getMessage());
            AlertUtil.mostrarErrorDetallado("Error", "No se pudo cargar el dashboard.", e);
        }
    }

    private void mostrarError(Label label, String mensaje) {
        label.setText(mensaje);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void ocultarError(Label label) {
        if (label != null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private void limpiarRegistro() {
        txtRegUsername.clear();
        txtRegEmail.clear();
        txtRegPassword.clear();
        txtRegPasswordConfirm.clear();
    }
}
