package com.facturapp.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Entidad de dominio: Línea de detalle de una factura.
 * Contiene producto, cantidad, precio y descuento aplicado.
 */
public class LineaFactura {

    private Long id;
    private Producto producto;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuento; // Porcentaje de descuento (0-100)

    public LineaFactura() {
        this.cantidad = 1;
        this.descuento = BigDecimal.ZERO;
    }

    public LineaFactura(Producto producto, int cantidad,
                        BigDecimal precioUnitario, BigDecimal descuento) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.descuento = descuento != null ? descuento : BigDecimal.ZERO;
    }

    /** Base imponible (sin IVA, con descuento aplicado) */
    public BigDecimal getBaseImponible() {
        BigDecimal bruto = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        if (descuento.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal factorDescuento = BigDecimal.ONE.subtract(
                descuento.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            );
            return bruto.multiply(factorDescuento).setScale(2, RoundingMode.HALF_UP);
        }
        return bruto.setScale(2, RoundingMode.HALF_UP);
    }

    /** Importe de IVA para esta línea */
    public BigDecimal getImporteIva() {
        if (producto == null) return BigDecimal.ZERO;
        BigDecimal factorIva = BigDecimal.valueOf(producto.getPorcentajeIva())
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return getBaseImponible().multiply(factorIva).setScale(2, RoundingMode.HALF_UP);
    }

    /** Total de la línea (base + IVA) */
    public BigDecimal getTotalLinea() {
        return getBaseImponible().add(getImporteIva()).setScale(2, RoundingMode.HALF_UP);
    }

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }
}
