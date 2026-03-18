package com.facturapp.domain.repository;

import com.facturapp.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (interfaz) del repositorio de usuarios.
 * Define el contrato de acceso a datos independiente de la tecnología.
 */
public interface UsuarioRepository {
    Usuario save(Usuario usuario);
    Optional<Usuario> findById(Long id);
    Optional<Usuario> findByUsername(String username);
    List<Usuario> findAll();
    void delete(Long id);
    boolean existsByUsername(String username);
}
