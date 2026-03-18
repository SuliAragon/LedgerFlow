package com.facturapp.infrastructure.pdf;

import com.facturapp.domain.model.*;
import com.facturapp.domain.repository.EmpresaLogoRepository;
import com.facturapp.infrastructure.persistence.H2EmpresaConfigRepository;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Genera facturas y presupuestos en PDF con iText 8.
 * Diseño moderno, compacto y elegante.
 */
public class PdfFacturaGenerator {

    private static final Logger log = Logger.getLogger(PdfFacturaGenerator.class.getName());

    // Paleta de colores
    private static final DeviceRgb AZUL_OSCURO  = new DeviceRgb(30,  41,  59);   // encabezados
    private static final DeviceRgb AZUL_MEDIO   = new DeviceRgb(59,  130, 246);  // acento
    private static final DeviceRgb GRIS_TEXTO   = new DeviceRgb(71,  85,  105);  // texto secundario
    private static final DeviceRgb GRIS_CLARO   = new DeviceRgb(248, 250, 252);  // fila alternada
    private static final DeviceRgb BORDE_SUAVE  = new DeviceRgb(226, 232, 240);  // bordes tabla
    private static final DeviceRgb BLANCO       = new DeviceRgb(255, 255, 255);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmpresaLogoRepository     logoRepo;
    private final H2EmpresaConfigRepository configRepo;

    public PdfFacturaGenerator(EmpresaLogoRepository logoRepo,
                                H2EmpresaConfigRepository configRepo) {
        this.logoRepo   = logoRepo;
        this.configRepo = configRepo;
    }

    // ── PÚBLICOS ─────────────────────────────────────────────────────────────

    public Path generar(Factura f) {
        EmpresaConfig cfg = f.getEmpresaId() != null
            ? configRepo.findById(f.getEmpresaId()).orElse(new EmpresaConfig())
            : new EmpresaConfig();
        generarDocumento("FACTURA", f.getNumero(), f.getFechaEmision(),
            f.getEstado().getEtiqueta(), f.getCliente(), f.getLineas(),
            f.getSubtotal(), f.getTotalIva(), f.getTotal(), f.getObservaciones(),
            cfg, logoRepo.findActivo().orElse(null), destino(f.getNumero()));
        return destino(f.getNumero());
    }

    public Path generarPresupuesto(Presupuesto p) {
        EmpresaConfig cfg = p.getEmpresaId() != null
            ? configRepo.findById(p.getEmpresaId()).orElse(new EmpresaConfig())
            : new EmpresaConfig();
        generarDocumento("PRESUPUESTO", p.getNumero(), p.getFechaEmision(),
            p.getEstado().getEtiqueta(), p.getCliente(), p.getLineas(),
            p.getSubtotal(), p.getTotalIva(), p.getTotal(), p.getObservaciones(),
            cfg, logoRepo.findActivo().orElse(null), destino(p.getNumero()));
        return destino(p.getNumero());
    }

    // ── CORE ─────────────────────────────────────────────────────────────────

    private void generarDocumento(String tipo, String numero, LocalDate fecha, String estadoLabel,
                                   Cliente cliente, List<LineaFactura> lineas,
                                   BigDecimal subtotal, BigDecimal totalIva, BigDecimal total,
                                   String observaciones, EmpresaConfig cfg,
                                   EmpresaLogo logo, Path destino) {
        try {
            Files.createDirectories(destino.getParent());
            Document doc = new Document(new PdfDocument(new PdfWriter(destino.toFile())), PageSize.A4);
            doc.setMargins(36, 44, 36, 44);

            cabecera(doc, tipo, numero, fecha, estadoLabel, cfg, logo);
            separador(doc, 8, 10);
            bloqueEmisorCliente(doc, cfg, cliente);
            separador(doc, 6, 10);
            tablaLineas(doc, lineas);
            totalesYNotas(doc, subtotal, totalIva, total, cfg, observaciones);
            pie(doc);

            doc.close();
            log.info("PDF → " + destino);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error generando PDF", e);
            throw new RuntimeException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    private Path destino(String numero) {
        return Paths.get(System.getProperty("user.home"), "Downloads",
            numero.replace("/", "-") + ".pdf");
    }

    // ── CABECERA ─────────────────────────────────────────────────────────────

    private void cabecera(Document doc, String tipo, String numero, LocalDate fecha,
                           String estadoLabel, EmpresaConfig cfg, EmpresaLogo logo) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
            .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);

        // Izquierda: logo o nombre empresa
        Cell izq = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        boolean logoOk = false;
        if (logo != null && logo.getRutaArchivo() != null) {
            try {
                if (new File(logo.getRutaArchivo()).exists()) {
                    Image img = new Image(ImageDataFactory.create(logo.getRutaArchivo()));
                    img.setMaxWidth(130).setAutoScaleHeight(true);
                    izq.add(img);
                    logoOk = true;
                }
            } catch (Exception e) { log.warning("Logo no cargado: " + e.getMessage()); }
        }
        if (!logoOk) {
            String nombre = cfg != null && cfg.tieneNombreEmpresa() ? cfg.getNombreEmpresa() : "LedgerFlow";
            izq.add(par(nombre, 18, AZUL_OSCURO).setBold());
        }

        // Derecha: tipo + número + metadatos
        Cell der = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
            .setVerticalAlignment(VerticalAlignment.MIDDLE);
        der.add(par(tipo, 20, AZUL_MEDIO).setBold().setMarginBottom(2));
        der.add(par(numero, 11, AZUL_OSCURO).setBold().setMarginBottom(1));
        if (fecha != null) der.add(par("Fecha: " + fecha.format(FMT), 8, GRIS_TEXTO));
        der.add(par("Estado: " + estadoLabel, 8, GRIS_TEXTO));

        t.addCell(izq);
        t.addCell(der);
        doc.add(t);
    }

    // ── BLOQUE EMISOR + CLIENTE ───────────────────────────────────────────────

    private void bloqueEmisorCliente(Document doc, EmpresaConfig c, Cliente cl) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{48, 4, 48}))
            .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);

        // Emisor
        Cell emisor = new Cell().setBorder(Border.NO_BORDER);
        boolean hayEmisor = c != null && (ok(c.getNombreEmpresa()) || ok(c.getNif()));
        if (hayEmisor) {
            emisor.add(etiquetaSeccion("DATOS DEL EMISOR"));
            if (ok(c.getNombreEmpresa())) emisor.add(par(c.getNombreEmpresa(), 9, AZUL_OSCURO).setBold());
            if (ok(c.getNif()))           emisor.add(par("NIF: " + c.getNif(), 8, GRIS_TEXTO));
            String dir = c.getDireccionCompleta();
            if (ok(dir)) emisor.add(par(dir, 8, GRIS_TEXTO));
            if (ok(c.getTelefono())) emisor.add(par(c.getTelefono(), 8, GRIS_TEXTO));
            if (ok(c.getEmail()))    emisor.add(par(c.getEmail(), 8, GRIS_TEXTO));
            if (ok(c.getWeb()))      emisor.add(par(c.getWeb(), 8, GRIS_TEXTO));
            if (ok(c.getNombreEmisor())) {
                String e = c.getNombreEmisor() + (ok(c.getCargo()) ? " · " + c.getCargo() : "");
                emisor.add(par(e, 8, GRIS_TEXTO));
            }
        }

        // Separador central (vacío)
        Cell sep = new Cell().setBorder(Border.NO_BORDER);

        // Cliente
        Cell cliente = new Cell().setBorder(Border.NO_BORDER);
        if (cl != null) {
            cliente.add(etiquetaSeccion("FACTURAR A"));
            cliente.add(par(cl.getNombre(), 9, AZUL_OSCURO).setBold());
            cliente.add(par("NIF/CIF: " + cl.getNifCif(), 8, GRIS_TEXTO));
            if (ok(cl.getDireccion())) cliente.add(par(cl.getDireccion(), 8, GRIS_TEXTO));
            if (ok(cl.getEmail()))     cliente.add(par(cl.getEmail(), 8, GRIS_TEXTO));
            if (ok(cl.getTelefono()))  cliente.add(par(cl.getTelefono(), 8, GRIS_TEXTO));
        }

        t.addCell(emisor);
        t.addCell(sep);
        t.addCell(cliente);
        doc.add(t);
    }

    // ── TABLA DE LÍNEAS ──────────────────────────────────────────────────────

    private void tablaLineas(Document doc, List<LineaFactura> lineas) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{38, 8, 14, 9, 9, 14, 8}))
            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(4);

        // Cabecera
        String[] cols = {"Descripción", "Cant.", "P. Unit.", "Dto.%", "IVA%", "Total", ""};
        for (int i = 0; i < cols.length; i++) {
            boolean esAccion = i == cols.length - 1;
            t.addHeaderCell(new Cell()
                .add(new Paragraph(cols[i]).setFontSize(7.5f).setBold()
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(i == 0 ? TextAlignment.LEFT : TextAlignment.CENTER))
                .setBackgroundColor(AZUL_OSCURO)
                .setPaddingTop(5).setPaddingBottom(5)
                .setPaddingLeft(i == 0 ? 6 : 3).setPaddingRight(3)
                .setBorder(Border.NO_BORDER));
        }

        // Filas
        boolean alt = false;
        for (LineaFactura l : lineas) {
            DeviceRgb bg = alt ? GRIS_CLARO : BLANCO;
            alt = !alt;
            int iva = l.getProducto() != null ? l.getProducto().getPorcentajeIva() : 0;
            String prod = l.getProducto() != null ? l.getProducto().getNombre() : "-";

            t.addCell(celdaFila(prod,                             TextAlignment.LEFT,   bg, true));
            t.addCell(celdaFila(String.valueOf(l.getCantidad()),  TextAlignment.CENTER, bg, false));
            t.addCell(celdaFila(eur(l.getPrecioUnitario()),       TextAlignment.RIGHT,  bg, false));
            t.addCell(celdaFila(l.getDescuento() + "%",          TextAlignment.CENTER, bg, false));
            t.addCell(celdaFila(iva + "%",                        TextAlignment.CENTER, bg, false));
            t.addCell(celdaFila(eur(l.getTotalLinea()),           TextAlignment.RIGHT,  bg, false));
            t.addCell(celdaFila("",                               TextAlignment.CENTER, bg, false));
        }

        doc.add(t);
    }

    // ── TOTALES Y NOTAS ──────────────────────────────────────────────────────

    private void totalesYNotas(Document doc, BigDecimal subtotal, BigDecimal totalIva,
                                BigDecimal total, EmpresaConfig cfg, String observaciones) {
        // Layout: notas a la izquierda, totales a la derecha
        Table wrap = new Table(UnitValue.createPercentArray(new float[]{52, 4, 44}))
            .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER).setMarginTop(8);

        // Columna izquierda: notas
        Cell notas = new Cell().setBorder(Border.NO_BORDER).setPaddingTop(4);
        if (cfg != null && ok(cfg.getCuentaBancaria())) {
            notas.add(par("Forma de pago", 7, GRIS_TEXTO).setBold());
            notas.add(par("Transferencia bancaria", 8, AZUL_OSCURO));
            notas.add(par(cfg.getCuentaBancaria(), 8, AZUL_OSCURO).setMarginBottom(6));
        }
        if (ok(observaciones)) {
            notas.add(par("Observaciones", 7, GRIS_TEXTO).setBold());
            notas.add(par(observaciones, 8, AZUL_OSCURO));
        }
        if (cfg != null && ok(cfg.getNotasPie())) {
            notas.add(par(cfg.getNotasPie(), 7.5f, GRIS_TEXTO).setMarginTop(4));
        }

        // Columna derecha: totales
        Table tot = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
            .setWidth(UnitValue.createPercentValue(100));

        filaTotal(tot, "Base imponible", eur(subtotal), false);
        filaTotal(tot, "IVA",            eur(totalIva), false);

        // Fila TOTAL destacada
        SolidBorder topLine = new SolidBorder(AZUL_MEDIO, 1.5f);
        tot.addCell(new Cell().add(par("TOTAL", 10, AZUL_OSCURO).setBold())
            .setPadding(6).setBorderTop(topLine).setBorderBottom(Border.NO_BORDER)
            .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER));
        tot.addCell(new Cell().add(par(eur(total), 10, AZUL_MEDIO).setBold()
            .setTextAlignment(TextAlignment.RIGHT))
            .setPadding(6).setBorderTop(topLine).setBorderBottom(Border.NO_BORDER)
            .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER));

        wrap.addCell(notas);
        wrap.addCell(new Cell().setBorder(Border.NO_BORDER)); // separador
        wrap.addCell(new Cell().setBorder(Border.NO_BORDER).add(tot));
        doc.add(wrap);
    }

    // ── PIE ──────────────────────────────────────────────────────────────────

    private void pie(Document doc) {
        doc.add(new LineSeparator(new SolidLine(0.4f)).setMarginTop(16).setMarginBottom(5)
            .setStrokeColor(BORDE_SUAVE));
        doc.add(par("Documento generado con LedgerFlow", 7, GRIS_TEXTO)
            .setTextAlignment(TextAlignment.CENTER));
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private Paragraph etiquetaSeccion(String txt) {
        return par(txt, 6.5f, AZUL_MEDIO).setBold()
            .setCharacterSpacing(0.5f).setMarginBottom(3);
    }

    private void separador(Document doc, float marginTop, float marginBottom) {
        doc.add(new LineSeparator(new SolidLine(0.5f))
            .setMarginTop(marginTop).setMarginBottom(marginBottom)
            .setStrokeColor(BORDE_SUAVE));
    }

    private Cell celdaFila(String txt, TextAlignment align, DeviceRgb bg, boolean izquierda) {
        return new Cell()
            .add(new Paragraph(txt).setFontSize(8.5f).setFontColor(AZUL_OSCURO).setTextAlignment(align))
            .setBackgroundColor(bg)
            .setPaddingTop(4).setPaddingBottom(4)
            .setPaddingLeft(izquierda ? 6 : 3).setPaddingRight(3)
            .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
            .setBorderTop(Border.NO_BORDER)
            .setBorderBottom(new SolidBorder(BORDE_SUAVE, 0.4f));
    }

    private void filaTotal(Table t, String label, String valor, boolean destacado) {
        SolidBorder borde = new SolidBorder(BORDE_SUAVE, 0.4f);
        t.addCell(new Cell().add(par(label, 8.5f, GRIS_TEXTO))
            .setPadding(4).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
            .setBorderTop(Border.NO_BORDER).setBorderBottom(borde));
        t.addCell(new Cell().add(par(valor, 8.5f, AZUL_OSCURO).setTextAlignment(TextAlignment.RIGHT))
            .setPadding(4).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
            .setBorderTop(Border.NO_BORDER).setBorderBottom(borde));
    }

    private Paragraph par(String txt, float size, DeviceRgb color) {
        return new Paragraph(txt).setFontSize(size).setFontColor(color).setMargin(0);
    }

    private boolean ok(String v) { return v != null && !v.isBlank(); }

    private String eur(BigDecimal v) {
        return v == null ? "0,00 €" : String.format("%,.2f €", v);
    }
}
