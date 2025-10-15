package com.altoque.altoque.Controller;

import com.altoque.altoque.Dto.EmailRequestDto;
import com.altoque.altoque.Service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
public class EmailController {

    @Autowired
    private NotificacionService notificacionService;

    @PostMapping("/enviar-documentos")
    public ResponseEntity<String> enviarDocumentos(@RequestBody EmailRequestDto request) {
        try {
            notificacionService.enviarContratoYCronograma(request);
            return ResponseEntity.ok("Correo con documentos enviado exitosamente a " + (request.getEmailDestino() != null ? request.getEmailDestino() : "cliente."));
        } catch (Exception e) {
            // Es buena práctica registrar el error
            // logger.error("Error al enviar correo: ", e);
            return ResponseEntity.internalServerError().body("Error al enviar el correo: " + e.getMessage());
        }
    }
}
