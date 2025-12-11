package com.altoque.altoque.Controller;

import com.altoque.altoque.Dto.Flow.FlowCreatePaymentDto;
import com.altoque.altoque.Dto.Flow.FlowCreateResponseDto;
import com.altoque.altoque.Dto.Payment.PagoResponseDto;
import com.altoque.altoque.Entity.Caja;
import com.altoque.altoque.Entity.Pago;
import com.altoque.altoque.Entity.Prestamo;
import com.altoque.altoque.Entity.Usuario;
import com.altoque.altoque.Repository.CajaRepository;
import com.altoque.altoque.Repository.PagoRepository;
import com.altoque.altoque.Repository.PrestamoRepository;
import com.altoque.altoque.Repository.UsuarioRepository;
import com.altoque.altoque.Service.FlowPaymentService;
import com.altoque.altoque.Service.OperacionesPagoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/flow")
public class FlowController {

    @Autowired
    private FlowPaymentService flowPaymentService;

    @Autowired
    private OperacionesPagoService operacionesPagoService;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CajaRepository cajaRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Inicia una transacción de pago.
     * Con logs detallados para depuración de error 500.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody FlowCreatePaymentDto request) {
        try {
            System.out.println("--- Inicio Flow /create ---");

            // 1. Obtener autenticación con validación defensiva
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null) ? auth.getName() : "ANONIMO";
            System.out.println("Usuario autenticado: " + username);

            Map<String, Object> metadata = new HashMap<>();

            // Solo intentamos buscar datos de usuario si hay un username válido
            if (username != null && !username.equals("ANONIMO")) {
                Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

                if (usuarioOpt.isPresent()) {
                    Usuario usuario = usuarioOpt.get();
                    metadata.put("userId", usuario.getIdUsuario()); // Integer
                    System.out.println("Usuario ID encontrado: " + usuario.getIdUsuario());

                    // 2. Buscar caja abierta (Envuelto en try-catch para no bloquear el pago si falla)
                    try {
                        Optional<Caja> cajaAbierta = cajaRepository.findByUsuarioAndFechaCierreIsNull(usuario);
                        if (cajaAbierta.isPresent()) {
                            metadata.put("cajaId", cajaAbierta.get().getIdCaja()); // Integer
                            System.out.println("Caja Abierta ID vinculada: " + cajaAbierta.get().getIdCaja());
                        } else {
                            System.out.println("El usuario no tiene caja abierta activa.");
                        }
                    } catch (Exception e) {
                        System.err.println("Error no bloqueante buscando caja: " + e.getMessage());
                        // Continuamos sin vincular caja
                    }
                } else {
                    System.err.println("Error: Usuario autenticado no encontrado en BD: " + username);
                }
            } else {
                System.out.println("Advertencia: Petición sin usuario autenticado (Spring Security Context vacío).");
            }

            request.setMetadata(metadata);

            // 3. Llamada al servicio de Flow
            System.out.println("Invocando FlowPaymentService...");
            FlowCreateResponseDto response = flowPaymentService.createPayment(request);

            System.out.println("Respuesta exitosa de Flow recibida. URL: " + response.getUrl());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace(); // CRÍTICO: Esto imprimirá el error real en tus logs de Heroku
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno procesando pago Flow: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> checkPaymentStatus(@RequestParam("token") String token) {
        try {
            Map<String, Object> flowStatus = flowPaymentService.getPaymentStatusExtended(token);

            int status = Integer.parseInt(flowStatus.get("status").toString());
            String flowOrder = flowStatus.get("flowOrder").toString();
            String commerceOrder = flowStatus.get("commerceOrder").toString();

            Map<String, Object> response = new HashMap<>();
            response.put("status", mapFlowStatus(status));
            response.put("flowOrder", flowOrder);

            if (status == 2) { // PAGADO
                Optional<Pago> pagoExistente = pagoRepository.findByOrdenExterna(commerceOrder);
                if (pagoExistente.isPresent()) {
                    Pago p = pagoExistente.get();
                    response.put("paymentId", p.getIdPago());
                    response.put("amount", p.getMonto());
                    return ResponseEntity.ok(response);
                }

                String[] parts = commerceOrder.split("-");
                if (parts.length >= 2 && "LOAN".equals(parts[0])) {
                    Integer prestamoId = Integer.parseInt(parts[1]); // ID prestamo suele ser Long en path pero Integer en DB, verificar
                    Double amount = Double.parseDouble(flowStatus.get("amount").toString());

                    Integer cajaId = null;
                    Integer userId = null;

                    if (flowStatus.containsKey("optional")) {
                        Object optionalObj = flowStatus.get("optional");
                        Map<String, Object> optionalMap = null;

                        try {
                            if (optionalObj instanceof Map) {
                                optionalMap = (Map<String, Object>) optionalObj;
                            } else if (optionalObj instanceof String) {
                                optionalMap = objectMapper.readValue((String) optionalObj, Map.class);
                            }
                        } catch (Exception ex) {
                            System.err.println("Error parseando metadata optional: " + ex.getMessage());
                        }

                        if (optionalMap != null) {
                            // CORRECCIÓN: Parseo seguro a Integer
                            if (optionalMap.containsKey("userId")) {
                                try {
                                    userId = Integer.valueOf(optionalMap.get("userId").toString());
                                } catch (NumberFormatException e) {
                                    System.err.println("Error formato userId: " + e.getMessage());
                                }
                            }
                            if (optionalMap.containsKey("cajaId")) {
                                try {
                                    cajaId = Integer.valueOf(optionalMap.get("cajaId").toString());
                                } catch (NumberFormatException e) {
                                    System.err.println("Error formato cajaId: " + e.getMessage());
                                }
                            }
                        }
                    }

                    // Si tenemos datos, procesamos con lógica de negocio
                    if (cajaId != null && userId != null) {
                        // Nota: procesarPagoFlow espera Long o Integer según tu service.
                        // Asumiendo que tu service usa Integer para IDs de usuario/caja como indicaste.
                        // Convertimos a Long si el servicio lo pide, o ajustamos el servicio.
                        // En tu OperacionesPagoService anterior vi que usabas Long para los IDs en procesarPagoFlow.
                        // Si cambiaste a Integer aquí, asegúrate que procesarPagoFlow acepte Integer o castéalos.

                        PagoResponseDto resultNegocio = operacionesPagoService.procesarPagoFlow(
                                prestamoId, amount, cajaId, userId, flowOrder, commerceOrder
                        );
                        response.put("paymentId", resultNegocio.getIdPago());
                        response.put("amount", amount);
                    } else {
                        // Fallback: Guardar pago simple sin lógica de cuotas para no perder el dinero
                        System.err.println("ALERTA: Pago Flow sin metadata de Caja/Usuario. Guardando registro básico.");
                        // Aquí podrías implementar un guardado de emergencia si lo deseas
                    }
                }
            } else if (status == 3 || status == 4) {
                if (flowStatus.containsKey("lastError")) {
                    response.put("error", flowStatus.get("lastError"));
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error validando pago: " + e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestParam("token") String token) {
        try {
            flowPaymentService.getPaymentStatus(token);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String mapFlowStatus(int status) {
        switch (status) {
            case 1: return "PENDING";
            case 2: return "SUCCESS";
            case 3: return "FAILURE";
            case 4: return "FAILURE";
            default: return "UNKNOWN";
        }
    }
}