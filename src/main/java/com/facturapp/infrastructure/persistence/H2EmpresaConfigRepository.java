package com.facturapp.infrastructure.persistence;

import com.facturapp.domain.model.EmpresaConfig;
import com.facturapp.infrastructure.config.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2EmpresaConfigRepository {

    private Connection getConn() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    public List<EmpresaConfig> listar() {
        String sql = "SELECT * FROM empresa_config ORDER BY nombre_empresa";
        try (Statement stmt = getConn().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            List<EmpresaConfig> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando empresas", e);
        }
    }

    public Optional<EmpresaConfig> findById(Long id) {
        String sql = "SELECT * FROM empresa_config WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando empresa por ID", e);
        }
    }

    public EmpresaConfig guardar(EmpresaConfig c) {
        if (c.getId() == null) {
            return insertar(c);
        } else {
            actualizar(c);
            return c;
        }
    }

    private EmpresaConfig insertar(EmpresaConfig c) {
        String sql = """
            INSERT INTO empresa_config
                (nombre_empresa, nif, nombre_emisor, cargo,
                 telefono, email, web, direccion, ciudad,
                 codigo_postal, provincia, cuenta_bancaria, notas_pie)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, c);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) c.setId(keys.getLong(1));
            return c;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando empresa", e);
        }
    }

    private void actualizar(EmpresaConfig c) {
        String sql = """
            UPDATE empresa_config SET
                nombre_empresa=?, nif=?, nombre_emisor=?, cargo=?,
                telefono=?, email=?, web=?, direccion=?, ciudad=?,
                codigo_postal=?, provincia=?, cuenta_bancaria=?, notas_pie=?
            WHERE id=?
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            setParams(ps, c);
            ps.setLong(14, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando empresa", e);
        }
    }

    public void eliminar(Long id) {
        String sql = "DELETE FROM empresa_config WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando empresa", e);
        }
    }

    private void setParams(PreparedStatement ps, EmpresaConfig c) throws SQLException {
        ps.setString(1,  c.getNombreEmpresa());
        ps.setString(2,  c.getNif());
        ps.setString(3,  c.getNombreEmisor());
        ps.setString(4,  c.getCargo());
        ps.setString(5,  c.getTelefono());
        ps.setString(6,  c.getEmail());
        ps.setString(7,  c.getWeb());
        ps.setString(8,  c.getDireccion());
        ps.setString(9,  c.getCiudad());
        ps.setString(10, c.getCodigoPostal());
        ps.setString(11, c.getProvincia());
        ps.setString(12, c.getCuentaBancaria());
        ps.setString(13, c.getNotasPie());
    }

    private EmpresaConfig mapear(ResultSet rs) throws SQLException {
        EmpresaConfig c = new EmpresaConfig();
        c.setId(rs.getLong("id"));
        c.setNombreEmpresa(rs.getString("nombre_empresa"));
        c.setNif(rs.getString("nif"));
        c.setNombreEmisor(rs.getString("nombre_emisor"));
        c.setCargo(rs.getString("cargo"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setWeb(rs.getString("web"));
        c.setDireccion(rs.getString("direccion"));
        c.setCiudad(rs.getString("ciudad"));
        c.setCodigoPostal(rs.getString("codigo_postal"));
        c.setProvincia(rs.getString("provincia"));
        c.setCuentaBancaria(rs.getString("cuenta_bancaria"));
        c.setNotasPie(rs.getString("notas_pie"));
        return c;
    }
}
