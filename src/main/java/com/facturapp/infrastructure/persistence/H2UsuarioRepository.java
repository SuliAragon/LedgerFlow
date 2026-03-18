package com.facturapp.infrastructure.persistence;

import com.facturapp.domain.model.Usuario;
import com.facturapp.domain.repository.UsuarioRepository;
import com.facturapp.infrastructure.config.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Implementación JDBC del repositorio de usuarios usando H2.
 */
public class H2UsuarioRepository implements UsuarioRepository {

    private static final Logger log = Logger.getLogger(H2UsuarioRepository.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    @Override
    public Usuario save(Usuario usuario) {
        if (usuario.getId() == null) {
            return insertar(usuario);
        } else {
            return actualizar(usuario);
        }
    }

    private Usuario insertar(Usuario usuario) {
        String sql = "INSERT INTO usuarios (username, password_hash, email, rol, activo) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getUsername());
            ps.setString(2, usuario.getPasswordHash());
            ps.setString(3, usuario.getEmail());
            ps.setString(4, usuario.getRol().name());
            ps.setBoolean(5, usuario.isActivo());
            ps.executeUpdate();

            var keys = ps.getGeneratedKeys();
            if (keys.next()) {
                usuario.setId(keys.getLong(1));
            }
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando usuario", e);
        }
    }

    private Usuario actualizar(Usuario usuario) {
        String sql = "UPDATE usuarios SET username=?, password_hash=?, email=?, rol=?, activo=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, usuario.getUsername());
            ps.setString(2, usuario.getPasswordHash());
            ps.setString(3, usuario.getEmail());
            ps.setString(4, usuario.getRol().name());
            ps.setBoolean(5, usuario.isActivo());
            ps.setLong(6, usuario.getId());
            ps.executeUpdate();
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando usuario", e);
        }
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando usuario por ID", e);
        }
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        String sql = "SELECT * FROM usuarios WHERE username = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando usuario por username", e);
        }
    }

    @Override
    public List<Usuario> findAll() {
        String sql = "SELECT * FROM usuarios ORDER BY username";
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            List<Usuario> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando usuarios", e);
        }
    }

    @Override
    public void delete(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando usuario", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE username = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error verificando username", e);
        }
    }

    /** Mapea un ResultSet a la entidad Usuario */
    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setEmail(rs.getString("email"));
        u.setRol(Usuario.Rol.valueOf(rs.getString("rol")));
        u.setActivo(rs.getBoolean("activo"));
        var ts = rs.getTimestamp("fecha_creacion");
        if (ts != null) u.setFechaCreacion(ts.toLocalDateTime());
        return u;
    }
}
