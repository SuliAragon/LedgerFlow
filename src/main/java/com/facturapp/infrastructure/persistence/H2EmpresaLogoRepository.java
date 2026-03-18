package com.facturapp.infrastructure.persistence;

import com.facturapp.domain.model.EmpresaLogo;
import com.facturapp.domain.repository.EmpresaLogoRepository;
import com.facturapp.infrastructure.config.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del repositorio de logos usando H2.
 */
public class H2EmpresaLogoRepository implements EmpresaLogoRepository {

    private Connection getConn() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    @Override
    public EmpresaLogo save(EmpresaLogo logo) {
        return logo.getId() == null ? insertar(logo) : actualizar(logo);
    }

    private EmpresaLogo insertar(EmpresaLogo logo) {
        String sql = "INSERT INTO empresa_logos (nombre, ruta_archivo, fecha_subida, activo) VALUES (?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, logo.getNombre());
            ps.setString(2, logo.getRutaArchivo());
            ps.setDate(3, Date.valueOf(logo.getFechaSubida() != null ? logo.getFechaSubida() : java.time.LocalDate.now()));
            ps.setBoolean(4, logo.isActivo());
            ps.executeUpdate();
            var keys = ps.getGeneratedKeys();
            if (keys.next()) logo.setId(keys.getLong(1));
            return logo;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando logo", e);
        }
    }

    private EmpresaLogo actualizar(EmpresaLogo logo) {
        String sql = "UPDATE empresa_logos SET nombre=?, ruta_archivo=?, activo=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, logo.getNombre());
            ps.setString(2, logo.getRutaArchivo());
            ps.setBoolean(3, logo.isActivo());
            ps.setLong(4, logo.getId());
            ps.executeUpdate();
            return logo;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando logo", e);
        }
    }

    @Override
    public Optional<EmpresaLogo> findById(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM empresa_logos WHERE id=?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando logo por ID", e);
        }
    }

    @Override
    public List<EmpresaLogo> findAll() {
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM empresa_logos ORDER BY fecha_subida DESC");
            List<EmpresaLogo> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando logos", e);
        }
    }

    @Override
    public void delete(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM empresa_logos WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando logo", e);
        }
    }

    @Override
    public Optional<EmpresaLogo> findActivo() {
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM empresa_logos WHERE activo = TRUE LIMIT 1");
            if (rs.next()) return Optional.of(mapear(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando logo activo", e);
        }
    }

    @Override
    public void setActivo(Long id) {
        // Primero desactivar todos, luego activar el elegido
        try (Statement stmt = getConn().createStatement()) {
            stmt.execute("UPDATE empresa_logos SET activo = FALSE");
        } catch (SQLException e) {
            throw new RuntimeException("Error desactivando logos", e);
        }
        try (PreparedStatement ps = getConn().prepareStatement(
                "UPDATE empresa_logos SET activo = TRUE WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error activando logo", e);
        }
    }

    private EmpresaLogo mapear(ResultSet rs) throws SQLException {
        EmpresaLogo l = new EmpresaLogo();
        l.setId(rs.getLong("id"));
        l.setNombre(rs.getString("nombre"));
        l.setRutaArchivo(rs.getString("ruta_archivo"));
        Date fecha = rs.getDate("fecha_subida");
        if (fecha != null) l.setFechaSubida(fecha.toLocalDate());
        l.setActivo(rs.getBoolean("activo"));
        return l;
    }
}
