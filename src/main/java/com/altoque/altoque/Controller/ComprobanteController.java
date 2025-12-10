package com.altoque.altoque.Controller;

import com.altoque.altoque.Service.Comprobante.PdfComprobanteService;
import com.altoque.altoque.Service.EmailService;

import jakarta.mail.MessagingException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/comprobantes")
public class ComprobanteController {

    private final PdfComprobanteService pdfService;
    private final EmailService emailService;

    public ComprobanteController(PdfComprobanteService pdfService, EmailService emailService) {
        this.pdfService = pdfService;
        this.emailService = emailService;
    }

    // ============================================================
    //                       FACTURA
    // ============================================================

    @GetMapping("/factura/{pagoId}")
    public ResponseEntity<byte[]> factura(
            @PathVariable Integer pagoId,
            @RequestParam(defaultValue = "descargar") String accion,
            @RequestParam(required = false) String emailDestino
    ) {
        byte[] pdf = pdfService.generarFacturaPorPago(pagoId);
        String nombreArchivo = "FACTURA_F001-" + String.format("%08d", pagoId) + ".pdf";
        String asunto = "Factura electrónica F001-" + String.format("%08d", pagoId);

        return manejarAccion(pdf, nombreArchivo, accion, emailDestino, asunto);
    }

    // ============================================================
    //                       BOLETA
    // ============================================================

    @GetMapping("/boleta/{pagoId}")
    public ResponseEntity<byte[]> boleta(
            @PathVariable Integer pagoId,
            @RequestParam(defaultValue = "descargar") String accion,
            @RequestParam(required = false) String emailDestino
    ) {
        byte[] pdf = pdfService.generarBoletaPorPago(pagoId);
        String nombreArchivo = "BOLETA_B001-" + String.format("%08d", pagoId) + ".pdf";
        String asunto = "Boleta electrónica B001-" + String.format("%08d", pagoId);

        return manejarAccion(pdf, nombreArchivo, accion, emailDestino, asunto);
    }

    // ============================================================
    //               MANEJO DE IMPRIMIR / DESCARGAR / CORREO
    // ============================================================

    private ResponseEntity<byte[]> manejarAccion(
            byte[] pdfBytes,
            String nombreArchivo,
            String accion,
            String emailDestino,
            String asuntoCorreo
    ) {

        if ("correo".equalsIgnoreCase(accion)) {

            if (emailDestino == null || emailDestino.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }

            Map<String, byte[]> adjuntos = Map.of(nombreArchivo, pdfBytes);

            try {
                emailService.sendEmailWithAttachments(
                        emailDestino,
                        asuntoCorreo,
                        "Adjuntamos su comprobante electrónico.",
                        adjuntos
                );
            } catch (MessagingException e) {
                throw new RuntimeException("Error enviando correo con comprobante", e);
            }

            return ResponseEntity.ok().build();
        }

        boolean attachment = !"imprimir".equalsIgnoreCase(accion);
        String disposition = (attachment ? "attachment" : "inline") +
                "; filename=\"" + nombreArchivo + "\"";

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}