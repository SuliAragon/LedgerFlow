package com.facturapp.presentation.controller;

import com.facturapp.application.ClienteUseCase;
import com.facturapp.application.EmpresaConfigUseCase;
import com.facturapp.application.PresupuestoUseCase;
import com.facturapp.application.ProductoUseCase;
import com.facturapp.domain.model.*;
import com.facturapp.presentation.util.AlertUtil;
import com.facturapp.presentation.util.AppContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PresupuestoController implements Initializable {

    // Panel listado
    @FXML private VBox panelLista;
    @FXML private TableView<Presupuesto> tablaPresupuestos;
    @FXML private TableColumn<Presupuesto, String> colNumero;
    @FXML private TableColumn<Presupuesto, String> colCliente;
    @FXML private TableColumn<Presupuesto, String> colFecha;
    @FXML private TableColumn<Presupuesto, String> colTotal;
    @FXML private TableColumn<Presupuesto, String> colEstado;
    @FXML private TableColumn<Presupuesto, String> colAcciones;

    // Panel nuevo/editar
    @FXML private VBox panelNuevo;
    @FXML private Label lblTituloForm;
    @FXML private TextField txtNumero;
    @FXML private DatePicker dpFechaEmision;
    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private ComboBox<EmpresaConfig> cmbEmpresa;
    @FXML private TextArea txtObservaciones;

    // Tabla líneas
    @FXML private TableView<LineaFactura> tablaLineas;
    @FXML private TableColumn<LineaFactura, String> colLinProducto;
    @FXML private TableColumn<LineaFactura, String> colLinCantidad;
    @FXML private TableColumn<LineaFactura, String> colLinPrecio;
    @FXML private TableColumn<LineaFactura, String> colLinDescuento;
    @FXML private TableColumn<LineaFactura, String> colLinTotal;
    @FXML private TableColumn<LineaFactura, String> colLinAccion;

    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtDescuento;
    @FXML private Label lblSubtotal;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;

    private PresupuestoUseCase presupuestoUseCase;
    private ClienteUseCase clienteUseCase;
    private ProductoUseCase productoUseCase;
    private EmpresaConfigUseCase empresaConfigUseCase;

    private final ObservableList<LineaFactura> lineasActuales = FXCollections.observableArrayList();
    private Presupuesto presupuestoEditando = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        presupuestoUseCase   = AppContext.getInstance().getPresupuestoUseCase();
        clienteUseCase       = AppContext.getInstance().getClienteUseCase();
        productoUseCase      = AppContext.getInstance().getProductoUseCase();
        empresaConfigUseCase = AppContext.getInstance().getEmpresaConfigUseCase();

        configurarTablaPresupuestos();
        configurarTablaLineas();
        cargarCombos();
        cargarPresupuestos();
        mostrarPanelLista();
    }

    // ── LISTADO ──────────────────────────────────────────────────────────────

    private void configurarTablaPresupuestos() {
        colNumero.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        colCliente.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCliente() != null ? d.getValue().getCliente().getNombre() : "-"));
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getFechaEmision() != null ? d.getValue().getFechaEmision().toString() : "-"));
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%,.2f €", d.getValue().getTotal())));

        // Estado como botón clicable
        colEstado.setCellFactory(col -> new TableCell<>() {
            final Button btnEstado = new Button();
            {
                btnEstado.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size())
                        ciclarEstado(getTableView().getItems().get(idx));
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Presupuesto p = getTableView().getItems().get(getIndex());
                    aplicarEstiloEstado(btnEstado, p.getEstado());
                    setGraphic(btnEstado);
                }
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnPdf    = new Button("PDF");
            final Button btnEditar = new Button("Editar");
            final Button btnElim   = new Button("Eliminar");
            final HBox botones = new HBox(5, btnPdf, btnEditar, btnElim);
            {
                btnPdf.getStyleClass().add("btn-primary");
                btnEditar.getStyleClass().add("btn-secondary");
                btnElim.getStyleClass().add("btn-danger");
                btnPdf.setOnAction(e -> generarPdf(getTableView().getItems().get(getIndex())));
                btnEditar.setOnAction(e -> editarPresupuesto(getTableView().getItems().get(getIndex())));
                btnElim.setOnAction(e -> eliminarPresupuesto(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()
                    ? null : botones);
            }
        });

        tablaPresupuestos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void aplicarEstiloEstado(Button btn, Presupuesto.Estado estado) {
        btn.getStyleClass().removeAll(
            "btn-estado-nopagada", "btn-estado-pendiente", "btn-estado-pagada", "btn-estado-anulada");
        switch (estado) {
            case BORRADOR  -> { btn.setText("Borrador");  btn.getStyleClass().add("btn-estado-nopagada"); }
            case ENVIADO   -> { btn.setText("Enviado");   btn.getStyleClass().add("btn-estado-pendiente"); }
            case ACEPTADO  -> { btn.setText("Aceptado");  btn.getStyleClass().add("btn-estado-pagada"); }
            case RECHAZADO -> { btn.setText("Rechazado"); btn.getStyleClass().add("btn-estado-anulada"); btn.setDisable(true); }
            case EXPIRADO  -> { btn.setText("Expirado");  btn.getStyleClass().add("btn-estado-anulada"); btn.setDisable(true); }
        }
    }

    private void ciclarEstado(Presupuesto p) {
        if (p.getEstado() == Presupuesto.Estado.RECHAZADO ||
            p.getEstado() == Presupuesto.Estado.EXPIRADO) return;
        Presupuesto.Estado siguiente = switch (p.getEstado()) {
            case BORRADOR -> Presupuesto.Estado.ENVIADO;
            case ENVIADO  -> Presupuesto.Estado.ACEPTADO;
            case ACEPTADO -> Presupuesto.Estado.BORRADOR;
            default       -> p.getEstado();
        };
        try {
            presupuestoUseCase.cambiarEstado(p.getId(), siguiente);
            cargarPresupuestos();
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", e.getMessage());
        }
    }

    private void cargarPresupuestos() {
        tablaPresupuestos.setItems(FXCollections.observableArrayList(presupuestoUseCase.listarPresupuestos()));
    }

    private void generarPdf(Presupuesto p) {
        try {
            Path ruta = presupuestoUseCase.generarPdf(p.getId());
            AlertUtil.mostrarExito("PDF Generado", "Presupuesto guardado en:\n" + ruta);
        } catch (Exception e) {
            AlertUtil.mostrarErrorDetallado("Error", "No se pudo generar el PDF.", e);
        }
    }

    private void eliminarPresupuesto(Presupuesto p) {
        boolean confirm = AlertUtil.mostrarConfirmacion("Eliminar presupuesto",
            "¿Eliminar el presupuesto " + p.getNumero() + "?");
        if (confirm) {
            try {
                presupuestoUseCase.eliminarPresupuesto(p.getId());
                cargarPresupuestos();
            } catch (Exception e) {
                AlertUtil.mostrarError("Error", e.getMessage());
            }
        }
    }

    // ── FORMULARIO ───────────────────────────────────────────────────────────

    private void configurarTablaLineas() {
        colLinProducto.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getProducto() != null ? d.getValue().getProducto().getNombre() : "-"));
        colLinCantidad.setCellValueFactory(d -> new SimpleStringProperty(
            String.valueOf(d.getValue().getCantidad())));
        colLinPrecio.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%.2f €", d.getValue().getPrecioUnitario())));
        colLinDescuento.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getDescuento() + "%"));
        colLinTotal.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%.2f €", d.getValue().getTotalLinea())));

        colLinAccion.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("✕");
            { btn.getStyleClass().add("btn-danger");
              btn.setOnAction(e -> { lineasActuales.remove(getIndex()); actualizarTotales(); }); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tablaLineas.setItems(lineasActuales);
        tablaLineas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void cargarCombos() {
        List<Cliente> clientes = clienteUseCase.listarClientes();
        cmbCliente.setItems(FXCollections.observableArrayList(clientes));
        cmbProducto.setItems(FXCollections.observableArrayList(productoUseCase.listarProductosActivos()));
        cmbEmpresa.setItems(FXCollections.observableArrayList(empresaConfigUseCase.listar()));
        cmbProducto.setOnAction(e -> { if (cmbProducto.getValue() != null) txtCantidad.setText("1"); });
    }

    @FXML
    private void onNuevoPresupuesto() {
        presupuestoEditando = null;
        lblTituloForm.setText("Datos del Presupuesto");
        txtNumero.clear();
        txtNumero.setDisable(true);
        dpFechaEmision.setValue(LocalDate.now());
        cmbCliente.setValue(null);
        cmbEmpresa.setValue(null);
        txtObservaciones.clear();
        lineasActuales.clear();
        txtCantidad.setText("1");
        txtDescuento.setText("0");
        actualizarTotales();
        mostrarPanelNuevo();
    }

    private void editarPresupuesto(Presupuesto p) {
        presupuestoEditando = p;
        lblTituloForm.setText("Editar Presupuesto: " + p.getNumero());
        txtNumero.setText(p.getNumero());
        txtNumero.setDisable(false);
        dpFechaEmision.setValue(p.getFechaEmision());
        cmbCliente.setValue(p.getCliente());
        if (p.getEmpresaId() != null) {
            cmbEmpresa.getItems().stream()
                .filter(e -> e.getId().equals(p.getEmpresaId()))
                .findFirst().ifPresent(cmbEmpresa::setValue);
        } else {
            cmbEmpresa.setValue(null);
        }
        txtObservaciones.setText(p.getObservaciones() != null ? p.getObservaciones() : "");
        lineasActuales.clear();
        lineasActuales.addAll(p.getLineas());
        actualizarTotales();
        mostrarPanelNuevo();
    }

    @FXML
    private void onAgregarLinea() {
        Producto producto = cmbProducto.getValue();
        if (producto == null) { AlertUtil.mostrarAdvertencia("Sin producto", "Selecciona un producto."); return; }
        int cantidad;
        BigDecimal descuento;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            AlertUtil.mostrarError("Cantidad inválida", "Introduce una cantidad entera positiva."); return;
        }
        try {
            String dto = txtDescuento.getText().trim().replace(",", ".");
            descuento = dto.isEmpty() ? BigDecimal.ZERO : new BigDecimal(dto);
        } catch (NumberFormatException e) {
            AlertUtil.mostrarError("Descuento inválido", "El descuento debe ser un número (0-100)."); return;
        }
        lineasActuales.add(new LineaFactura(producto, cantidad, producto.getPrecio(), descuento));
        actualizarTotales();
        cmbProducto.setValue(null);
        txtCantidad.setText("1");
        txtDescuento.setText("0");
    }

    @FXML
    private void onGuardar() {
        if (cmbCliente.getValue() == null) {
            AlertUtil.mostrarAdvertencia("Sin cliente", "Selecciona un cliente."); return;
        }
        if (lineasActuales.isEmpty()) {
            AlertUtil.mostrarAdvertencia("Sin líneas", "Añade al menos una línea."); return;
        }
        try {
            Long empresaId = cmbEmpresa.getValue() != null ? cmbEmpresa.getValue().getId() : null;
            if (presupuestoEditando == null) {
                LocalDate fecha = dpFechaEmision.getValue() != null ? dpFechaEmision.getValue() : LocalDate.now();
                List<LineaFactura> lineas = new ArrayList<>(lineasActuales);
                Presupuesto nuevo = presupuestoUseCase.crearPresupuesto(
                    cmbCliente.getValue().getId(), empresaId, fecha, lineas,
                    txtObservaciones.getText().trim());
                AlertUtil.mostrarExito("Presupuesto creado",
                    "Presupuesto " + nuevo.getNumero() + " creado.\nTotal: " +
                    String.format("%,.2f €", nuevo.getTotal()));
                boolean generarPdf = AlertUtil.mostrarConfirmacion("Generar PDF", "¿Deseas generar el PDF ahora?");
                if (generarPdf) generarPdf(nuevo);
            } else {
                String num = txtNumero.getText().trim();
                if (!num.isEmpty()) presupuestoEditando.setNumero(num);
                if (dpFechaEmision.getValue() != null) presupuestoEditando.setFechaEmision(dpFechaEmision.getValue());
                presupuestoEditando.setCliente(cmbCliente.getValue());
                presupuestoEditando.setEmpresaId(empresaId);
                presupuestoEditando.setObservaciones(txtObservaciones.getText().trim());
                presupuestoEditando.setLineas(new ArrayList<>(lineasActuales));
                Presupuesto actualizado = presupuestoUseCase.actualizarPresupuesto(presupuestoEditando);
                AlertUtil.mostrarExito("Presupuesto actualizado",
                    "Presupuesto " + actualizado.getNumero() + " actualizado.\nTotal: " +
                    String.format("%,.2f €", actualizado.getTotal()));
            }
            cargarPresupuestos();
            mostrarPanelLista();
        } catch (IllegalArgumentException e) {
            AlertUtil.mostrarError("Error de validación", e.getMessage());
        } catch (Exception e) {
            AlertUtil.mostrarErrorDetallado("Error", "No se pudo guardar el presupuesto.", e);
        }
    }

    @FXML
    private void onCancelar() {
        mostrarPanelLista();
    }

    private void actualizarTotales() {
        BigDecimal subtotal = lineasActuales.stream().map(LineaFactura::getBaseImponible).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal iva      = lineasActuales.stream().map(LineaFactura::getImporteIva).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblSubtotal.setText(String.format("%,.2f €", subtotal));
        lblIva.setText(String.format("%,.2f €", iva));
        lblTotal.setText(String.format("%,.2f €", subtotal.add(iva)));
    }

    private void mostrarPanelLista() {
        panelLista.setVisible(true);  panelLista.setManaged(true);
        panelNuevo.setVisible(false); panelNuevo.setManaged(false);
    }

    private void mostrarPanelNuevo() {
        panelLista.setVisible(false); panelLista.setManaged(false);
        panelNuevo.setVisible(true);  panelNuevo.setManaged(true);
    }
}
