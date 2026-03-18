package com.facturapp.domain.model;

import java.math.BigDecimal;

/**
 * Entidad de dominio: Producto o Servicio.
 * Usado como línea de detalle en las facturas.
 */
public class Producto {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private int porcentajeIva; // Valores válidos: 0, 4, 10, 21
    private boolean activo;

    public Producto() {
        this.activo = true;
        this.porcentajeIva = 21;
        this.precio = BigDecimal.ZERO;
    }

    public Producto(Long id, String nombre, String descripcion,
                    BigDecimal precio, int porcentajeIva, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.porcentajeIva = porcentajeIva;
        this.activo = activo;
    }

    /** Calcula el precio con IVA incluido */
    public BigDecimal getPrecioConIva() {
        BigDecimal factor = BigDecimal.ONE.add(
            BigDecimal.valueOf(porcentajeIva).divide(BigDecimal.valueOf(100))
        );
        return precio.multiply(factor);
    }

    /** Retorna el texto del IVA para mostrar en listas */
    public String getIvaTexto() {
        return porcentajeIva + "%";
    }

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public int getPorcentajeIva() { return porcentajeIva; }
    public void setPorcentajeIva(int porcentajeIva) { this.porcentajeIva = porcentajeIva; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return nombre + " - " + precio + "€ (IVA " + porcentajeIva + "%)";
    }
}
