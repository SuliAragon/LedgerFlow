package com.facturapp.presentation.controller;

import com.facturapp.application.ClienteUseCase;
import com.facturapp.application.EmpresaConfigUseCase;
import com.facturapp.application.FacturaUseCase;
import com.facturapp.application.ProductoUseCase;
import com.facturapp.domain.model.*;
import com.facturapp.domain.model.EmpresaConfig;
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

/**
 * Controlador de la sección Facturas: listado, creación, edición y generación PDF.
 */
public class FacturaController implements Initializable {

    // --- Panel listado ---
    @FXML private TableView<Factura> tablaFacturas;
    @FXML private TableColumn<Factura, String> colNumero;
    @FXML private TableColumn<Factura, String> colCliente;
    @FXML private TableColumn<Factura, String> colFecha;
    @FXML private TableColumn<Factura, String> colTotal;
    @FXML private TableColumn<Factura, String> colEstado;
    @FXML private TableColumn<Factura, String> colAcciones;

    // Filtros
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<Cliente> cmbClienteFiltro;
    @FXML private VBox panelLista;

    // --- Panel nueva/editar factura ---
    @FXML private VBox panelNuevaFactura;
    @FXML private Label lblTituloForm;
    @FXML private TextField txtNumero;
    @FXML private DatePicker dpFechaEmision;
    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private ComboBox<EmpresaConfig> cmbEmpresa;
    @FXML private TextArea txtObservaciones;

    // Tabla de líneas
    @FXML private TableView<LineaFactura> tablaLineas;
    @FXML private TableColumn<LineaFactura, String> colLinProducto;
    @FXML private TableColumn<LineaFactura, String> colLinCantidad;
    @FXML private TableColumn<LineaFactura, String> colLinPrecio;
    @FXML private TableColumn<LineaFactura, String> colLinDescuento;
    @FXML private TableColumn<LineaFactura, String> colLinTotal;
    @FXML private TableColumn<LineaFactura, String> colLinAccion;

    // Controles añadir línea
    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtDescuento;

    // Totales
    @FXML private Label lblSubtotal;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;

    private FacturaUseCase facturaUseCase;
    private ClienteUseCase clienteUseCase;
    private ProductoUseCase productoUseCase;
    private EmpresaConfigUseCase empresaConfigUseCase;

    private final ObservableList<LineaFactura> lineasActuales = FXCollections.observableArrayList();

    /** Factura que se está editando; null si es una nueva. */
    private Factura facturaEditando = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        facturaUseCase       = AppContext.getInstance().getFacturaUseCase();
        clienteUseCase       = AppContext.getInstance().getClienteUseCase();
        productoUseCase      = AppContext.getInstance().getProductoUseCase();
        empresaConfigUseCase = AppContext.getInstance().getEmpresaConfigUseCase();

        configurarTablaFacturas();
        configurarTablaLineas();
        cargarCombos();
        cargarFacturas();
        mostrarPanelLista();
    }

    // ============================================================
    //  PANEL LISTADO
    // ============================================================

    private void configurarTablaFacturas() {
        colNumero.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        colCliente.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCliente() != null ? d.getValue().getCliente().getNombre() : "-"));
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getFechaEmision() != null
                ? d.getValue().getFechaEmision().toString() : "-"));
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%,.2f €", d.getValue().getTotal())));

        // Columna Estado: botón clicable que cicla el estado
        colEstado.setCellFactory(col -> new TableCell<>() {
            final Button btnEstado = new Button();
            {
                btnEstado.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        ciclarEstado(getTableView().getItems().get(idx));
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Factura f = getTableView().getItems().get(getIndex());
                    btnEstado.setDisable(false);
                    aplicarEstiloEstado(btnEstado, f.getEstado());
                    setGraphic(btnEstado);
                }
            }
        });

        // Columna Acciones: PDF | Editar | Anular
        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnPdf    = new Button("PDF");
            final Button btnEditar = new Button("Editar");
            final Button btnAnular = new Button("Anular");
            final HBox botones = new HBox(5, btnPdf, btnEditar, btnAnular);

            {
                btnPdf.getStyleClass().add("btn-primary");
                btnEditar.getStyleClass().add("btn-secondary");
                btnAnular.getStyleClass().add("btn-danger");
                btnPdf.setOnAction(e -> generarPdf(getTableView().getItems().get(getIndex())));
                btnEditar.setOnAction(e -> editarFactura(getTableView().getItems().get(getIndex())));
                btnAnular.setOnAction(e -> anularFactura(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Factura f = getTableView().getItems().get(getIndex());
                    boolean anulada = f.getEstado() == Factura.Estado.ANULADA;
                    btnEditar.setDisable(anulada);
                    btnAnular.setDisable(anulada);
                    setGraphic(botones);
                }
            }
        });

        tablaFacturas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    /** Aplica el estilo y texto del botón según el estado de la factura. */
    private void aplicarEstiloEstado(Button btn, Factura.Estado estado) {
        btn.getStyleClass().removeAll(
            "btn-estado-nopagada", "btn-estado-pendiente",
            "btn-estado-pagada", "btn-estado-anulada");
        switch (estado) {
            case BORRADOR, EMITIDA -> {
                btn.setText("No Pagada");
                btn.getStyleClass().add("btn-estado-nopagada");
            }
            case PENDIENTE -> {
                btn.setText("Pendiente");
                btn.getStyleClass().add("btn-estado-pendiente");
            }
            case PAGADA -> {
                btn.setText("Pagada");
                btn.getStyleClass().add("btn-estado-pagada");
            }
            case ANULADA -> {
                btn.setText("Anulada");
                btn.getStyleClass().add("btn-estado-anulada");
                btn.setDisable(true);
            }
        }
    }

    /** Cicla el estado: No Pagada → Pendiente → Pagada → No Pagada */
    private void ciclarEstado(Factura factura) {
        if (factura.getEstado() == Factura.Estado.ANULADA) return;
        Factura.Estado siguiente = switch (factura.getEstado()) {
            case BORRADOR, EMITIDA -> Factura.Estado.PENDIENTE;
            case PENDIENTE         -> Factura.Estado.PAGADA;
            case PAGADA            -> Factura.Estado.EMITIDA;
            default                -> factura.getEstado();
        };
        try {
            facturaUseCase.cambiarEstado(factura.getId(), siguiente);
            cargarFacturas();
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", e.getMessage());
        }
    }

    private void cargarFacturas() {
        tablaFacturas.setItems(FXCollections.observableArrayList(facturaUseCase.listarFacturas()));
    }

    @FXML
    private void onFiltrar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        List<Factura> resultados;
        if (desde != null && hasta != null) {
            resultados = facturaUseCase.filtrarPorFecha(desde, hasta);
        } else {
            resultados = facturaUseCase.listarFacturas();
        }

        Cliente clienteSel = cmbClienteFiltro.getValue();
        if (clienteSel != null) {
            resultados = resultados.stream()
                .filter(f -> f.getCliente() != null &&
                             f.getCliente().getId().equals(clienteSel.getId()))
                .toList();
        }

        tablaFacturas.setItems(FXCollections.observableArrayList(resultados));
    }

    @FXML
    private void onLimpiarFiltros() {
        dpDesde.setValue(null);
        dpHasta.setValue(null);
        cmbClienteFiltro.setValue(null);
        cargarFacturas();
    }

    private void generarPdf(Factura factura) {
        try {
            Path ruta = facturaUseCase.generarPdf(factura.getId());
            AlertUtil.mostrarExito("PDF Generado",
                "La factura se guardó en:\n" + ruta.toString());
        } catch (Exception e) {
            AlertUtil.mostrarErrorDetallado("Error", "No se pudo generar el PDF.", e);
        }
    }

    private void anularFactura(Factura factura) {
        boolean confirm = AlertUtil.mostrarConfirmacion("Anular factura",
            "¿Deseas anular la factura " + factura.getNumero() + "?\nEsta acción no se puede deshacer.");
        if (confirm) {
            try {
                facturaUseCase.cambiarEstado(factura.getId(), Factura.Estado.ANULADA);
                cargarFacturas();
            } catch (Exception e) {
                AlertUtil.mostrarError("Error", e.getMessage());
            }
        }
    }

    // ============================================================
    //  PANEL NUEVA / EDITAR FACTURA
    // ============================================================

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
            final Button btnEliminar = new Button("✕");
            {
                btnEliminar.getStyleClass().add("btn-danger");
                btnEliminar.setOnAction(e -> eliminarLinea(getIndex()));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnEliminar);
            }
        });

        tablaLineas.setItems(lineasActuales);
        tablaLineas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void cargarCombos() {
        List<Cliente> clientes = clienteUseCase.listarClientes();
        cmbCliente.setItems(FXCollections.observableArrayList(clientes));
        cmbClienteFiltro.setItems(FXCollections.observableArrayList(clientes));

        List<Producto> productos = productoUseCase.listarProductosActivos();
        cmbProducto.setItems(FXCollections.observableArrayList(productos));

        cmbEmpresa.setItems(FXCollections.observableArrayList(empresaConfigUseCase.listar()));

        cmbProducto.setOnAction(e -> {
            Producto p = cmbProducto.getValue();
            if (p != null) txtCantidad.setText("1");
        });
    }

    @FXML
    private void onNuevaFactura() {
        facturaEditando = null;
        lblTituloForm.setText("Datos de la Factura");
        txtNumero.clear();
        txtNumero.setDisable(true);
        dpFechaEmision.setValue(LocalDate.now());
        lineasActuales.clear();
        cmbCliente.setValue(null);
        cmbEmpresa.setValue(null);
        txtObservaciones.clear();
        txtCantidad.setText("1");
        txtDescuento.setText("0");
        actualizarTotales();
        mostrarPanelNueva();
    }

    /** Carga una factura existente en el formulario para editarla. */
    private void editarFactura(Factura factura) {
        facturaEditando = factura;
        lblTituloForm.setText("Editar Factura: " + factura.getNumero());

        txtNumero.setText(factura.getNumero());
        txtNumero.setDisable(false);
        dpFechaEmision.setValue(factura.getFechaEmision());

        cmbCliente.setValue(factura.getCliente());

        if (factura.getEmpresaId() != null) {
            cmbEmpresa.getItems().stream()
                .filter(e -> e.getId().equals(factura.getEmpresaId()))
                .findFirst()
                .ifPresent(cmbEmpresa::setValue);
        } else {
            cmbEmpresa.setValue(null);
        }

        txtObservaciones.setText(factura.getObservaciones() != null ? factura.getObservaciones() : "");

        lineasActuales.clear();
        lineasActuales.addAll(factura.getLineas());
        actualizarTotales();

        mostrarPanelNueva();
    }

    @FXML
    private void onAgregarLinea() {
        Producto producto = cmbProducto.getValue();
        if (producto == null) {
            AlertUtil.mostrarAdvertencia("Sin producto", "Selecciona un producto.");
            return;
        }

        int cantidad;
        BigDecimal descuento;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            AlertUtil.mostrarError("Cantidad inválida", "Introduce una cantidad entera positiva.");
            return;
        }
        try {
            String dtoTxt = txtDescuento.getText().trim().replace(",", ".");
            descuento = dtoTxt.isEmpty() ? BigDecimal.ZERO : new BigDecimal(dtoTxt);
        } catch (NumberFormatException e) {
            AlertUtil.mostrarError("Descuento inválido", "El descuento debe ser un número (0-100).");
            return;
        }

        LineaFactura linea = new LineaFactura(producto, cantidad, producto.getPrecio(), descuento);
        lineasActuales.add(linea);
        actualizarTotales();
        cmbProducto.setValue(null);
        txtCantidad.setText("1");
        txtDescuento.setText("0");
    }

    private void eliminarLinea(int index) {
        if (index >= 0 && index < lineasActuales.size()) {
            lineasActuales.remove(index);
            actualizarTotales();
        }
    }

    @FXML
    private void onGuardarFactura() {
        if (cmbCliente.getValue() == null) {
            AlertUtil.mostrarAdvertencia("Sin cliente", "Selecciona un cliente.");
            return;
        }
        if (lineasActuales.isEmpty()) {
            AlertUtil.mostrarAdvertencia("Sin líneas", "Añade al menos una línea a la factura.");
            return;
        }

        try {
            Long empresaId = cmbEmpresa.getValue() != null ? cmbEmpresa.getValue().getId() : null;

            if (facturaEditando == null) {
                // Crear nueva factura
                LocalDate fecha = dpFechaEmision.getValue() != null ? dpFechaEmision.getValue() : LocalDate.now();
                List<LineaFactura> lineasNuevas = new ArrayList<>(lineasActuales);
                Factura nueva = facturaUseCase.crearFactura(
                    cmbCliente.getValue().getId(),
                    empresaId,
                    fecha,
                    lineasNuevas,
                    txtObservaciones.getText().trim()
                );
                AlertUtil.mostrarExito("Factura creada",
                    "Factura " + nueva.getNumero() + " creada correctamente.\n" +
                    "Total: " + String.format("%,.2f €", nueva.getTotal()));
                boolean generarPdf = AlertUtil.mostrarConfirmacion("Generar PDF",
                    "¿Deseas generar el PDF ahora?");
                if (generarPdf) generarPdf(nueva);
            } else {
                // Actualizar factura existente
                String numEditado = txtNumero.getText().trim();
                if (!numEditado.isEmpty()) facturaEditando.setNumero(numEditado);
                if (dpFechaEmision.getValue() != null) facturaEditando.setFechaEmision(dpFechaEmision.getValue());
                facturaEditando.setCliente(cmbCliente.getValue());
                facturaEditando.setEmpresaId(empresaId);
                facturaEditando.setObservaciones(txtObservaciones.getText().trim());
                facturaEditando.setLineas(new ArrayList<>(lineasActuales));

                Factura actualizada = facturaUseCase.actualizarFactura(facturaEditando);
                AlertUtil.mostrarExito("Factura actualizada",
                    "Factura " + actualizada.getNumero() + " actualizada correctamente.\n" +
                    "Total: " + String.format("%,.2f €", actualizada.getTotal()));
            }

            cargarFacturas();
            mostrarPanelLista();

        } catch (IllegalArgumentException e) {
            AlertUtil.mostrarError("Error de validación", e.getMessage());
        } catch (Exception e) {
            AlertUtil.mostrarErrorDetallado("Error", "No se pudo guardar la factura.", e);
        }
    }

    @FXML
    private void onCancelarFactura() {
        mostrarPanelLista();
    }

    private void actualizarTotales() {
        BigDecimal subtotal = lineasActuales.stream()
            .map(LineaFactura::getBaseImponible)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal iva = lineasActuales.stream()
            .map(LineaFactura::getImporteIva)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = subtotal.add(iva);

        lblSubtotal.setText(String.format("%,.2f €", subtotal));
        lblIva.setText(String.format("%,.2f €", iva));
        lblTotal.setText(String.format("%,.2f €", total));
    }

    private void mostrarPanelLista() {
        panelLista.setVisible(true);
        panelLista.setManaged(true);
        panelNuevaFactura.setVisible(false);
        panelNuevaFactura.setManaged(false);
    }

    private void mostrarPanelNueva() {
        panelLista.setVisible(false);
        panelLista.setManaged(false);
        panelNuevaFactura.setVisible(true);
        panelNuevaFactura.setManaged(true);
    }
}
