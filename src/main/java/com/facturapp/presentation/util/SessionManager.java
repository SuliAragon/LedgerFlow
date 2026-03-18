package com.facturapp.presentation.util;

import com.facturapp.domain.model.Usuario;

/**
 * Gestor de sesión de usuario (Singleton).
 * Mantiene el usuario autenticado durante toda la sesión de la aplicación.
 */
public class SessionManager {

    private static SessionManager instancia;
    private Usuario usuarioActual;

    private SessionManager() {}

    /** Obtiene la instancia única del gestor de sesión */
    public static synchronized SessionManager getInstance() {
        if (instancia == null) {
            instancia = new SessionManager();
        }
        return instancia;
    }

    /** Establece el usuario actualmente autenticado */
    public void iniciarSesion(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    /** Cierra la sesión del usuario actual */
    public void cerrarSesion() {
        this.usuarioActual = null;
    }

    /** Retorna el usuario autenticado, o null si no hay sesión */
    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    /** Comprueba si hay una sesión activa */
    public boolean haySession() {
        return usuarioActual != null;
    }

    /** Comprueba si el usuario actual tiene rol ADMIN */
    public boolean esAdmin() {
        return haySession() && usuarioActual.esAdmin();
    }
}
