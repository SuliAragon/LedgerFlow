package com.facturapp.presentation.controller;

import com.facturapp.application.ClienteUseCase;
import com.facturapp.domain.model.Cliente;
import com.facturapp.presentation.util.AlertUtil;
import com.facturapp.presentation.util.AppContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controlador CRUD de la sección Clientes.
 */
public class ClienteController implements Initializable {

    private static final Logger log = Logger.getLogger(ClienteController.class.getName());

    // Tabla
    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colNifCif;
    @FXML private TableColumn<Cliente, String> colEmail;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colDireccion;
    @FXML private TableColumn<Cliente, String> colAcciones;

    // Formulario
    @FXML private TextField txtNombre;
    @FXML private TextField txtNifCif;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtBuscar;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private Label lblFormTitulo;

    private ClienteUseCase clienteUseCase;
    private Cliente clienteEditando = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        clienteUseCase = AppContext.getInstance().getClienteUseCase();
        configurarTabla();
        cargarClientes();
        modoVista();
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colNifCif.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNifCif()));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getEmail() != null ? d.getValue().getEmail() : ""));
        colTelefono.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getTelefono() != null ? d.getValue().getTelefono() : ""));
        colDireccion.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getDireccion() != null ? d.getValue().getDireccion() : ""));

        // Columna de acciones con botones Editar/Eliminar
        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnEditar = new Button("Editar");
            final Button btnEliminar = new Button("Eliminar");
            final HBox botones = new HBox(6, btnEditar, btnEliminar);

            {
                btnEditar.getStyleClass().add("btn-secondary");
                btnEliminar.getStyleClass().addAll("btn-danger");
                btnEditar.setOnAction(e -> editarCliente(getTableView().getItems().get(getIndex())));
                btnEliminar.setOnAction(e -> eliminarCliente(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : botones);
            }
        });

        // Ajustar columnas
        tablaClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void cargarClientes() {
        List<Cliente> clientes = clienteUseCase.listarClientes();
        tablaClientes.setItems(FXCollections.observableArrayList(clientes));
    }

    @FXML
    private void onBuscar() {
        String texto = txtBuscar.getText().trim();
        List<Cliente> resultados = texto.isEmpty()
            ? clienteUseCase.listarClientes()
            : clienteUseCase.buscarPorNombre(texto);
        tablaClientes.setItems(FXCollections.observableArrayList(resultados));
    }

    @FXML
    private void onNuevo() {
        clienteEditando = null;
        limpiarFormulario();
        lblFormTitulo.setText("Nuevo Cliente");
        modoEdicion();
    }

    @FXML
    private void onGuardar() {
        String nombre   = txtNombre.getText().trim();
        String nifCif   = txtNifCif.getText().trim();
        String email    = txtEmail.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String dir      = txtDireccion.getText().trim();

        if (nombre.isEmpty() || nifCif.isEmpty()) {
            AlertUtil.mostrarAdvertencia("Campos obligatorios", "Nombre y NIF/CIF son obligatorios.");
            return;
        }

        try {
            if (clienteEditando == null) {
                clienteUseCase.crearCliente(nombre, nifCif,
                    email.isEmpty() ? null : email,
                    telefono.isEmpty() ? null : telefono,
                    dir.isEmpty() ? null : dir);
                AlertUtil.mostrarExito("Cliente creado", "El cliente se ha guardado correctamente.");
            } else {
                clienteUseCase.actualizarCliente(clienteEditando.getId(),
                    nombre, nifCif,
                    email.isEmpty() ? null : email,
                    telefono.isEmpty() ? null : telefono,
                    dir.isEmpty() ? null : dir);
                AlertUtil.mostrarExito("Cliente actualizado", "Los datos se han actualizado.");
            }
            cargarClientes();
            modoVista();

        } catch (IllegalArgumentException e) {
            AlertUtil.mostrarError("Error de validación", e.getMessage());
        } catch (Exception e) {
            log.severe("Error guardando cliente: " + e.getMessage());
            AlertUtil.mostrarErrorDetallado("Error", "No se pudo guardar el cliente.", e);
        }
    }

    @FXML
    private void onCancelar() {
        modoVista();
        limpiarFormulario();
    }

    private void editarCliente(Cliente cliente) {
        clienteEditando = cliente;
        txtNombre.setText(cliente.getNombre());
        txtNifCif.setText(cliente.getNifCif());
        txtEmail.setText(cliente.getEmail() != null ? cliente.getEmail() : "");
        txtTelefono.setText(cliente.getTelefono() != null ? cliente.getTelefono() : "");
        txtDireccion.setText(cliente.getDireccion() != null ? cliente.getDireccion() : "");
        lblFormTitulo.setText("Editar Cliente");
        modoEdicion();
    }

    private void eliminarCliente(Cliente cliente) {
        boolean confirm = AlertUtil.mostrarConfirmacion(
            "Eliminar cliente",
            "¿Estás seguro de eliminar al cliente " + cliente.getNombre() + "?\n" +
            "Esta acción no se puede deshacer."
        );
        if (confirm) {
            try {
                clienteUseCase.eliminarCliente(cliente.getId());
                cargarClientes();
                AlertUtil.mostrarExito("Eliminado", "Cliente eliminado correctamente.");
            } catch (Exception e) {
                AlertUtil.mostrarError("Error", "No se pudo eliminar: " + e.getMessage());
            }
        }
    }

    private void modoEdicion() {
        btnGuardar.setVisible(true);
        btnCancelar.setVisible(true);
        setFormularioEditable(true);
    }

    private void modoVista() {
        btnGuardar.setVisible(false);
        btnCancelar.setVisible(false);
        setFormularioEditable(false);
        limpiarFormulario();
        clienteEditando = null;
    }

    private void setFormularioEditable(boolean editable) {
        txtNombre.setDisable(!editable);
        txtNifCif.setDisable(!editable);
        txtEmail.setDisable(!editable);
        txtTelefono.setDisable(!editable);
        txtDireccion.setDisable(!editable);
    }

    private void limpiarFormulario() {
        txtNombre.clear(); txtNifCif.clear();
        txtEmail.clear(); txtTelefono.clear(); txtDireccion.clear();
    }
}
