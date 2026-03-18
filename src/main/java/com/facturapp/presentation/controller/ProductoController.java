package com.facturapp.presentation.controller;

import com.facturapp.application.ProductoUseCase;
import com.facturapp.domain.model.Producto;
import com.facturapp.presentation.util.AlertUtil;
import com.facturapp.presentation.util.AppContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador CRUD de la sección Productos/Servicios.
 */
public class ProductoController implements Initializable {

    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colDescripcion;
    @FXML private TableColumn<Producto, String> colPrecio;
    @FXML private TableColumn<Producto, String> colIva;
    @FXML private TableColumn<Producto, String> colEstado;
    @FXML private TableColumn<Producto, String> colAcciones;

    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtPrecio;
    @FXML private ComboBox<Integer> cmbIva;
    @FXML private TextField txtBuscar;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private Label lblFormTitulo;

    private ProductoUseCase productoUseCase;
    private Producto productoEditando = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        productoUseCase = AppContext.getInstance().getProductoUseCase();
        configurarComboIva();
        configurarTabla();
        cargarProductos();
        modoVista();
    }

    private void configurarComboIva() {
        cmbIva.setItems(FXCollections.observableArrayList(0, 4, 10, 21));
        cmbIva.setValue(21);
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colDescripcion.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getDescripcion() != null ? d.getValue().getDescripcion() : ""));
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%.2f €", d.getValue().getPrecio())));
        colIva.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIvaTexto()));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().isActivo() ? "Activo" : "Inactivo"));

        // Colorear estado activo/inactivo
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (!empty && item != null) {
                    getStyleClass().removeAll("badge-success", "badge-danger");
                    getStyleClass().add("Activo".equals(item) ? "badge-success" : "badge-danger");
                }
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnEditar   = new Button("Editar");
            final Button btnToggle   = new Button("Toggle");
            final Button btnEliminar = new Button("Eliminar");
            final HBox botones = new HBox(6, btnEditar, btnToggle, btnEliminar);

            {
                btnEditar.getStyleClass().add("btn-secondary");
                btnToggle.getStyleClass().add("btn-warning");
                btnEliminar.getStyleClass().add("btn-danger");
                btnEditar.setOnAction(e -> editarProducto(getTableView().getItems().get(getIndex())));
                btnToggle.setOnAction(e -> toggleProducto(getTableView().getItems().get(getIndex())));
                btnEliminar.setOnAction(e -> eliminarProducto(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : botones);
            }
        });

        tablaProductos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void cargarProductos() {
        tablaProductos.setItems(FXCollections.observableArrayList(productoUseCase.listarProductos()));
    }

    @FXML
    private void onBuscar() {
        String texto = txtBuscar.getText().trim();
        List<Producto> resultados = texto.isEmpty()
            ? productoUseCase.listarProductos()
            : productoUseCase.buscarPorNombre(texto);
        tablaProductos.setItems(FXCollections.observableArrayList(resultados));
    }

    @FXML
    private void onNuevo() {
        productoEditando = null;
        limpiarFormulario();
        lblFormTitulo.setText("Nuevo Producto");
        modoEdicion();
    }

    @FXML
    private void onGuardar() {
        String nombre = txtNombre.getText().trim();
        String desc   = txtDescripcion.getText().trim();
        String precioTxt = txtPrecio.getText().trim().replace(",", ".");

        if (nombre.isEmpty() || precioTxt.isEmpty()) {
            AlertUtil.mostrarAdvertencia("Campos obligatorios", "Nombre y precio son obligatorios.");
            return;
        }

        BigDecimal precio;
        try {
            precio = new BigDecimal(precioTxt);
            if (precio.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            AlertUtil.mostrarError("Precio inválido", "Introduce un precio numérico positivo (ej: 29.99)");
            return;
        }

        int iva = cmbIva.getValue() != null ? cmbIva.getValue() : 21;

        try {
            if (productoEditando == null) {
                productoUseCase.crearProducto(nombre, desc.isEmpty() ? null : desc, precio, iva);
                AlertUtil.mostrarExito("Producto creado", "El producto se guardó correctamente.");
            } else {
                productoUseCase.actualizarProducto(productoEditando.getId(),
                    nombre, desc.isEmpty() ? null : desc, precio, iva);
                AlertUtil.mostrarExito("Producto actualizado", "Los datos se actualizaron.");
            }
            cargarProductos();
            modoVista();

        } catch (IllegalArgumentException e) {
            AlertUtil.mostrarError("Error de validación", e.getMessage());
        } catch (Exception e) {
            AlertUtil.mostrarErrorDetallado("Error", "No se pudo guardar el producto.", e);
        }
    }

    @FXML
    private void onCancelar() {
        modoVista();
        limpiarFormulario();
    }

    private void editarProducto(Producto p) {
        productoEditando = p;
        txtNombre.setText(p.getNombre());
        txtDescripcion.setText(p.getDescripcion() != null ? p.getDescripcion() : "");
        txtPrecio.setText(p.getPrecio().toPlainString());
        cmbIva.setValue(p.getPorcentajeIva());
        lblFormTitulo.setText("Editar Producto");
        modoEdicion();
    }

    private void toggleProducto(Producto p) {
        try {
            productoUseCase.toggleActivoProducto(p.getId());
            cargarProductos();
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", e.getMessage());
        }
    }

    private void eliminarProducto(Producto p) {
        boolean confirm = AlertUtil.mostrarConfirmacion(
            "Eliminar producto",
            "¿Eliminar el producto \"" + p.getNombre() + "\"?\nSi está en facturas, se desactivará en su lugar."
        );
        if (confirm) {
            try {
                productoUseCase.eliminarProducto(p.getId());
                cargarProductos();
                AlertUtil.mostrarExito("Eliminado", "Producto eliminado correctamente.");
            } catch (RuntimeException e) {
                if ("DEACTIVATED".equals(e.getMessage())) {
                    cargarProductos();
                    AlertUtil.mostrarInfo("Producto desactivado",
                        "\"" + p.getNombre() + "\" está en facturas existentes y no puede eliminarse.\n" +
                        "Se ha desactivado para que no aparezca en nuevas facturas.");
                } else {
                    AlertUtil.mostrarError("Error", "No se pudo eliminar: " + e.getMessage());
                }
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
        productoEditando = null;
    }

    private void setFormularioEditable(boolean editable) {
        txtNombre.setDisable(!editable);
        txtDescripcion.setDisable(!editable);
        txtPrecio.setDisable(!editable);
        cmbIva.setDisable(!editable);
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtDescripcion.clear();
        txtPrecio.clear();
        cmbIva.setValue(21);
    }
}
