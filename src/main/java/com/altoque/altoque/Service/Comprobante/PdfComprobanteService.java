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

    // ⚠️ Ajusta estos datos a tu empresa (emisor)
    private static final String EMISOR_RUC = "20123456789";
    private static final String EMISOR_RAZON = "ALTOQUE FINANCIERA S.A.C.";
    private static final String EMISOR_DIRECCION = "AV. PRINCIPAL 123 - LIMA - PERU";

    private final PagoRepository pagoRepository;

    public PdfComprobanteService(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    // ================== API ==================

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

    // ================== Mapeo ==================

    private ComprobanteBaseDto construirDesdePago(Integer idPago, boolean esFactura) {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        Prestamo prestamo = pago.getPrestamo();
        Cliente cliente = prestamo.getCliente();

        BigDecimal total = BigDecimal.valueOf(pago.getMontoTotal()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gravada = total.divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(gravada);

        ComprobanteBaseDto dto = new ComprobanteBaseDto();

        dto.setRucEmisor(EMISOR_RUC);
        dto.setRazonSocialEmisor(EMISOR_RAZON);
        dto.setDireccionEmisor(EMISOR_DIRECCION);

        dto.setMoneda("PEN");
        dto.setTipoMonedaTexto("SOLES");
        dto.setFechaEmision(pago.getFechaPago().toLocalDate());
        dto.setFormaPago("CONTADO");

        dto.setNumero(String.format("%08d", pago.getIdPago()));

        if (esFactura) {
            dto.setTipoComprobante("FACTURA ELECTRONICA");
            dto.setSerie("F001");

            dto.setTipoDocCliente("6");
            dto.setNumeroDocCliente(cliente.getRuc());
            dto.setNombreCliente(cliente.getRazonSocial());
            dto.setDireccionCliente(
                    cliente.getDireccionFiscal() != null ? cliente.getDireccionFiscal() : cliente.getDireccionCliente()
            );
        } else {
            dto.setTipoComprobante("BOLETA DE VENTA ELECTRONICA");
            dto.setSerie("B001");

            dto.setTipoDocCliente("1");
            dto.setNumeroDocCliente(cliente.getDniCliente());
            dto.setNombreCliente(
                    (cliente.getNombreCliente() != null ? cliente.getNombreCliente() : "") +
                            " " +
                            (cliente.getApellidoCliente() != null ? cliente.getApellidoCliente() : "")
            );
            dto.setDireccionCliente(cliente.getDireccionCliente());
        }

        dto.setObservacion("SERVICIO DE PAGO DE PRÉSTAMO N° " + prestamo.getIdPrestamo());

        dto.setOpGravadas(gravada);
        dto.setOpExoneradas(BigDecimal.ZERO);
        dto.setOpInafectas(BigDecimal.ZERO);
        dto.setDescuentoGlobal(BigDecimal.ZERO);
        dto.setOtrosCargos(BigDecimal.valueOf(
                pago.getAjusteRedondeo() != null ? pago.getAjusteRedondeo() : 0.0
        ));
        dto.setIgvTotal(igv);
        dto.setImporteTotal(total);

        dto.setLeyendaImporteEnLetras(NumeroALetrasUtil.aLetras(total, "SOLES"));

        dto.setNotaRepresentacion(
                "Esta es una representación impresa de la " +
                        dto.getTipoComprobante().toLowerCase() +
                        ", generada desde el sistema del contribuyente. " +
                        "Puede verificarse en SUNAT con la clave SOL."
        );

        ComprobanteItemDto item = new ComprobanteItemDto();
        item.setCantidad(BigDecimal.ONE);
        item.setUnidadMedida("UNIDAD");
        item.setDescripcion("Pago de préstamo N° " + prestamo.getIdPrestamo());
        item.setValorUnitario(gravada);       // como cantidad=1, valor unitario = base imponible
        item.setImporteTotal(total);          // incluye IGV

        dto.setItems(Collections.singletonList(item));

        return dto;
    }

    // ================== Generación de PDF ==================

    public byte[] generarPdf(ComprobanteBaseDto comp) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
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
            throw new RuntimeException("Error generando PDF de comprobante", e);
        }
    }

    private void agregarEncabezado(Document document, ComprobanteBaseDto comp) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{60f, 40f});
        table.setWidthPercentage(100);

        PdfPTable emisorTable = new PdfPTable(1);
        emisorTable.setWidthPercentage(100);

        emisorTable.addCell(celdaSinBorde(comp.getRazonSocialEmisor(), FONT_BOLD));
        emisorTable.addCell(celdaSinBorde("RUC: " + comp.getRucEmisor(), FONT_NORMAL));
        emisorTable.addCell(celdaSinBorde(comp.getDireccionEmisor(), FONT_NORMAL));

        PdfPCell left = new PdfPCell(emisorTable);
        left.setBorder(Rectangle.NO_BORDER);

        PdfPTable tipoTable = new PdfPTable(1);
        tipoTable.setWidthPercentage(100);

        PdfPCell rucCell = new PdfPCell(new Phrase("RUC: " + comp.getRucEmisor(), FONT_BOLD));
        rucCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tipoTable.addCell(rucCell);

        PdfPCell tipoCell = new PdfPCell(new Phrase(comp.getTipoComprobante(), FONT_TITULO));
        tipoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
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

        t.addCell(celdaLabel("Señor(es):"));
        t.addCell(celdaValor(comp.getNombreCliente()));
        t.addCell(celdaLabel("Doc. Cliente:"));
        t.addCell(celdaValor(comp.getTipoDocCliente() + " - " + comp.getNumeroDocCliente()));

        t.addCell(celdaLabel("Dirección:"));
        t.addCell(celdaValor(comp.getDireccionCliente() != null ? comp.getDireccionCliente() : ""));
        t.addCell(celdaLabel("Forma de pago:"));
        t.addCell(celdaValor(comp.getFormaPago()));

        t.addCell(celdaLabel("Observación:"));
        PdfPCell obsCell = new PdfPCell(new Phrase(
                comp.getObservacion() != null ? comp.getObservacion() : "", FONT_NORMAL));
        obsCell.setColspan(3);
        obsCell.setBorder(Rectangle.NO_BORDER);
        t.addCell(obsCell);

        document.add(t);
        document.add(new Paragraph(" "));
    }

    private void agregarTablaItems(Document document, ComprobanteBaseDto comp) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{10f, 15f, 45f, 15f, 15f});
        table.setWidthPercentage(100);

        table.addCell(celdaHeader("Cantidad"));
        table.addCell(celdaHeader("Unidad"));
        table.addCell(celdaHeader("Descripción"));
        table.addCell(celdaHeader("Valor Unit."));
        table.addCell(celdaHeader("Valor Venta"));

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
            Paragraph p = new Paragraph(
                    "SON: " + comp.getLeyendaImporteEnLetras(), FONT_BOLD);
            document.add(p);
            document.add(new Paragraph(" "));
        }
    }

    private void agregarTotales(Document document, ComprobanteBaseDto comp) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{60f, 40f});
        table.setWidthPercentage(100);

        PdfPCell left = new PdfPCell(new Phrase(""));
        left.setBorder(Rectangle.NO_BORDER);
        table.addCell(left);

        PdfPTable totales = new PdfPTable(new float[]{60f, 40f});
        totales.setWidthPercentage(100);

        totales.addCell(celdaLabelRight("Op. Gravadas:"));
        totales.addCell(celdaValorRight(formatoMonto(comp.getOpGravadas())));

        totales.addCell(celdaLabelRight("IGV (18%):"));
        totales.addCell(celdaValorRight(formatoMonto(comp.getIgvTotal())));

        if (comp.getOtrosCargos() != null && comp.getOtrosCargos().compareTo(BigDecimal.ZERO) != 0) {
            totales.addCell(celdaLabelRight("Otros Cargos:"));
            totales.addCell(celdaValorRight(formatoMonto(comp.getOtrosCargos())));
        }

        totales.addCell(celdaLabelRight("Importe Total:"));
        totales.addCell(celdaValorRight(formatoMonto(comp.getImporteTotal())));

        PdfPCell right = new PdfPCell(totales);
        right.setBorder(Rectangle.NO_BORDER);
        table.addCell(right);

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void agregarNotaRepresentacion(Document document, ComprobanteBaseDto comp)
            throws DocumentException {

        String texto = comp.getNotaRepresentacion();
        Paragraph p = new Paragraph(texto, FONT_FOOTER);
        p.setAlignment(Element.ALIGN_CENTER);
        document.add(p);
    }

    // ===== Helpers celdas ====

    private PdfPCell celdaSinBorde(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell celdaHeader(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BOLD));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(230, 230, 230));
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
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", FONT_NORMAL));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private String formatoMonto(BigDecimal monto) {
        if (monto == null) return "0.00";
        return String.format("%.2f", monto);
    }
}
