package com.facturapp.application;

import com.facturapp.domain.model.EmpresaConfig;
import com.facturapp.infrastructure.persistence.H2EmpresaConfigRepository;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class EmpresaConfigUseCase {

    private static final Logger log = Logger.getLogger(EmpresaConfigUseCase.class.getName());
    private final H2EmpresaConfigRepository repo;

    public EmpresaConfigUseCase(H2EmpresaConfigRepository repo) {
        this.repo = repo;
    }

    public List<EmpresaConfig> listar() {
        return repo.listar();
    }

    public Optional<EmpresaConfig> findById(Long id) {
        return repo.findById(id);
    }

    public EmpresaConfig guardar(EmpresaConfig config) {
        EmpresaConfig saved = repo.guardar(config);
        log.info("Empresa guardada: " + saved.getNombreEmpresa() + " (id=" + saved.getId() + ")");
        return saved;
    }

    public void eliminar(Long id) {
        repo.eliminar(id);
        log.info("Empresa eliminada id=" + id);
    }
}
