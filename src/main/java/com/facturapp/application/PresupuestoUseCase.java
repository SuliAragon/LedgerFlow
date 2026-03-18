package com.facturapp.application;

import com.facturapp.domain.model.Cliente;
import com.facturapp.domain.model.LineaFactura;
import com.facturapp.domain.model.Presupuesto;
import com.facturapp.domain.repository.ClienteRepository;
import com.facturapp.domain.repository.PresupuestoRepository;
import com.facturapp.infrastructure.pdf.PdfFacturaGenerator;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class PresupuestoUseCase {

    private static final Logger log = Logger.getLogger(PresupuestoUseCase.class.getName());

    private final PresupuestoRepository presupuestoRepository;
    private final ClienteRepository clienteRepository;
    private final PdfFacturaGenerator pdfGenerator;

    public PresupuestoUseCase(PresupuestoRepository presupuestoRepository,
                               ClienteRepository clienteRepository,
                               PdfFacturaGenerator pdfGenerator) {
        this.presupuestoRepository = presupuestoRepository;
        this.clienteRepository = clienteRepository;
        this.pdfGenerator = pdfGenerator;
    }

    public Presupuesto crearPresupuesto(Long clienteId, Long empresaId, LocalDate fechaEmision,
                                         List<LineaFactura> lineas, String observaciones) {
        if (lineas == null || lineas.isEmpty())
            throw new IllegalArgumentException("El presupuesto debe tener al menos una línea.");

        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + clienteId));

        Presupuesto p = new Presupuesto();
        p.setNumero(presupuestoRepository.generarNumero());
        p.setCliente(cliente);
        p.setFechaEmision(fechaEmision != null ? fechaEmision : LocalDate.now());
        p.setEstado(Presupuesto.Estado.BORRADOR);
        p.setObservaciones(observaciones);
        p.setEmpresaId(empresaId);
        p.setLineas(lineas);

        Presupuesto guardado = presupuestoRepository.save(p);
        log.info("Presupuesto creado: " + guardado.getNumero() + " - Total: " + guardado.getTotal() + "€");
        return guardado;
    }

    public Presupuesto actualizarPresupuesto(Presupuesto p) {
        if (p.getLineas() == null || p.getLineas().isEmpty())
            throw new IllegalArgumentException("El presupuesto debe tener al menos una línea.");
        Presupuesto guardado = presupuestoRepository.save(p);
        log.info("Presupuesto actualizado: " + guardado.getNumero());
        return guardado;
    }

    public Presupuesto cambiarEstado(Long id, Presupuesto.Estado nuevoEstado) {
        Presupuesto p = presupuestoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Presupuesto no encontrado: " + id));
        p.setEstado(nuevoEstado);
        return presupuestoRepository.save(p);
    }

    public Path generarPdf(Long id) {
        Presupuesto p = presupuestoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Presupuesto no encontrado: " + id));
        return pdfGenerator.generarPresupuesto(p);
    }

    public void eliminarPresupuesto(Long id) {
        presupuestoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Presupuesto no encontrado: " + id));
        presupuestoRepository.delete(id);
    }

    public List<Presupuesto> listarPresupuestos() {
        return presupuestoRepository.findAll();
    }

    public Optional<Presupuesto> buscarPorId(Long id) {
        return presupuestoRepository.findById(id);
    }

    public long totalPresupuestos() {
        return presupuestoRepository.count();
    }
}
