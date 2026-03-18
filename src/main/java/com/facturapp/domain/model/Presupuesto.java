package com.facturapp.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad de dominio: Presupuesto.
 * Mismo modelo de líneas que Factura, PDF idéntico pero etiquetado como PRESUPUESTO.
 */
public class Presupuesto {

    private Long id;
    private String numero;       // PRE-2026-0001
    private Cliente cliente;
    private List<LineaFactura> lineas;
    private LocalDate fechaEmision;
    private Estado estado;
    private String observaciones;
    private Long empresaId;

    public enum Estado {
        BORRADOR("Borrador"),
        ENVIADO("Enviado"),
        ACEPTADO("Aceptado"),
        RECHAZADO("Rechazado"),
        EXPIRADO("Expirado");

        private final String etiqueta;
        Estado(String etiqueta) { this.etiqueta = etiqueta; }
        public String getEtiqueta() { return etiqueta; }
    }

    public Presupuesto() {
        this.lineas = new ArrayList<>();
        this.fechaEmision = LocalDate.now();
        this.estado = Estado.BORRADOR;
    }

    public BigDecimal getSubtotal() {
        return lineas.stream()
            .map(LineaFactura::getBaseImponible)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalIva() {
        return lineas.stream()
            .map(LineaFactura::getImporteIva)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotal() {
        return getSubtotal().add(getTotalIva()).setScale(2, RoundingMode.HALF_UP);
    }

    public void addLinea(LineaFactura linea) { this.lineas.add(linea); }

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public List<LineaFactura> getLineas() { return lineas; }
    public void setLineas(List<LineaFactura> lineas) { this.lineas = lineas; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    @Override
    public String toString() {
        return "Presupuesto{numero='" + numero + "', cliente=" +
            (cliente != null ? cliente.getNombre() : "N/A") +
            ", total=" + getTotal() + "€}";
    }
}
