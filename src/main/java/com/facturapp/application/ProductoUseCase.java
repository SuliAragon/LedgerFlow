package com.facturapp.application;

import com.facturapp.domain.model.Producto;
import com.facturapp.domain.repository.ProductoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Caso de uso: Gestión completa de productos y servicios (CRUD).
 */
public class ProductoUseCase {

    private static final Logger log = Logger.getLogger(ProductoUseCase.class.getName());
    private static final Set<Integer> IVA_VALIDOS = Set.of(0, 4, 10, 21);

    private final ProductoRepository productoRepository;

    public ProductoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Crea un nuevo producto con validaciones.
     */
    public Producto crearProducto(String nombre, String descripcion,
                                   BigDecimal precio, int porcentajeIva) {
        validarProducto(nombre, precio, porcentajeIva);

        Producto producto = new Producto();
        producto.setNombre(nombre.trim());
        producto.setDescripcion(descripcion != null ? descripcion.trim() : null);
        producto.setPrecio(precio.setScale(2, java.math.RoundingMode.HALF_UP));
        producto.setPorcentajeIva(porcentajeIva);
        producto.setActivo(true);

        Producto guardado = productoRepository.save(producto);
        log.info("Producto creado: " + guardado.getNombre() + " (ID: " + guardado.getId() + ")");
        return guardado;
    }

    /**
     * Actualiza un producto existente.
     */
    public Producto actualizarProducto(Long id, String nombre, String descripcion,
                                        BigDecimal precio, int porcentajeIva) {
        validarProducto(nombre, precio, porcentajeIva);

        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        producto.setNombre(nombre.trim());
        producto.setDescripcion(descripcion != null ? descripcion.trim() : null);
        producto.setPrecio(precio.setScale(2, java.math.RoundingMode.HALF_UP));
        producto.setPorcentajeIva(porcentajeIva);

        return productoRepository.save(producto);
    }

    /**
     * Activa o desactiva un producto (borrado lógico).
     */
    public void toggleActivoProducto(Long id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        producto.setActivo(!producto.isActivo());
        productoRepository.save(producto);
    }

    /** Elimina físicamente un producto */
    public void eliminarProducto(Long id) {
        productoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        productoRepository.delete(id);
        log.info("Producto eliminado ID: " + id);
    }

    /** Lista todos los productos (activos e inactivos) */
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    /** Lista solo los productos activos (para usarlos en facturas) */
    public List<Producto> listarProductosActivos() {
        return productoRepository.findActivos();
    }

    /** Busca un producto por ID */
    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findById(id);
    }

    /** Busca productos por nombre */
    public List<Producto> buscarPorNombre(String nombre) {
        return productoRepository.findByNombre(nombre);
    }

    /** Total de productos registrados */
    public long totalProductos() {
        return productoRepository.count();
    }

    private void validarProducto(String nombre, BigDecimal precio, int porcentajeIva) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio.");
        }
        if (precio == null || precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        if (!IVA_VALIDOS.contains(porcentajeIva)) {
            throw new IllegalArgumentException("El IVA debe ser 0%, 4%, 10% o 21%.");
        }
    }
}
