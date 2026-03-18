package com.facturapp.domain.model;

import java.time.LocalDate;

/**
 * Entidad de dominio: Logo de empresa.
 * Almacena metadatos del logo. El fichero de imagen se guarda en disco.
 * Solo un logo puede estar marcado como activo (usado en las facturas PDF).
 */
public class EmpresaLogo {

    private Long id;
    private String nombre;        // Nombre descriptivo, p.ej. "Empresa Principal"
    private String rutaArchivo;   // Ruta absoluta al fichero de imagen en disco
    private LocalDate fechaSubida;
    private boolean activo;       // TRUE = este logo se usa en las facturas

    public EmpresaLogo() {
        this.fechaSubida = LocalDate.now();
        this.activo = false;
    }

    public EmpresaLogo(Long id, String nombre, String rutaArchivo,
                       LocalDate fechaSubida, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.rutaArchivo = rutaArchivo;
        this.fechaSubida = fechaSubida;
        this.activo = activo;
    }

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }

    public LocalDate getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDate fechaSubida) { this.fechaSubida = fechaSubida; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return nombre + (activo ? " ★" : "");
    }
}
