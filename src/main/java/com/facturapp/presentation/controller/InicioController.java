package com.facturapp.presentation.controller;

import com.facturapp.application.FacturaUseCase;
import com.facturapp.domain.model.Factura;
import com.facturapp.presentation.util.AppContext;
import com.facturapp.presentation.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla de inicio del dashboard.
 * Muestra estadísticas, últimas facturas y top clientes.
 */
public class InicioController implements Initializable {

    @FXML private Label lblBienvenida;
    @FXML private Label lblTotalClientes;
    @FXML private Label lblTotalProductos;
    @FXML private Label lblTotalFacturas;
    @FXML private Label lblFacturacionMes;
    @FXML private Label lblFacturacionAnio;

    // Últimas facturas
    @FXML private TableView<Factura> tablaUltimas;
    @FXML private TableColumn<Factura, String> colUltNumero;
    @FXML private TableColumn<Factura, String> colUltCliente;
    @FXML private TableColumn<Factura, String> colUltFecha;
    @FXML private TableColumn<Factura, String> colUltTotal;
    @FXML private TableColumn<Factura, String> colUltEstado;

    // Top clientes
    @FXML private TableView<Object[]> tablaTopClientes;
    @FXML private TableColumn<Object[], String> colTopNombre;
    @FXML private TableColumn<Object[], String> colTopTotal;

    private FacturaUseCase facturaUseCase;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        facturaUseCase = AppContext.getInstance().getFacturaUseCase();

        var usuario = SessionManager.getInstance().getUsuarioActual();
        if (usuario != null && lblBienvenida != null) {
            lblBienvenida.setText("Bienvenido, " + usuario.getUsername() + " 👋");
        }

        cargarEstadisticas();
        configurarTablaUltimas();
        configurarTablaTopClientes();
        cargarUltimasFacturas();
        cargarTopClientes();
    }

    private void cargarEstadisticas() {
        try {
            long totalClientes  = AppContext.getInstance().getClienteUseCase().totalClientes();
            long totalProductos = AppContext.getInstance().getProductoUseCase().totalProductos();
            long totalFacturas  = facturaUseCase.totalFacturas();

            YearMonth mes = YearMonth.now();
            BigDecimal factMes = facturaUseCase.totalFacturadoEntre(
                mes.atDay(1), mes.atEndOfMonth());
            BigDecimal factAnio = facturaUseCase.totalFacturadoEntre(
                LocalDate.of(LocalDate.now().getYear(), 1, 1), LocalDate.now());

            if (lblTotalClientes  != null) lblTotalClientes.setText(String.valueOf(totalClientes));
            if (lblTotalProductos != null) lblTotalProductos.setText(String.valueOf(totalProductos));
            if (lblTotalFacturas  != null) lblTotalFacturas.setText(String.valueOf(totalFacturas));
            if (lblFacturacionMes != null) lblFacturacionMes.setText(
                String.format("%,.2f €", factMes));
            if (lblFacturacionAnio != null) lblFacturacionAnio.setText(
                String.format("%,.2f €", factAnio));
        } catch (Exception e) {
            // No crítico si falla
        }
    }

    private void configurarTablaUltimas() {
        if (tablaUltimas == null) return;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colUltNumero.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        colUltCliente.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCliente() != null ? d.getValue().getCliente().getNombre() : "-"));
        colUltFecha.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getFechaEmision() != null
                ? d.getValue().getFechaEmision().format(fmt) : "-"));
        colUltTotal.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%,.2f €", d.getValue().getTotal())));
        colUltEstado.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getEstado().getEtiqueta()));
        tablaUltimas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void configurarTablaTopClientes() {
        if (tablaTopClientes == null) return;
        colTopNombre.setCellValueFactory(d -> new SimpleStringProperty((String) d.getValue()[0]));
        colTopTotal.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%,.2f €", (BigDecimal) d.getValue()[1])));
        tablaTopClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void cargarUltimasFacturas() {
        if (tablaUltimas == null) return;
        try {
            List<Factura> todas = facturaUseCase.listarFacturas();
            // Mostrar solo las 10 más recientes
            List<Factura> ultimas = todas.stream().limit(10).toList();
            tablaUltimas.setItems(FXCollections.observableArrayList(ultimas));
        } catch (Exception ignored) {}
    }

    private void cargarTopClientes() {
        if (tablaTopClientes == null) return;
        try {
            List<Object[]> top = facturaUseCase.topClientes(5);
            tablaTopClientes.setItems(FXCollections.observableArrayList(top));
        } catch (Exception ignored) {}
    }
}
