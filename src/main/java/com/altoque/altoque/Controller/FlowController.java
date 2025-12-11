package com.altoque.altoque.Controller;

import com.altoque.altoque.Dto.Flow.FlowCreatePaymentDto;
import com.altoque.altoque.Dto.Flow.FlowCreateResponseDto;
import com.altoque.altoque.Dto.Payment.PagoResponseDto;
import com.altoque.altoque.Entity.Caja;
import com.altoque.altoque.Entity.Pago;
import com.altoque.altoque.Entity.Usuario;
import com.altoque.altoque.Repository.CajaRepository;
import com.altoque.altoque.Repository.PagoRepository;
import com.altoque.altoque.Repository.UsuarioRepository;
import com.altoque.altoque.Service.FlowPaymentService;
import com.altoque.altoque.Service.OperacionesPagoService; // Importante
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
    private OperacionesPagoService operacionesPagoService; // Inyectamos el servicio de negocio

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CajaRepository cajaRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody FlowCreatePaymentDto request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

            Map<String, Object> metadata = new HashMap<>();

            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                metadata.put("userId", usuario.getIdUsuario());

                // Buscar caja abierta
                Optional<Caja> cajaAbierta = cajaRepository.findByUsuarioAndFechaCierreIsNull(usuario);
                if (cajaAbierta.isPresent()) {
                    metadata.put("cajaId", cajaAbierta.get().getIdCaja());
                }
            }

            request.setMetadata(metadata);
            FlowCreateResponseDto response = flowPaymentService.createPayment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error iniciando pago: " + e.getMessage());
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
                // 1. Verificar idempotencia (si ya se procesó)
                Optional<Pago> pagoExistente = pagoRepository.findByOrdenExterna(commerceOrder);
                if (pagoExistente.isPresent()) {
                    Pago p = pagoExistente.get();
                    response.put("paymentId", p.getIdPago());
                    response.put("amount", p.getMonto());
                    return ResponseEntity.ok(response);
                }

                // 2. Extraer datos para procesar negocio
                String[] parts = commerceOrder.split("-");
                if (parts.length >= 2 && "LOAN".equals(parts[0])) {
                    Integer prestamoId = Integer.parseInt(parts[1]);
                    Double amount = Double.parseDouble(flowStatus.get("amount").toString());

                    // Recuperar Metadata (Caja y Usuario)
                    Integer cajaId = null;
                    Integer userId = null;

                    if (flowStatus.containsKey("optional")) {
                        Object optionalObj = flowStatus.get("optional");
                        Map<String, Object> optionalMap = null;
                        if (optionalObj instanceof Map) {
                            optionalMap = (Map<String, Object>) optionalObj;
                        } else if (optionalObj instanceof String) {
                            optionalMap = objectMapper.readValue((String) optionalObj, Map.class);
                        }

                        if (optionalMap != null) {
                            if (optionalMap.containsKey("userId")) userId = Integer.valueOf(optionalMap.get("userId").toString());
                            if (optionalMap.containsKey("cajaId")) cajaId = Integer.valueOf(optionalMap.get("cajaId").toString());
                        }
                    }

                    if (cajaId != null && userId != null) {
                        // 3. LLAMADA AL NÚCLEO DE NEGOCIO
                        // Esto ejecuta la lógica de cuotas, mora y actualiza la caja
                        PagoResponseDto resultNegocio = operacionesPagoService.procesarPagoFlow(
                                prestamoId,
                                amount,
                                cajaId,
                                userId,
                                flowOrder,
                                commerceOrder
                        );

                        response.put("paymentId", resultNegocio.getIdPago());
                        response.put("amount", amount);
                    } else {
                        // Fallback crítico: Si no hay caja (ej: pagó días después y la caja cerró),
                        // deberías tener una lógica de "Caja de Administración" o lanzar error.
                        // Por ahora logueamos error.
                        System.err.println("Pago Flow sin Caja asociada en metadata. CommerceOrder: " + commerceOrder);
                        // Opcional: Procesar sin caja o asignar a caja admin.
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