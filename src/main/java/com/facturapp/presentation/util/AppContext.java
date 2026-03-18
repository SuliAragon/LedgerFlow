package com.facturapp.presentation.util;

import com.facturapp.application.*;
import com.facturapp.infrastructure.pdf.PdfFacturaGenerator;
import com.facturapp.infrastructure.persistence.*;

/**
 * Contenedor de dependencias manual (sin framework de inyección).
 * Instancia y conecta todos los componentes de la aplicación.
 */
public class AppContext {

    private static AppContext instancia;

    // ---- Repositorios ----
    private final H2UsuarioRepository       usuarioRepository;
    private final H2ClienteRepository       clienteRepository;
    private final H2ProductoRepository      productoRepository;
    private final H2FacturaRepository       facturaRepository;
    private final H2PresupuestoRepository   presupuestoRepository;
    private final H2EmpresaLogoRepository   logoRepository;
    private final H2EmpresaConfigRepository configRepository;

    // ---- Casos de uso ----
    private final AuthUseCase          authUseCase;
    private final ClienteUseCase       clienteUseCase;
    private final ProductoUseCase      productoUseCase;
    private final FacturaUseCase       facturaUseCase;
    private final PresupuestoUseCase   presupuestoUseCase;
    private final LogoUseCase          logoUseCase;
    private final EmpresaConfigUseCase empresaConfigUseCase;

    private AppContext() {
        usuarioRepository    = new H2UsuarioRepository();
        clienteRepository    = new H2ClienteRepository();
        productoRepository   = new H2ProductoRepository();
        facturaRepository    = new H2FacturaRepository(clienteRepository, productoRepository);
        presupuestoRepository = new H2PresupuestoRepository(clienteRepository, productoRepository);
        logoRepository       = new H2EmpresaLogoRepository();
        configRepository     = new H2EmpresaConfigRepository();

        PdfFacturaGenerator pdfGenerator = new PdfFacturaGenerator(logoRepository, configRepository);

        authUseCase          = new AuthUseCase(usuarioRepository);
        clienteUseCase       = new ClienteUseCase(clienteRepository);
        productoUseCase      = new ProductoUseCase(productoRepository);
        facturaUseCase       = new FacturaUseCase(facturaRepository, clienteRepository,
                                                   productoRepository, pdfGenerator);
        presupuestoUseCase   = new PresupuestoUseCase(presupuestoRepository, clienteRepository, pdfGenerator);
        logoUseCase          = new LogoUseCase(logoRepository);
        empresaConfigUseCase = new EmpresaConfigUseCase(configRepository);
    }

    public static synchronized AppContext getInstance() {
        if (instancia == null) instancia = new AppContext();
        return instancia;
    }

    public AuthUseCase          getAuthUseCase()          { return authUseCase; }
    public ClienteUseCase       getClienteUseCase()       { return clienteUseCase; }
    public ProductoUseCase      getProductoUseCase()      { return productoUseCase; }
    public FacturaUseCase       getFacturaUseCase()       { return facturaUseCase; }
    public PresupuestoUseCase   getPresupuestoUseCase()   { return presupuestoUseCase; }
    public LogoUseCase          getLogoUseCase()          { return logoUseCase; }
    public EmpresaConfigUseCase getEmpresaConfigUseCase() { return empresaConfigUseCase; }
}
