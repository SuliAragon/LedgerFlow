package com.facturapp.domain.repository;

import com.facturapp.domain.model.Factura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Puerto (interfaz) del repositorio de facturas.
 */
public interface FacturaRepository {
    Factura save(Factura factura);
    Optional<Factura> findById(Long id);
    List<Factura> findAll();
    List<Factura> findByClienteId(Long clienteId);
    List<Factura> findByFechaEntre(LocalDate desde, LocalDate hasta);
    List<Factura> findByEstado(Factura.Estado estado);
    void delete(Long id);
    long count();

    /** Siguiente número correlativo de factura */
    String generarNumeroFactura();

    /** Total facturado en un período */
    BigDecimal totalFacturadoEntre(LocalDate desde, LocalDate hasta);

    /** Top N clientes por volumen de facturación */
    List<Object[]> topClientesPorFacturacion(int limite);
}
