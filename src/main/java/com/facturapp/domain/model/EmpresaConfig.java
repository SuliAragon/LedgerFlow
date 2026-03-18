package com.facturapp.domain.model;

/**
 * Configuración de la empresa emisora de facturas.
 * Solo existe un registro (singleton) en la base de datos.
 * Todos los campos son opcionales.
 */
public class EmpresaConfig {

    private Long id;
    private String nombreEmpresa;
    private String nif;
    private String nombreEmisor;    // Persona de contacto
    private String cargo;           // Cargo del emisor
    private String telefono;
    private String email;
    private String web;
    private String direccion;
    private String ciudad;
    private String codigoPostal;
    private String provincia;
    private String cuentaBancaria;  // IBAN
    private String notasPie;        // Texto libre al pie de la factura

    public EmpresaConfig() {
    }

    // ---- Getters y Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String v) { this.nombreEmpresa = v; }

    public String getNif() { return nif; }
    public void setNif(String v) { this.nif = v; }

    public String getNombreEmisor() { return nombreEmisor; }
    public void setNombreEmisor(String v) { this.nombreEmisor = v; }

    public String getCargo() { return cargo; }
    public void setCargo(String v) { this.cargo = v; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String v) { this.telefono = v; }

    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }

    public String getWeb() { return web; }
    public void setWeb(String v) { this.web = v; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String v) { this.direccion = v; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String v) { this.ciudad = v; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String v) { this.codigoPostal = v; }

    public String getProvincia() { return provincia; }
    public void setProvincia(String v) { this.provincia = v; }

    public String getCuentaBancaria() { return cuentaBancaria; }
    public void setCuentaBancaria(String v) { this.cuentaBancaria = v; }

    public String getNotasPie() { return notasPie; }
    public void setNotasPie(String v) { this.notasPie = v; }

    /** Devuelve la dirección completa formateada en una línea */
    public String getDireccionCompleta() {
        StringBuilder sb = new StringBuilder();
        if (str(direccion)) sb.append(direccion);
        if (str(codigoPostal)) { if (sb.length()>0) sb.append(", "); sb.append(codigoPostal); }
        if (str(ciudad))       { if (sb.length()>0) sb.append(" "); sb.append(ciudad); }
        if (str(provincia))    { if (sb.length()>0) sb.append(", "); sb.append(provincia); }
        return sb.toString();
    }

    /** true si el campo tiene contenido útil */
    public boolean tieneNombreEmpresa() { return str(nombreEmpresa); }

    private boolean str(String v) { return v != null && !v.isBlank(); }

    @Override
    public String toString() {
        return nombreEmpresa != null && !nombreEmpresa.isBlank() ? nombreEmpresa : "(Sin nombre)";
    }
}
