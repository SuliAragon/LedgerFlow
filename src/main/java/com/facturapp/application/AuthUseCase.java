package com.facturapp.application;

import com.facturapp.domain.model.Usuario;
import com.facturapp.domain.repository.UsuarioRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Caso de uso: Autenticación y gestión de usuarios.
 * Maneja login, registro y cambio de contraseña.
 */
public class AuthUseCase {

    private static final Logger log = Logger.getLogger(AuthUseCase.class.getName());
    private final UsuarioRepository usuarioRepository;

    public AuthUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Autentica un usuario por username y contraseña.
     * @return Optional con el usuario si las credenciales son válidas.
     */
    public Optional<Usuario> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username.trim());

        if (usuarioOpt.isEmpty()) {
            log.warning("Intento de login con usuario inexistente: " + username);
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();

        if (!usuario.isActivo()) {
            log.warning("Intento de login con usuario inactivo: " + username);
            return Optional.empty();
        }

        String hashIngresado = hashPassword(password);
        if (!hashIngresado.equals(usuario.getPasswordHash())) {
            log.warning("Contraseña incorrecta para usuario: " + username);
            return Optional.empty();
        }

        log.info("Login exitoso: " + username);
        return Optional.of(usuario);
    }

    /**
     * Crea un nuevo usuario en el sistema.
     * @throws IllegalArgumentException si el username ya existe.
     */
    public Usuario crearUsuario(String username, String password,
                                 String email, Usuario.Rol rol) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El usuario '" + username + "' ya existe.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username.trim());
        usuario.setPasswordHash(hashPassword(password));
        usuario.setEmail(email);
        usuario.setRol(rol);
        usuario.setActivo(true);

        return usuarioRepository.save(usuario);
    }

    /**
     * Cambia la contraseña de un usuario existente.
     */
    public void cambiarPassword(Long usuarioId, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
        }

        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            u.setPasswordHash(hashPassword(nuevaPassword));
            usuarioRepository.save(u);
        });
    }

    /** Obtiene todos los usuarios del sistema */
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    /** Activa o desactiva un usuario */
    public void toggleEstadoUsuario(Long usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            u.setActivo(!u.isActivo());
            usuarioRepository.save(u);
        });
    }

    /** Elimina un usuario del sistema */
    public void eliminarUsuario(Long usuarioId) {
        usuarioRepository.delete(usuarioId);
    }

    /** Genera el hash SHA-256 de una contraseña */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al generar hash de contraseña", e);
        }
    }
}
