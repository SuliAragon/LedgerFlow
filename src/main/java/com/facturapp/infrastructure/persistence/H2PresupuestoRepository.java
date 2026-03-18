package com.facturapp.infrastructure.persistence;

import com.facturapp.domain.model.*;
import com.facturapp.domain.repository.ClienteRepository;
import com.facturapp.domain.repository.PresupuestoRepository;
import com.facturapp.domain.repository.ProductoRepository;
import com.facturapp.infrastructure.config.DatabaseConfig;

import java.sql.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2PresupuestoRepository implements PresupuestoRepository {

    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

    public H2PresupuestoRepository(ClienteRepository clienteRepository,
                                    ProductoRepository productoRepository) {
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
    }

    private Connection getConn() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    @Override
    public Presupuesto save(Presupuesto p) {
        return p.getId() == null ? insertar(p) : actualizar(p);
    }

    private Presupuesto insertar(Presupuesto p) {
        String sql = "INSERT INTO presupuestos (numero, cliente_id, fecha_emision, estado, observaciones, empresa_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNumero());
            ps.setLong(2, p.getCliente().getId());
            ps.setDate(3, Date.valueOf(p.getFechaEmision()));
            ps.setString(4, p.getEstado().name());
            ps.setString(5, p.getObservaciones());
            if (p.getEmpresaId() != null) ps.setLong(6, p.getEmpresaId()); else ps.setNull(6, Types.BIGINT);
            ps.executeUpdate();
            var keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getLong(1));
            for (LineaFactura linea : p.getLineas()) insertarLinea(p.getId(), linea);
            return p;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando presupuesto", e);
        }
    }

    private void insertarLinea(Long presupuestoId, LineaFactura linea) throws SQLException {
        String sql = "INSERT INTO lineas_presupuesto (presupuesto_id, producto_id, cantidad, precio_unitario, descuento) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, presupuestoId);
            ps.setLong(2, linea.getProducto().getId());
            ps.setInt(3, linea.getCantidad());
            ps.setBigDecimal(4, linea.getPrecioUnitario());
            ps.setBigDecimal(5, linea.getDescuento());
            ps.executeUpdate();
            var keys = ps.getGeneratedKeys();
            if (keys.next()) linea.setId(keys.getLong(1));
        }
    }

    private Presupuesto actualizar(Presupuesto p) {
        String sql = "UPDATE presupuestos SET cliente_id=?, fecha_emision=?, estado=?, observaciones=?, empresa_id=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, p.getCliente().getId());
            ps.setDate(2, Date.valueOf(p.getFechaEmision()));
            ps.setString(3, p.getEstado().name());
            ps.setString(4, p.getObservaciones());
            if (p.getEmpresaId() != null) ps.setLong(5, p.getEmpresaId()); else ps.setNull(5, Types.BIGINT);
            ps.setLong(6, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando presupuesto", e);
        }
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM lineas_presupuesto WHERE presupuesto_id = ?")) {
            ps.setLong(1, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando líneas de presupuesto", e);
        }
        try {
            for (LineaFactura linea : p.getLineas()) insertarLinea(p.getId(), linea);
        } catch (SQLException e) {
            throw new RuntimeException("Error reinsertando líneas de presupuesto", e);
        }
        return p;
    }

    @Override
    public Optional<Presupuesto> findById(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM presupuestos WHERE id = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Presupuesto p = mapear(rs);
                p.setLineas(cargarLineas(p.getId()));
                return Optional.of(p);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando presupuesto por ID", e);
        }
    }

    @Override
    public List<Presupuesto> findAll() {
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM presupuestos ORDER BY fecha_emision DESC");
            List<Presupuesto> lista = new ArrayList<>();
            while (rs.next()) {
                Presupuesto p = mapear(rs);
                p.setLineas(cargarLineas(p.getId()));
                lista.add(p);
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando presupuestos", e);
        }
    }

    @Override
    public List<Presupuesto> findByClienteId(Long clienteId) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT * FROM presupuestos WHERE cliente_id = ? ORDER BY fecha_emision DESC")) {
            ps.setLong(1, clienteId);
            ResultSet rs = ps.executeQuery();
            List<Presupuesto> lista = new ArrayList<>();
            while (rs.next()) {
                Presupuesto p = mapear(rs);
                p.setLineas(cargarLineas(p.getId()));
                lista.add(p);
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando presupuestos por cliente", e);
        }
    }

    @Override
    public void delete(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM presupuestos WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando presupuesto", e);
        }
    }

    @Override
    public long count() {
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM presupuestos");
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error contando presupuestos", e);
        }
    }

    @Override
    public String generarNumero() {
        int anio = Year.now().getValue();
        String sql = "SELECT COUNT(*) FROM presupuestos WHERE YEAR(fecha_emision) = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, anio);
            ResultSet rs = ps.executeQuery();
            int siguiente = rs.next() ? rs.getInt(1) + 1 : 1;
            return String.format("PRE-%d-%04d", anio, siguiente);
        } catch (SQLException e) {
            throw new RuntimeException("Error generando número de presupuesto", e);
        }
    }

    private List<LineaFactura> cargarLineas(Long presupuestoId) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT * FROM lineas_presupuesto WHERE presupuesto_id = ?")) {
            ps.setLong(1, presupuestoId);
            ResultSet rs = ps.executeQuery();
            List<LineaFactura> lineas = new ArrayList<>();
            while (rs.next()) {
                LineaFactura l = new LineaFactura();
                l.setId(rs.getLong("id"));
                productoRepository.findById(rs.getLong("producto_id")).ifPresent(l::setProducto);
                l.setCantidad(rs.getInt("cantidad"));
                l.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                l.setDescuento(rs.getBigDecimal("descuento"));
                lineas.add(l);
            }
            return lineas;
        }
    }

    private Presupuesto mapear(ResultSet rs) throws SQLException {
        Presupuesto p = new Presupuesto();
        p.setId(rs.getLong("id"));
        p.setNumero(rs.getString("numero"));
        clienteRepository.findById(rs.getLong("cliente_id")).ifPresent(p::setCliente);
        Date fecha = rs.getDate("fecha_emision");
        if (fecha != null) p.setFechaEmision(fecha.toLocalDate());
        p.setEstado(Presupuesto.Estado.valueOf(rs.getString("estado")));
        p.setObservaciones(rs.getString("observaciones"));
        p.setEmpresaId(rs.getLong("empresa_id"));
        if (rs.wasNull()) p.setEmpresaId(null);
        return p;
    }
}
