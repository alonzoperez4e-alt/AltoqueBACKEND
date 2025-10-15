package com.altoque.altoque.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;


import java.util.Map;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Envía un correo electrónico con múltiples archivos adjuntos.
     *
     * @param to          El destinatario del correo.
     * @param subject     El asunto del correo.
     * @param body        El cuerpo del correo (puede ser HTML).
     * @param attachments Un mapa donde la clave es el nombre del archivo (ej. "contrato.pdf")
     * y el valor es el contenido del archivo en bytes.
     */
    public void sendEmailWithAttachments(String to, String subject, String body, Map<String, byte[]> attachments) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        // El 'true' en el constructor indica que será un mensaje multipart (necesario para adjuntos)
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true); // El 'true' indica que el cuerpo es HTML

        // Añadir los archivos adjuntos
        if (attachments != null) {
            for (Map.Entry<String, byte[]> entry : attachments.entrySet()) {
                String filename = entry.getKey();
                byte[] content = entry.getValue();
                // ByteArrayResource es una clase de Spring que envuelve el array de bytes
                helper.addAttachment(filename, new ByteArrayResource(content));
            }
        }

        mailSender.send(mimeMessage);
    }
}
