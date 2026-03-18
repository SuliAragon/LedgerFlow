package com.facturapp.presentation.controller;

import com.facturapp.application.AuthUseCase;
import com.facturapp.domain.model.Usuario;
import com.facturapp.presentation.util.AlertUtil;
import com.facturapp.presentation.util.AppContext;
import com.facturapp.presentation.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador de la sección Gestión de Usuarios (solo ADMIN).
 */
public class UsuarioController implements Initializable {

    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colUsername;
    @FXML private TableColumn<Usuario, String> colEmail;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colEstado;
    @FXML private TableColumn<Usuario, String> colAcciones;

    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<Usuario.Rol> cmbRol;
    @FXML private Label lblFormTitulo;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private AuthUseCase authUseCase;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Solo administradores pueden acceder
        if (!SessionManager.getInstance().esAdmin()) {
            AlertUtil.mostrarAdvertencia("Acceso denegado", "Solo los administradores pueden ver esta sección.");
            return;
        }
        authUseCase = AppContext.getInstance().getAuthUseCase();
        configurarTabla();
        configurarCombos();
        cargarUsuarios();
        modoVista();
    }

    private void configurarTabla() {
        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getEmail() != null ? d.getValue().getEmail() : ""));
        colRol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRol().getDescripcion()));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().isActivo() ? "Activo" : "Inactivo"));

        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnToggle   = new Button("Activar/Desactivar");
            final Button btnEliminar = new Button("Eliminar");
            final HBox botones = new HBox(6, btnToggle, btnEliminar);

            {
                btnToggle.getStyleClass().add("btn-warning");
                btnEliminar.getStyleClass().add("btn-danger");
                btnToggle.setOnAction(e -> toggleUsuario(getTableView().getItems().get(getIndex())));
                btnEliminar.setOnAction(e -> eliminarUsuario(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : botones);
            }
        });

        tablaUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void configurarCombos() {
        cmbRol.setItems(FXCollections.observableArrayList(Usuario.Rol.values()));
        cmbRol.setValue(Usuario.Rol.USUARIO);
    }

    private void cargarUsuarios() {
        tablaUsuarios.setItems(FXCollections.observableArrayList(authUseCase.listarUsuarios()));
    }

    @FXML
    private void onNuevo() {
        limpiarFormulario();
        lblFormTitulo.setText("Nuevo Usuario");
        modoEdicion();
    }

    @FXML
    private void onGuardar() {
        String username = txtUsername.getText().trim();
        String email    = txtEmail.getText().trim();
        String password = txtPassword.getText();
        Usuario.Rol rol = cmbRol.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            AlertUtil.mostrarAdvertencia("Campos obligatorios", "Usuario y contraseña son obligatorios.");
            return;
        }

        try {
            authUseCase.crearUsuario(username, password,
                email.isEmpty() ? null : email, rol != null ? rol : Usuario.Rol.USUARIO);
            AlertUtil.mostrarExito("Usuario creado", "El usuario se creó correctamente.");
            cargarUsuarios();
            modoVista();
        } catch (IllegalArgumentException e) {
            AlertUtil.mostrarError("Error de validación", e.getMessage());
        } catch (Exception e) {
            AlertUtil.mostrarErrorDetallado("Error", "No se pudo crear el usuario.", e);
        }
    }

    @FXML
    private void onCancelar() {
        modoVista();
        limpiarFormulario();
    }

    private void toggleUsuario(Usuario u) {
        // No permitir desactivar al usuario actual
        var actual = SessionManager.getInstance().getUsuarioActual();
        if (actual != null && actual.getId().equals(u.getId())) {
            AlertUtil.mostrarAdvertencia("No permitido", "No puedes desactivar tu propia cuenta.");
            return;
        }
        try {
            authUseCase.toggleEstadoUsuario(u.getId());
            cargarUsuarios();
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", e.getMessage());
        }
    }

    private void eliminarUsuario(Usuario u) {
        var actual = SessionManager.getInstance().getUsuarioActual();
        if (actual != null && actual.getId().equals(u.getId())) {
            AlertUtil.mostrarAdvertencia("No permitido", "No puedes eliminar tu propia cuenta.");
            return;
        }
        boolean confirm = AlertUtil.mostrarConfirmacion("Eliminar usuario",
            "¿Eliminar al usuario \"" + u.getUsername() + "\"?");
        if (confirm) {
            try {
                authUseCase.eliminarUsuario(u.getId());
                cargarUsuarios();
            } catch (Exception e) {
                AlertUtil.mostrarError("Error", e.getMessage());
            }
        }
    }

    private void modoEdicion() {
        btnGuardar.setVisible(true);
        btnCancelar.setVisible(true);
        setFormEditable(true);
    }

    private void modoVista() {
        btnGuardar.setVisible(false);
        btnCancelar.setVisible(false);
        setFormEditable(false);
        limpiarFormulario();
    }

    private void setFormEditable(boolean e) {
        txtUsername.setEditable(e);
        txtEmail.setEditable(e);
        txtPassword.setEditable(e);
        cmbRol.setDisable(!e);
    }

    private void limpiarFormulario() {
        txtUsername.clear();
        txtEmail.clear();
        txtPassword.clear();
        cmbRol.setValue(Usuario.Rol.USUARIO);
    }
}
