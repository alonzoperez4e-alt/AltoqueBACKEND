
package com.altoque.altoque.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Método existente para textos simples (si lo tienes)
    public void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }

    /**
     * Envía un correo con archivos adjuntos generados en memoria (byte[])
     * @param to Destinatario
     * @param subject Asunto
     * @param body Cuerpo del mensaje (puede ser HTML)
     * @param attachments Mapa con <NombreArchivo, ContenidoBytes>
     */
    public void sendEmailWithAttachments(String to, String subject, String body, Map<String, byte[]> attachments) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();

        // true = multipart, "UTF-8" = encoding para caracteres especiales (tildes, ñ)
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail); // Importante: Remitente
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true); // true para interpretar HTML

        if (attachments != null) {
            for (Map.Entry<String, byte[]> entry : attachments.entrySet()) {
                // ByteArrayResource permite adjuntar bytes como si fueran un archivo
                helper.addAttachment(entry.getKey(), new ByteArrayResource(entry.getValue()));
            }
        }

        javaMailSender.send(message);
    }
}