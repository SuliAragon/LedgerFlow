package com.facturapp.infrastructure.pdf;

import com.facturapp.domain.model.EmpresaConfig;
import com.facturapp.domain.model.EmpresaLogo;
import com.facturapp.domain.model.Factura;
import com.facturapp.domain.model.LineaFactura;
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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Genera facturas en PDF con iText 8.
 * Incluye logo de empresa y datos del emisor si están configurados.
 */
public class PdfFacturaGenerator {

    private static final Logger log = Logger.getLogger(PdfFacturaGenerator.class.getName());

    private static final DeviceRgb PRIMARIO   = new DeviceRgb(30, 41, 59);
    private static final DeviceRgb SECUNDARIO = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb ACENTO     = new DeviceRgb(59, 130, 246);
    private static final DeviceRgb FONDO_FILA = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb BORDE      = new DeviceRgb(226, 232, 240);
    private static final DeviceRgb BLANCO     = new DeviceRgb(255, 255, 255);

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmpresaLogoRepository    logoRepo;
    private final H2EmpresaConfigRepository configRepo;

    public PdfFacturaGenerator(EmpresaLogoRepository logoRepo,
                               H2EmpresaConfigRepository configRepo) {
        this.logoRepo  = logoRepo;
        this.configRepo = configRepo;
    }

    public Path generar(Factura factura) {
        Path destino = Paths.get(System.getProperty("user.home"), "Downloads",
            factura.getNumero().replace("/", "-") + ".pdf");
        try {
            Files.createDirectories(destino.getParent());

            EmpresaConfig config = (factura.getEmpresaId() != null)
                ? configRepo.findById(factura.getEmpresaId()).orElse(new EmpresaConfig())
                : new EmpresaConfig();
            Optional<EmpresaLogo> logo = logoRepo.findActivo();

            Document doc = new Document(new PdfDocument(new PdfWriter(destino.toFile())), PageSize.A4);
            doc.setMargins(40, 40, 40, 40);

            seccionCabecera(doc, factura, config, logo.orElse(null));
            seccionEmisor(doc, config);
            seccionCliente(doc, factura);
            seccionLineas(doc, factura);
            seccionTotales(doc, factura, config);
            seccionPie(doc);

            doc.close();
            log.info("PDF → " + destino);
            return destino;

        } catch (IOException e) {
            log.log(Level.SEVERE, "Error generando PDF", e);
            throw new RuntimeException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    // ── CABECERA ─────────────────────────────────────────────────────────────

    private void seccionCabecera(Document doc, Factura factura,
                                  EmpresaConfig config, EmpresaLogo logo) {
        Table tbl = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
            .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);

        // Columna izquierda: logo + nombre empresa
        Cell izq = new Cell().setBorder(Border.NO_BORDER);
        boolean logoMostrado = false;
        if (logo != null && logo.getRutaArchivo() != null) {
            try {
                File f = new File(logo.getRutaArchivo());
                if (f.exists()) {
                    Image img = new Image(ImageDataFactory.create(logo.getRutaArchivo()));
                    img.setMaxWidth(170).setAutoScaleHeight(true);
                    izq.add(img);
                    logoMostrado = true;
                }
            } catch (Exception e) {
                log.warning("Logo no cargado: " + e.getMessage());
            }
        }
        // Nombre empresa (solo si no hay logo, o siempre si está relleno)
        String nombre = (config != null && config.tieneNombreEmpresa())
            ? config.getNombreEmpresa() : "FacturApp";
        if (!logoMostrado) {
            izq.add(new Paragraph(nombre).setFontSize(22).setBold().setFontColor(PRIMARIO));
        }

        // Columna derecha: datos de la factura
        Cell der = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        der.add(new Paragraph("FACTURA").setFontSize(24).setBold().setFontColor(ACENTO));
        der.add(new Paragraph(factura.getNumero()).setFontSize(15).setBold().setFontColor(PRIMARIO));
        if (factura.getFechaEmision() != null)
            der.add(par("Fecha: " + factura.getFechaEmision().format(FMT_FECHA), 10, SECUNDARIO));
        der.add(par("Estado: " + factura.getEstado().getEtiqueta(), 10, SECUNDARIO));

        tbl.addCell(izq);
        tbl.addCell(der);
        doc.add(tbl);
        doc.add(new LineSeparator(new SolidLine()).setMarginTop(12).setMarginBottom(14));
    }

    // ── DATOS EMISOR ─────────────────────────────────────────────────────────

    private void seccionEmisor(Document doc, EmpresaConfig c) {
        if (c == null) return;
        boolean tieneAlgo = c.getNombreEmpresa() != null || c.getNif() != null
            || c.getTelefono() != null || c.getEmail() != null
            || c.getDireccion() != null || c.getNombreEmisor() != null;
        if (!tieneAlgo) return;

        Table tbl = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
            .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER)
            .setMarginBottom(8);

        Cell emisor = new Cell().setBorder(Border.NO_BORDER);
        emisor.add(par("DATOS DEL EMISOR", 8, SECUNDARIO).setBold());
        if (ok(c.getNombreEmpresa()))
            emisor.add(par(c.getNombreEmpresa(), 11, PRIMARIO).setBold());
        if (ok(c.getNif()))
            emisor.add(par("NIF/CIF: " + c.getNif(), 10, PRIMARIO));
        String dir = c.getDireccionCompleta();
        if (ok(dir)) emisor.add(par(dir, 10, PRIMARIO));
        if (ok(c.getTelefono())) emisor.add(par("Tel: " + c.getTelefono(), 10, SECUNDARIO));
        if (ok(c.getEmail()))    emisor.add(par(c.getEmail(), 10, SECUNDARIO));
        if (ok(c.getWeb()))      emisor.add(par(c.getWeb(), 10, SECUNDARIO));
        if (ok(c.getNombreEmisor())) {
            String emisorTxt = c.getNombreEmisor();
            if (ok(c.getCargo())) emisorTxt += " · " + c.getCargo();
            emisor.add(par(emisorTxt, 10, SECUNDARIO));
        }

        tbl.addCell(emisor);
        tbl.addCell(new Cell().setBorder(Border.NO_BORDER));
        doc.add(tbl);
    }

    // ── DATOS CLIENTE ────────────────────────────────────────────────────────

    private void seccionCliente(Document doc, Factura factura) {
        if (factura.getCliente() == null) return;
        var c = factura.getCliente();

        doc.add(par("FACTURAR A:", 9, SECUNDARIO).setBold().setMarginBottom(4));
        doc.add(par(c.getNombre(), 13, PRIMARIO).setBold().setMarginBottom(2));
        doc.add(par("NIF/CIF: " + c.getNifCif(), 10, PRIMARIO).setMarginBottom(2));
        if (ok(c.getDireccion())) doc.add(par(c.getDireccion(), 10, PRIMARIO).setMarginBottom(2));
        if (ok(c.getEmail()))     doc.add(par(c.getEmail(), 10, SECUNDARIO).setMarginBottom(2));
        if (ok(c.getTelefono()))  doc.add(par(c.getTelefono(), 10, SECUNDARIO));
        doc.add(new Paragraph("\n"));
    }

    // ── TABLA DE LÍNEAS ──────────────────────────────────────────────────────

    private void seccionLineas(Document doc, Factura factura) {
        Table tbl = new Table(UnitValue.createPercentArray(new float[]{38, 10, 15, 10, 12, 15}))
            .setWidth(UnitValue.createPercentValue(100));

        for (String cab : new String[]{"Descripción","Cant.","Precio Unit.","Dto.%","IVA%","Total"})
            tbl.addHeaderCell(new Cell()
                .add(new Paragraph(cab).setFontSize(10).setBold().setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(PRIMARIO).setPadding(8).setBorder(Border.NO_BORDER));

        boolean alt = false;
        for (LineaFactura l : factura.getLineas()) {
            DeviceRgb bg = alt ? FONDO_FILA : BLANCO; alt = !alt;
            String prod = l.getProducto() != null ? l.getProducto().getNombre() : "-";
            int iva     = l.getProducto() != null ? l.getProducto().getPorcentajeIva() : 0;
            tbl.addCell(celda(prod,                          TextAlignment.LEFT,   bg));
            tbl.addCell(celda(String.valueOf(l.getCantidad()), TextAlignment.CENTER, bg));
            tbl.addCell(celda(eur(l.getPrecioUnitario()),    TextAlignment.RIGHT,  bg));
            tbl.addCell(celda(l.getDescuento() + "%",        TextAlignment.CENTER, bg));
            tbl.addCell(celda(iva + "%",                     TextAlignment.CENTER, bg));
            tbl.addCell(celda(eur(l.getTotalLinea()),        TextAlignment.RIGHT,  bg));
        }
        doc.add(tbl);
    }

    // ── TOTALES ──────────────────────────────────────────────────────────────

    private void seccionTotales(Document doc, Factura factura, EmpresaConfig config) {
        Table wrap = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
            .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER).setMarginTop(12);
        wrap.addCell(new Cell().setBorder(Border.NO_BORDER));

        Table tot = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
            .setWidth(UnitValue.createPercentValue(100));
        filaTotal(tot, "Base imponible", eur(factura.getSubtotal()));
        filaTotal(tot, "IVA total",       eur(factura.getTotalIva()));
        tot.addCell(new Cell().add(new Paragraph("TOTAL").setFontSize(13).setBold()
            .setFontColor(ColorConstants.WHITE)).setBackgroundColor(PRIMARIO)
            .setPadding(10).setBorder(Border.NO_BORDER));
        tot.addCell(new Cell().add(new Paragraph(eur(factura.getTotal())).setFontSize(13).setBold()
            .setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.RIGHT))
            .setBackgroundColor(PRIMARIO).setPadding(10).setBorder(Border.NO_BORDER));
        wrap.addCell(new Cell().setBorder(Border.NO_BORDER).add(tot));
        doc.add(wrap);

        // Cuenta bancaria si está configurada
        if (config != null && ok(config.getCuentaBancaria())) {
            doc.add(par("\nForma de pago — Transferencia bancaria: " + config.getCuentaBancaria(),
                9, SECUNDARIO));
        }
        // Observaciones de la factura
        if (ok(factura.getObservaciones())) {
            doc.add(par("\nObservaciones:", 10, PRIMARIO).setBold());
            doc.add(par(factura.getObservaciones(), 10, SECUNDARIO));
        }
        // Notas al pie de empresa
        if (config != null && ok(config.getNotasPie())) {
            doc.add(par("\n" + config.getNotasPie(), 9, SECUNDARIO));
        }
    }

    // ── PIE ──────────────────────────────────────────────────────────────────

    private void seccionPie(Document doc) {
        doc.add(new LineSeparator(new SolidLine()).setMarginTop(20).setMarginBottom(8));
        doc.add(new Paragraph("Documento generado con FacturApp")
            .setFontSize(8).setFontColor(SECUNDARIO).setTextAlignment(TextAlignment.CENTER));
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private Cell celda(String txt, TextAlignment align, DeviceRgb bg) {
        return new Cell()
            .add(new Paragraph(txt).setFontSize(10).setFontColor(PRIMARIO).setTextAlignment(align))
            .setBackgroundColor(bg).setPadding(7)
            .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
            .setBorderTop(Border.NO_BORDER).setBorderBottom(new SolidBorder(BORDE, 0.5f));
    }

    private void filaTotal(Table t, String label, String valor) {
        SolidBorder borde = new SolidBorder(BORDE, 0.5f);
        t.addCell(new Cell().add(new Paragraph(label).setFontSize(10).setBold().setFontColor(PRIMARIO))
            .setPadding(7).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
            .setBorderTop(Border.NO_BORDER).setBorderBottom(borde));
        t.addCell(new Cell().add(new Paragraph(valor).setFontSize(10).setFontColor(PRIMARIO)
            .setTextAlignment(TextAlignment.RIGHT))
            .setPadding(7).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
            .setBorderTop(Border.NO_BORDER).setBorderBottom(borde));
    }

    private Paragraph par(String txt, float size, DeviceRgb color) {
        return new Paragraph(txt).setFontSize(size).setFontColor(color);
    }

    private boolean ok(String v) { return v != null && !v.isBlank(); }

    private String eur(BigDecimal v) {
        return v == null ? "0,00 €" : String.format("%,.2f €", v);
    }
}
