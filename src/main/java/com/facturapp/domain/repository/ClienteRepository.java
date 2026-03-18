package com.facturapp.domain.repository;

import com.facturapp.domain.model.Cliente;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (interfaz) del repositorio de clientes.
 */
public interface ClienteRepository {
    Cliente save(Cliente cliente);
    Optional<Cliente> findById(Long id);
    List<Cliente> findAll();
    List<Cliente> findByNombre(String nombre);
    void delete(Long id);
    boolean existsByNifCif(String nifCif);
    long count();
}
