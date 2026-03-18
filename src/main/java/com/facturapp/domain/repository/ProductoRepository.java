package com.facturapp.domain.repository;

import com.facturapp.domain.model.Producto;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (interfaz) del repositorio de productos.
 */
public interface ProductoRepository {
    Producto save(Producto producto);
    Optional<Producto> findById(Long id);
    List<Producto> findAll();
    List<Producto> findActivos();
    List<Producto> findByNombre(String nombre);
    void delete(Long id);
    long count();
}
