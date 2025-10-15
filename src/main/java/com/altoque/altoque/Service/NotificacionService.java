package com.altoque.altoque.Service;

import com.altoque.altoque.Dto.EmailRequestDto;
import com.altoque.altoque.Entity.Cliente;
import com.altoque.altoque.Entity.Prestamo;
import com.altoque.altoque.Repository.ClienteRepository;
import com.altoque.altoque.Repository.PrestamoRepository;
import com.altoque.altoque.Utils.DocumentoGenerator;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.Map;

@Service
public class NotificacionService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private ClienteRepository clienteRepository; // Asumo que tienes estos repositorios

    @Autowired
    private PrestamoRepository prestamoRepository; // Asumo que tienes estos repositorios

    public void enviarContratoYCronograma(EmailRequestDto request) throws MessagingException {
        // 1. Buscar las entidades en la base de datos
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + request.getClienteId()));

        Prestamo prestamo = prestamoRepository.findById(request.getPrestamoId())
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado con ID: " + request.getPrestamoId()));

        // 2. Generar los documentos PDF usando tu clase Util
        byte[] contratoPdf = DocumentoGenerator.generarContratoPrestamo(prestamo);
        byte[] cronogramaPdf = DocumentoGenerator.generarCronogramaPDF(
                cliente.getNombreCliente() + " " + cliente.getApellidoCliente(),
                prestamo.getMonto(),
                prestamo.getTasaInteresAnual(), // Asumo que el préstamo tiene estos campos
                prestamo.getNumeroCuotas()
        );

        // 3. Preparar los adjuntos para el servicio de correo
        Map<String, byte[]> attachments = new HashMap<>();
        attachments.put("Contrato-Prestamo-" + cliente.getDniCliente() + ".pdf", contratoPdf);
        attachments.put("Cronograma-Pagos-" + cliente.getDniCliente() + ".pdf", cronogramaPdf);

        // 4. Definir el contenido del correo
        String emailDestino = (request.getEmailDestino() != null && !request.getEmailDestino().isEmpty())
                ? request.getEmailDestino()
                : cliente.getCorreoCliente(); // Asumo que el cliente tiene un campo email

        String subject = "Documentos de su préstamo - Financiera AL TOQUE S.A.C.";
        String body = "<html>"
                + "<body>"
                + "<h3>Estimado(a) " + cliente.getNombreCliente() + ",</h3>"
                + "<p>Le hacemos llegar los documentos correspondientes a su préstamo personal.</p>"
                + "<ul>"
                + "<li><b>Contrato de Préstamo Personal</b></li>"
                + "<li><b>Cronograma de Pagos Detallado</b></li>"
                + "</ul>"
                + "<p>Por favor, revise los documentos adjuntos. Si tiene alguna consulta, no dude en contactarnos.</p>"
                + "<br>"
                + "<p>Atentamente,</p>"
                + "<b>Financiera AL TOQUE S.A.C.</b>"
                + "</body>"
                + "</html>";


        // 5. Enviar el correo
        emailService.sendEmailWithAttachments(emailDestino, subject, body, attachments);
    }
}