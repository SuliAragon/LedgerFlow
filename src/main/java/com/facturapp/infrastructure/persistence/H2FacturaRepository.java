package com.facturapp.infrastructure.persistence;

import com.facturapp.domain.model.*;
import com.facturapp.domain.repository.ClienteRepository;
import com.facturapp.domain.repository.FacturaRepository;
import com.facturapp.domain.repository.ProductoRepository;
import com.facturapp.infrastructure.config.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del repositorio de facturas usando H2.
 * Maneja la persistencia de facturas y sus líneas de detalle.
 */
public class H2FacturaRepository implements FacturaRepository {

    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

    public H2FacturaRepository(ClienteRepository clienteRepository,
                                ProductoRepository productoRepository) {
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
    }

    private Connection getConn() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    @Override
    public Factura save(Factura factura) {
        if (factura.getId() == null) {
            return insertar(factura);
        } else {
            return actualizar(factura);
        }
    }

    private Factura insertar(Factura factura) {
        String sql = "INSERT INTO facturas (numero, cliente_id, fecha_emision, estado, observaciones, empresa_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, factura.getNumero());
            ps.setLong(2, factura.getCliente().getId());
            ps.setDate(3, Date.valueOf(factura.getFechaEmision()));
            ps.setString(4, factura.getEstado().name());
            ps.setString(5, factura.getObservaciones());
            if (factura.getEmpresaId() != null) ps.setLong(6, factura.getEmpresaId()); else ps.setNull(6, java.sql.Types.BIGINT);
            ps.executeUpdate();

            var keys = ps.getGeneratedKeys();
            if (keys.next()) factura.setId(keys.getLong(1));

            // Insertar líneas de la factura
            for (LineaFactura linea : factura.getLineas()) {
                insertarLinea(factura.getId(), linea);
            }
            return factura;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando factura", e);
        }
    }

    private void insertarLinea(Long facturaId, LineaFactura linea) throws SQLException {
        String sql = "INSERT INTO lineas_factura (factura_id, producto_id, cantidad, precio_unitario, descuento) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, facturaId);
            ps.setLong(2, linea.getProducto().getId());
            ps.setInt(3, linea.getCantidad());
            ps.setBigDecimal(4, linea.getPrecioUnitario());
            ps.setBigDecimal(5, linea.getDescuento());
            ps.executeUpdate();
            var keys = ps.getGeneratedKeys();
            if (keys.next()) linea.setId(keys.getLong(1));
        }
    }

    private Factura actualizar(Factura factura) {
        String sql = "UPDATE facturas SET cliente_id=?, fecha_emision=?, estado=?, observaciones=?, empresa_id=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, factura.getCliente().getId());
            ps.setDate(2, Date.valueOf(factura.getFechaEmision()));
            ps.setString(3, factura.getEstado().name());
            ps.setString(4, factura.getObservaciones());
            if (factura.getEmpresaId() != null) ps.setLong(5, factura.getEmpresaId()); else ps.setNull(5, java.sql.Types.BIGINT);
            ps.setLong(6, factura.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando factura", e);
        }
        // Reemplazar líneas: borrar las existentes e insertar las nuevas
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM lineas_factura WHERE factura_id = ?")) {
            ps.setLong(1, factura.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando líneas de factura", e);
        }
        try {
            for (LineaFactura linea : factura.getLineas()) {
                insertarLinea(factura.getId(), linea);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reinsertando líneas de factura", e);
        }
        return factura;
    }

    @Override
    public Optional<Factura> findById(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM facturas WHERE id = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Factura f = mapear(rs);
                f.setLineas(cargarLineas(f.getId()));
                return Optional.of(f);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando factura por ID", e);
        }
    }

    @Override
    public List<Factura> findAll() {
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM facturas ORDER BY fecha_emision DESC");
            List<Factura> lista = new ArrayList<>();
            while (rs.next()) {
                Factura f = mapear(rs);
                f.setLineas(cargarLineas(f.getId()));
                lista.add(f);
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando facturas", e);
        }
    }

    @Override
    public List<Factura> findByClienteId(Long clienteId) {
        String sql = "SELECT * FROM facturas WHERE cliente_id = ? ORDER BY fecha_emision DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, clienteId);
            ResultSet rs = ps.executeQuery();
            List<Factura> lista = new ArrayList<>();
            while (rs.next()) {
                Factura f = mapear(rs);
                f.setLineas(cargarLineas(f.getId()));
                lista.add(f);
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando facturas por cliente", e);
        }
    }

    @Override
    public List<Factura> findByFechaEntre(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT * FROM facturas WHERE fecha_emision BETWEEN ? AND ? ORDER BY fecha_emision DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            List<Factura> lista = new ArrayList<>();
            while (rs.next()) {
                Factura f = mapear(rs);
                f.setLineas(cargarLineas(f.getId()));
                lista.add(f);
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando facturas por fecha", e);
        }
    }

    @Override
    public List<Factura> findByEstado(Factura.Estado estado) {
        String sql = "SELECT * FROM facturas WHERE estado = ? ORDER BY fecha_emision DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ResultSet rs = ps.executeQuery();
            List<Factura> lista = new ArrayList<>();
            while (rs.next()) {
                Factura f = mapear(rs);
                f.setLineas(cargarLineas(f.getId()));
                lista.add(f);
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando facturas por estado", e);
        }
    }

    @Override
    public void delete(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM facturas WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando factura", e);
        }
    }

    @Override
    public long count() {
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM facturas");
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error contando facturas", e);
        }
    }

    @Override
    public String generarNumeroFactura() {
        int anio = Year.now().getValue();
        String sql = "SELECT COUNT(*) FROM facturas WHERE YEAR(fecha_emision) = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, anio);
            ResultSet rs = ps.executeQuery();
            int siguiente = rs.next() ? rs.getInt(1) + 1 : 1;
            return String.format("FAC-%d-%04d", anio, siguiente);
        } catch (SQLException e) {
            throw new RuntimeException("Error generando número de factura", e);
        }
    }

    @Override
    public BigDecimal totalFacturadoEntre(LocalDate desde, LocalDate hasta) {
        // Calcula el total sumando precio * cantidad de todas las líneas en el período
        String sql = """
            SELECT COALESCE(SUM(l.precio_unitario * l.cantidad * (1 - l.descuento/100.0)), 0)
            FROM lineas_factura l
            JOIN facturas f ON l.factura_id = f.id
            WHERE f.fecha_emision BETWEEN ? AND ?
            AND f.estado <> 'ANULADA'
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new RuntimeException("Error calculando total facturado", e);
        }
    }

    @Override
    public List<Object[]> topClientesPorFacturacion(int limite) {
        String sql = """
            SELECT c.nombre, COALESCE(SUM(l.precio_unitario * l.cantidad), 0) AS total
            FROM clientes c
            JOIN facturas f ON c.id = f.cliente_id
            JOIN lineas_factura l ON f.id = l.factura_id
            WHERE f.estado <> 'ANULADA'
            GROUP BY c.id, c.nombre
            ORDER BY total DESC
            LIMIT ?
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            List<Object[]> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new Object[]{rs.getString("nombre"), rs.getBigDecimal("total")});
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo top clientes", e);
        }
    }

    /** Carga las líneas de detalle de una factura */
    private List<LineaFactura> cargarLineas(Long facturaId) throws SQLException {
        String sql = "SELECT * FROM lineas_factura WHERE factura_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, facturaId);
            ResultSet rs = ps.executeQuery();
            List<LineaFactura> lineas = new ArrayList<>();
            while (rs.next()) {
                LineaFactura l = new LineaFactura();
                l.setId(rs.getLong("id"));
                long productoId = rs.getLong("producto_id");
                productoRepository.findById(productoId).ifPresent(l::setProducto);
                l.setCantidad(rs.getInt("cantidad"));
                l.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                l.setDescuento(rs.getBigDecimal("descuento"));
                lineas.add(l);
            }
            return lineas;
        }
    }

    private Factura mapear(ResultSet rs) throws SQLException {
        Factura f = new Factura();
        f.setId(rs.getLong("id"));
        f.setNumero(rs.getString("numero"));
        long clienteId = rs.getLong("cliente_id");
        clienteRepository.findById(clienteId).ifPresent(f::setCliente);
        Date fecha = rs.getDate("fecha_emision");
        if (fecha != null) f.setFechaEmision(fecha.toLocalDate());
        f.setEstado(Factura.Estado.valueOf(rs.getString("estado")));
        f.setObservaciones(rs.getString("observaciones"));
        f.setEmpresaId(rs.getLong("empresa_id")); if (rs.wasNull()) f.setEmpresaId(null);
        return f;
    }
}
