package com.facturapp.domain.model;

import java.time.LocalDate;

/**
 * Entidad de dominio: Cliente.
 * Representa a una persona física o jurídica que recibe facturas.
 */
public class Cliente {

    private Long id;
    private String nombre;
    private String nifCif;
    private String email;
    private String telefono;
    private String direccion;
    private LocalDate fechaCreacion;

    public Cliente() {
        this.fechaCreacion = LocalDate.now();
    }

    public Cliente(Long id, String nombre, String nifCif, String email,
                   String telefono, String direccion) {
        this.id = id;
        this.nombre = nombre;
        this.nifCif = nifCif;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
        this.fechaCreacion = LocalDate.now();
    }

    /** Retorna el nombre para mostrar en listas y combos */
    public String getNombreCompleto() {
        return nombre + " (" + nifCif + ")";
    }

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNifCif() { return nifCif; }
    public void setNifCif(String nifCif) { this.nifCif = nifCif; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    @Override
    public String toString() {
        return nombre + " - " + nifCif;
    }
}
