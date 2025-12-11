package com.altoque.altoque.Service.Comprobante;

import com.altoque.altoque.Dto.Comprobante.ComprobanteBaseDto;
import com.altoque.altoque.Dto.Comprobante.ComprobanteItemDto;
import com.altoque.altoque.Entity.Cliente;
import com.altoque.altoque.Entity.Pago;
import com.altoque.altoque.Entity.Prestamo;
import com.altoque.altoque.Repository.PagoRepository;
import com.altoque.altoque.Utils.NumeroALetrasUtil;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Service
public class PdfComprobanteService {

    private static final Font FONT_TITULO = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font FONT_NORMAL = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font FONT_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font FONT_FOOTER = new Font(Font.HELVETICA, 7, Font.NORMAL);

    // DATOS DE LA EMPRESA (Hardcoded o traer de configuración)
    private static final String EMISOR_RUC = "20601234567";
    private static final String EMISOR_RAZON = "INVERSIONES AL TOQUE S.A.C.";
    private static final String EMISOR_DIRECCION = "Av. Larco 123, Trujillo, Perú";

    private final PagoRepository pagoRepository;

    public PdfComprobanteService(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    // ================== API PÚBLICA ==================

    @Transactional(readOnly = true)
    public byte[] generarFacturaPorPago(Integer idPago) {
        ComprobanteBaseDto dto = construirDesdePago(idPago, true);
        return generarPdf(dto);
    }

    @Transactional(readOnly = true)
    public byte[] generarBoletaPorPago(Integer idPago) {
        ComprobanteBaseDto dto = construirDesdePago(idPago, false);
        return generarPdf(dto);
    }

    // ================== Lógica de Construcción ==================

    private ComprobanteBaseDto construirDesdePago(Integer idPago, boolean esFactura) {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + idPago));

        Prestamo prestamo = pago.getPrestamo();
        Cliente cliente = prestamo.getCliente();

        // Cálculos financieros seguros
        BigDecimal total = BigDecimal.valueOf(pago.getMonto()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.valueOf(1.18);
        BigDecimal gravada = total.divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(gravada);

        ComprobanteBaseDto dto = new ComprobanteBaseDto();

        // Datos Emisor
        dto.setRucEmisor(EMISOR_RUC);
        dto.setRazonSocialEmisor(EMISOR_RAZON);
        dto.setDireccionEmisor(EMISOR_DIRECCION);

        // Datos Generales
        dto.setMoneda("PEN");
        dto.setTipoMonedaTexto("SOLES");
        dto.setFechaEmision(pago.getFechaPago() != null ? pago.getFechaPago().toLocalDate() : java.time.LocalDate.now());
        dto.setFormaPago("CONTADO");
        dto.setNumero(String.format("%08d", pago.getIdPago()));

        // Lógica Cliente (Manejo de Nulos)
        if (esFactura) {
            dto.setTipoComprobante("FACTURA ELECTRÓNICA");
            dto.setSerie("F001");
            dto.setTipoDocCliente("6"); // RUC
            dto.setNumeroDocCliente(cliente.getRuc() != null ? cliente.getRuc() : "SIN RUC");
            dto.setNombreCliente(cliente.getRazonSocial() != null ? cliente.getRazonSocial() : "CLIENTE SIN RAZÓN SOCIAL");

            String dir = cliente.getDireccionFiscal() != null ? cliente.getDireccionFiscal() : cliente.getDireccionCliente();
            dto.setDireccionCliente(dir != null ? dir : "Dirección no registrada");
        } else {
            dto.setTipoComprobante("BOLETA DE VENTA ELECTRÓNICA");
            dto.setSerie("B001");
            dto.setTipoDocCliente("1"); // DNI
            dto.setNumeroDocCliente(cliente.getDniCliente() != null ? cliente.getDniCliente() : "00000000");

            String nombreCompleto = (cliente.getNombreCliente() != null ? cliente.getNombreCliente() : "") + " " +
                    (cliente.getApellidoCliente() != null ? cliente.getApellidoCliente() : "");
            dto.setNombreCliente(nombreCompleto.trim().isEmpty() ? "CLIENTE GENERAL" : nombreCompleto.trim());
            dto.setDireccionCliente(cliente.getDireccionCliente() != null ? cliente.getDireccionCliente() : "Trujillo, Perú");
        }

        dto.setObservacion("PAGO DE CUOTA - PRÉSTAMO N° " + prestamo.getIdPrestamo());

        // Asignación de Montos
        dto.setOpGravadas(gravada);
        dto.setOpExoneradas(BigDecimal.ZERO);
        dto.setOpInafectas(BigDecimal.ZERO);
        dto.setDescuentoGlobal(BigDecimal.ZERO);
        dto.setOtrosCargos(BigDecimal.valueOf(pago.getAjusteRedondeo() != null ? pago.getAjusteRedondeo() : 0.0));
        dto.setIgvTotal(igv);
        dto.setImporteTotal(total);

        // Conversión a Letras (Usando la nueva utilidad)
        dto.setLeyendaImporteEnLetras(NumeroALetrasUtil.aLetras(total, "SOLES"));

        dto.setNotaRepresentacion(
                "Representación impresa de la " + dto.getTipoComprobante().toLowerCase() +
                        ". Puede ser consultada en nuestro portal web."
        );

        // Detalle del Item
        ComprobanteItemDto item = new ComprobanteItemDto();
        item.setCantidad(BigDecimal.ONE);
        item.setUnidadMedida("ZZ"); // Unidad de servicio
        item.setDescripcion("SERVICIO DE PAGO DE CRÉDITO");
        item.setValorUnitario(gravada);
        item.setImporteTotal(total);

        dto.setItems(Collections.singletonList(item));

        return dto;
    }

    // ================== Generación PDF (iText / OpenPDF) ==================

    private byte[] generarPdf(ComprobanteBaseDto comp) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);

            document.open();
            agregarEncabezado(document, comp);
            agregarDatosGenerales(document, comp);
            agregarTablaItems(document, comp);
            agregarTotales(document, comp);
            agregarNotaRepresentacion(document, comp);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace(); // Log del error real
            throw new RuntimeException("Error interno generando el PDF del comprobante", e);
        }
    }

    // ... (Mantén tus métodos privados auxiliares: agregarEncabezado, etc. tal como los tenías) ...
    // Solo asegúrate que 'celdaValor' maneje nulos, ejemplo:

    private void agregarEncabezado(Document document, ComprobanteBaseDto comp) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{60f, 40f});
        table.setWidthPercentage(100);

        PdfPTable emisorTable = new PdfPTable(1);
        emisorTable.addCell(celdaSinBorde(comp.getRazonSocialEmisor(), FONT_BOLD));
        emisorTable.addCell(celdaSinBorde("RUC: " + comp.getRucEmisor(), FONT_NORMAL));
        emisorTable.addCell(celdaSinBorde(comp.getDireccionEmisor(), FONT_NORMAL));

        PdfPCell left = new PdfPCell(emisorTable);
        left.setBorder(Rectangle.NO_BORDER);

        PdfPTable tipoTable = new PdfPTable(1);
        PdfPCell rucCell = new PdfPCell(new Phrase("RUC: " + comp.getRucEmisor(), FONT_BOLD));
        rucCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tipoTable.addCell(rucCell);

        PdfPCell tipoCell = new PdfPCell(new Phrase(comp.getTipoComprobante(), FONT_TITULO));
        tipoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tipoCell.setPaddingBottom(5);
        tipoTable.addCell(tipoCell);

        String serieNumero = comp.getSerie() + "-" + comp.getNumero();
        PdfPCell serieCell = new PdfPCell(new Phrase(serieNumero, FONT_BOLD));
        serieCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tipoTable.addCell(serieCell);

        PdfPCell right = new PdfPCell(tipoTable);
        right.setBorder(Rectangle.BOX);

        table.addCell(left);
        table.addCell(right);
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void agregarDatosGenerales(Document document, ComprobanteBaseDto comp) throws DocumentException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        PdfPTable t = new PdfPTable(new float[]{18f, 32f, 18f, 32f});
        t.setWidthPercentage(100);

        t.addCell(celdaLabel("Fecha de Emisión:"));
        t.addCell(celdaValor(comp.getFechaEmision().format(fmt)));
        t.addCell(celdaLabel("Moneda:"));
        t.addCell(celdaValor(comp.getTipoMonedaTexto()));

        t.addCell(celdaLabel("Cliente:"));
        t.addCell(celdaValor(comp.getNombreCliente()));
        t.addCell(celdaLabel("Documento:"));
        t.addCell(celdaValor(comp.getNumeroDocCliente()));

        t.addCell(celdaLabel("Dirección:"));
        t.addCell(celdaValor(comp.getDireccionCliente()));
        t.addCell(celdaLabel("Forma de Pago:"));
        t.addCell(celdaValor(comp.getFormaPago()));

        document.add(t);
        document.add(new Paragraph(" "));
    }

    private void agregarTablaItems(Document document, ComprobanteBaseDto comp) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{10f, 15f, 45f, 15f, 15f});
        table.setWidthPercentage(100);

        table.addCell(celdaHeader("Cant."));
        table.addCell(celdaHeader("Und."));
        table.addCell(celdaHeader("Descripción"));
        table.addCell(celdaHeader("V. Unit"));
        table.addCell(celdaHeader("V. Venta"));

        for (ComprobanteItemDto item : comp.getItems()) {
            table.addCell(celdaValorRight(item.getCantidad().toPlainString()));
            table.addCell(celdaValor(item.getUnidadMedida()));
            table.addCell(celdaValor(item.getDescripcion()));
            table.addCell(celdaValorRight(formatoMonto(item.getValorUnitario())));
            table.addCell(celdaValorRight(formatoMonto(item.getImporteTotal())));
        }
        document.add(table);
        document.add(new Paragraph(" "));

        if (comp.getLeyendaImporteEnLetras() != null) {
            document.add(new Paragraph("SON: " + comp.getLeyendaImporteEnLetras(), FONT_BOLD));
            document.add(new Paragraph(" "));
        }
    }

    private void agregarTotales(Document document, ComprobanteBaseDto comp) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{60f, 40f});
        table.setWidthPercentage(100);

        // Celda vacía izquierda
        PdfPCell left = new PdfPCell(new Phrase(""));
        left.setBorder(Rectangle.NO_BORDER);
        table.addCell(left);

        PdfPTable totales = new PdfPTable(new float[]{50f, 50f});
        totales.setWidthPercentage(100);

        totales.addCell(celdaLabelRight("Op. Gravadas:"));
        totales.addCell(celdaValorRight(formatoMonto(comp.getOpGravadas())));

        totales.addCell(celdaLabelRight("IGV (18%):"));
        totales.addCell(celdaValorRight(formatoMonto(comp.getIgvTotal())));

        if (comp.getOtrosCargos() != null && comp.getOtrosCargos().compareTo(BigDecimal.ZERO) != 0) {
            totales.addCell(celdaLabelRight("Otros Cargos:"));
            totales.addCell(celdaValorRight(formatoMonto(comp.getOtrosCargos())));
        }

        totales.addCell(celdaLabelRight("IMPORTE TOTAL:"));
        totales.addCell(celdaValorRight(formatoMonto(comp.getImporteTotal())));

        PdfPCell right = new PdfPCell(totales);
        right.setBorder(Rectangle.BOX); // Caja alrededor de totales
        table.addCell(right);

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void agregarNotaRepresentacion(Document document, ComprobanteBaseDto comp) throws DocumentException {
        Paragraph p = new Paragraph(comp.getNotaRepresentacion(), FONT_FOOTER);
        p.setAlignment(Element.ALIGN_CENTER);
        document.add(p);
    }

    // Helpers
    private PdfPCell celdaSinBorde(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }
    private PdfPCell celdaHeader(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BOLD));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
    private PdfPCell celdaLabel(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BOLD));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }
    private PdfPCell celdaValor(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", FONT_NORMAL));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }
    private PdfPCell celdaLabelRight(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BOLD));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
    private PdfPCell celdaValorRight(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "0.00", FONT_NORMAL));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
    private String formatoMonto(BigDecimal monto) {
        return monto == null ? "0.00" : String.format("%.2f", monto);
    }
}