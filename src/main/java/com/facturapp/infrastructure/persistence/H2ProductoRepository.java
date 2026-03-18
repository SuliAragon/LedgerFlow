package com.facturapp.infrastructure.persistence;

import com.facturapp.domain.model.Producto;
import com.facturapp.domain.repository.ProductoRepository;
import com.facturapp.infrastructure.config.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del repositorio de productos usando H2.
 */
public class H2ProductoRepository implements ProductoRepository {

    private Connection getConn() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    @Override
    public Producto save(Producto producto) {
        return producto.getId() == null ? insertar(producto) : actualizar(producto);
    }

    private Producto insertar(Producto producto) {
        String sql = "INSERT INTO productos (nombre, descripcion, precio, porcentaje_iva, activo) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, producto.getNombre());
            ps.setString(2, producto.getDescripcion());
            ps.setBigDecimal(3, producto.getPrecio());
            ps.setInt(4, producto.getPorcentajeIva());
            ps.setBoolean(5, producto.isActivo());
            ps.executeUpdate();
            var keys = ps.getGeneratedKeys();
            if (keys.next()) producto.setId(keys.getLong(1));
            return producto;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando producto", e);
        }
    }

    private Producto actualizar(Producto producto) {
        String sql = "UPDATE productos SET nombre=?, descripcion=?, precio=?, porcentaje_iva=?, activo=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, producto.getNombre());
            ps.setString(2, producto.getDescripcion());
            ps.setBigDecimal(3, producto.getPrecio());
            ps.setInt(4, producto.getPorcentajeIva());
            ps.setBoolean(5, producto.isActivo());
            ps.setLong(6, producto.getId());
            ps.executeUpdate();
            return producto;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando producto", e);
        }
    }

    @Override
    public Optional<Producto> findById(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM productos WHERE id = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando producto por ID", e);
        }
    }

    @Override
    public List<Producto> findAll() {
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM productos ORDER BY nombre");
            List<Producto> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando productos", e);
        }
    }

    @Override
    public List<Producto> findActivos() {
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM productos WHERE activo = TRUE ORDER BY nombre");
            List<Producto> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando productos activos", e);
        }
    }

    @Override
    public List<Producto> findByNombre(String nombre) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT * FROM productos WHERE LOWER(nombre) LIKE LOWER(?) ORDER BY nombre")) {
            ps.setString(1, "%" + nombre + "%");
            ResultSet rs = ps.executeQuery();
            List<Producto> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando productos por nombre", e);
        }
    }

    @Override
    public void delete(Long id) {
        // Verificar si el producto está referenciado en líneas de factura
        String checkSql = "SELECT COUNT(*) FROM lineas_factura WHERE producto_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(checkSql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                // No se puede eliminar físicamente → desactivar (borrado lógico)
                try (PreparedStatement upd = getConn().prepareStatement(
                        "UPDATE productos SET activo = FALSE WHERE id = ?")) {
                    upd.setLong(1, id);
                    upd.executeUpdate();
                }
                throw new RuntimeException("DEACTIVATED"); // señal especial al Use Case
            }
        } catch (RuntimeException re) {
            throw re; // relanzar señal especial
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando producto", e);
        }

        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM productos WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando producto", e);
        }
    }

    @Override
    public long count() {
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM productos");
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error contando productos", e);
        }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getLong("id"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        BigDecimal precio = rs.getBigDecimal("precio");
        p.setPrecio(precio != null ? precio : BigDecimal.ZERO);
        p.setPorcentajeIva(rs.getInt("porcentaje_iva"));
        p.setActivo(rs.getBoolean("activo"));
        return p;
    }
}
