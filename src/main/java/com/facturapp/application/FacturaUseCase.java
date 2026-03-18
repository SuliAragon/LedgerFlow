package com.facturapp.application;

import com.facturapp.domain.model.Cliente;
import com.facturapp.domain.model.Factura;
import com.facturapp.domain.model.LineaFactura;
import com.facturapp.domain.model.Producto;
import com.facturapp.domain.repository.ClienteRepository;
import com.facturapp.domain.repository.FacturaRepository;
import com.facturapp.domain.repository.ProductoRepository;
import com.facturapp.infrastructure.pdf.PdfFacturaGenerator;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Caso de uso: Creación, gestión y consulta de facturas.
 * También coordina la generación de PDF.
 */
public class FacturaUseCase {

    private static final Logger log = Logger.getLogger(FacturaUseCase.class.getName());

    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PdfFacturaGenerator pdfGenerator;

    public FacturaUseCase(FacturaRepository facturaRepository,
                          ClienteRepository clienteRepository,
                          ProductoRepository productoRepository,
                          PdfFacturaGenerator pdfGenerator) {
        this.facturaRepository = facturaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.pdfGenerator = pdfGenerator;
    }

    /**
     * Crea y persiste una nueva factura con sus líneas de detalle.
     * @param clienteId  ID del cliente destinatario
     * @param empresaId  ID de la empresa emisora (puede ser null)
     * @param lineas     Líneas de la factura a añadir
     * @param observaciones Texto libre opcional
     */
    public Factura crearFactura(Long clienteId, Long empresaId, List<LineaFactura> lineas, String observaciones) {
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("La factura debe tener al menos una línea.");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + clienteId));

        Factura factura = new Factura();
        factura.setNumero(facturaRepository.generarNumeroFactura());
        factura.setCliente(cliente);
        factura.setFechaEmision(LocalDate.now());
        factura.setEstado(Factura.Estado.EMITIDA);
        factura.setObservaciones(observaciones);
        factura.setEmpresaId(empresaId);
        factura.setLineas(lineas);

        Factura guardada = facturaRepository.save(factura);
        log.info("Factura creada: " + guardada.getNumero() + " - Total: " + guardada.getTotal() + "€");
        return guardada;
    }

    /**
     * Cambia el estado de una factura existente.
     */
    public Factura cambiarEstado(Long facturaId, Factura.Estado nuevoEstado) {
        Factura factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + facturaId));
        factura.setEstado(nuevoEstado);
        return facturaRepository.save(factura);
    }

    /**
     * Genera el PDF de una factura y lo guarda en la carpeta Downloads.
     * @return Path del archivo PDF generado.
     */
    public Path generarPdf(Long facturaId) {
        Factura factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + facturaId));
        return pdfGenerator.generar(factura);
    }

    /** Elimina una factura por su ID */
    public void eliminarFactura(Long id) {
        facturaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + id));
        facturaRepository.delete(id);
    }

    /** Lista todas las facturas */
    public List<Factura> listarFacturas() {
        return facturaRepository.findAll();
    }

    /** Filtra facturas por rango de fechas */
    public List<Factura> filtrarPorFecha(LocalDate desde, LocalDate hasta) {
        return facturaRepository.findByFechaEntre(desde, hasta);
    }

    /** Filtra facturas de un cliente concreto */
    public List<Factura> filtrarPorCliente(Long clienteId) {
        return facturaRepository.findByClienteId(clienteId);
    }

    /** Busca una factura por ID */
    public Optional<Factura> buscarPorId(Long id) {
        return facturaRepository.findById(id);
    }

    /** Total de facturas en el sistema */
    public long totalFacturas() {
        return facturaRepository.count();
    }

    /** Facturación total en un período */
    public BigDecimal totalFacturadoEntre(LocalDate desde, LocalDate hasta) {
        return facturaRepository.totalFacturadoEntre(desde, hasta);
    }

    /** Top clientes por volumen de facturación */
    public List<Object[]> topClientes(int limite) {
        return facturaRepository.topClientesPorFacturacion(limite);
    }
}
