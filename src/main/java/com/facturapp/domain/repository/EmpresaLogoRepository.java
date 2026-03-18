package com.facturapp.domain.repository;

import com.facturapp.domain.model.EmpresaLogo;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (interfaz) del repositorio de logos de empresa.
 */
public interface EmpresaLogoRepository {
    EmpresaLogo save(EmpresaLogo logo);
    Optional<EmpresaLogo> findById(Long id);
    List<EmpresaLogo> findAll();
    void delete(Long id);

    /** Retorna el logo marcado como activo, si existe */
    Optional<EmpresaLogo> findActivo();

    /** Marca un logo como activo y desactiva todos los demás */
    void setActivo(Long id);
}
