package com.altoque.altoque.Controller;

import com.altoque.altoque.Dto.Payment.PagoRequestDto;
import com.altoque.altoque.Dto.Payment.PreferenceResponseDto;
import com.altoque.altoque.Service.MercadoPagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "*")
public class MercadoPagoController {

    @Autowired
    private MercadoPagoService mercadoPagoService;

    @PostMapping("/crear-preferencia")
    public ResponseEntity<?> crearPreferencia(@RequestBody PagoRequestDto request) {
        try {
            System.out.println("📥 Solicitud de preferencia recibida:");
            System.out.println("   Préstamo ID: " + request.getPrestamoId());
            System.out.println("   Monto: " + request.getMonto());
            System.out.println("   Método: " + request.getMetodoPago());

            // Validaciones básicas antes de llamar al servicio
            if (request.getMonto() == null || request.getMonto().doubleValue() <= 0) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "El monto debe ser mayor a 0");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.getPrestamoId() == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "El ID del préstamo es requerido");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // --- CORRECCIÓN AQUÍ ---
            // Pasamos el objeto DTO completo directamente al servicio.
            // El servicio ahora se encarga de extraer descripción, cliente, montos, etc.
            PreferenceResponseDto response = mercadoPagoService.createPreference(request);

            System.out.println("✅ Preferencia creada con éxito. ID: " + response.getPreferenceId());

            // Retornamos directamente la respuesta del servicio (que ya contiene el ID y el initPoint)
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            System.err.println("❌ Error procesando solicitud: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            // El mensaje de la excepción ahora vendrá detallado desde el servicio (ej: "Error Mercado Pago: Invalid Payer")
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}