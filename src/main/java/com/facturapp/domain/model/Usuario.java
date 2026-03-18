package com.facturapp.domain.model;

import java.time.LocalDateTime;

/**
 * Entidad de dominio: Usuario del sistema.
 * Contiene credenciales, rol y estado de activación.
 */
public class Usuario {

    private Long id;
    private String username;
    private String passwordHash;
    private String email;
    private Rol rol;
    private boolean activo;
    private LocalDateTime fechaCreacion;

    /** Roles disponibles en la aplicación */
    public enum Rol {
        ADMIN("Administrador"),
        USUARIO("Usuario");

        private final String descripcion;

        Rol(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public Usuario() {
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Usuario(Long id, String username, String passwordHash, String email, Rol rol, boolean activo) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.rol = rol;
        this.activo = activo;
        this.fechaCreacion = LocalDateTime.now();
    }

    public boolean esAdmin() {
        return Rol.ADMIN.equals(this.rol);
    }

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", username='" + username + "', rol=" + rol + "}";
    }
}
