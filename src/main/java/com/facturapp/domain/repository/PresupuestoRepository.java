package com.facturapp.domain.repository;

import com.facturapp.domain.model.Presupuesto;

import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository {
    Presupuesto save(Presupuesto p);
    Optional<Presupuesto> findById(Long id);
    List<Presupuesto> findAll();
    List<Presupuesto> findByClienteId(Long clienteId);
    void delete(Long id);
    long count();
    String generarNumero();
}
