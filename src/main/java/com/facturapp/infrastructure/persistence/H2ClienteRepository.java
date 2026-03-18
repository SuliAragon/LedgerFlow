package com.facturapp.infrastructure.persistence;

import com.facturapp.domain.model.Cliente;
import com.facturapp.domain.repository.ClienteRepository;
import com.facturapp.infrastructure.config.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del repositorio de clientes usando H2.
 */
public class H2ClienteRepository implements ClienteRepository {

    private Connection getConn() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    @Override
    public Cliente save(Cliente cliente) {
        return cliente.getId() == null ? insertar(cliente) : actualizar(cliente);
    }

    private Cliente insertar(Cliente cliente) {
        String sql = "INSERT INTO clientes (nombre, nif_cif, email, telefono, direccion, fecha_creacion) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getNifCif());
            ps.setString(3, cliente.getEmail());
            ps.setString(4, cliente.getTelefono());
            ps.setString(5, cliente.getDireccion());
            ps.setDate(6, Date.valueOf(cliente.getFechaCreacion() != null ?
                cliente.getFechaCreacion() : java.time.LocalDate.now()));
            ps.executeUpdate();
            var keys = ps.getGeneratedKeys();
            if (keys.next()) cliente.setId(keys.getLong(1));
            return cliente;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando cliente", e);
        }
    }

    private Cliente actualizar(Cliente cliente) {
        String sql = "UPDATE clientes SET nombre=?, nif_cif=?, email=?, telefono=?, direccion=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getNifCif());
            ps.setString(3, cliente.getEmail());
            ps.setString(4, cliente.getTelefono());
            ps.setString(5, cliente.getDireccion());
            ps.setLong(6, cliente.getId());
            ps.executeUpdate();
            return cliente;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando cliente", e);
        }
    }

    @Override
    public Optional<Cliente> findById(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM clientes WHERE id = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando cliente por ID", e);
        }
    }

    @Override
    public List<Cliente> findAll() {
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM clientes ORDER BY nombre");
            List<Cliente> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando clientes", e);
        }
    }

    @Override
    public List<Cliente> findByNombre(String nombre) {
        String sql = "SELECT * FROM clientes WHERE LOWER(nombre) LIKE LOWER(?) ORDER BY nombre";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, "%" + nombre + "%");
            ResultSet rs = ps.executeQuery();
            List<Cliente> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando clientes por nombre", e);
        }
    }

    @Override
    public void delete(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM clientes WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando cliente", e);
        }
    }

    @Override
    public boolean existsByNifCif(String nifCif) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT COUNT(*) FROM clientes WHERE UPPER(nif_cif) = UPPER(?)")) {
            ps.setString(1, nifCif);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error verificando NIF/CIF", e);
        }
    }

    @Override
    public long count() {
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM clientes");
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error contando clientes", e);
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getLong("id"));
        c.setNombre(rs.getString("nombre"));
        c.setNifCif(rs.getString("nif_cif"));
        c.setEmail(rs.getString("email"));
        c.setTelefono(rs.getString("telefono"));
        c.setDireccion(rs.getString("direccion"));
        Date fecha = rs.getDate("fecha_creacion");
        if (fecha != null) c.setFechaCreacion(fecha.toLocalDate());
        return c;
    }
}
