package com.altoque.altoque.Controller;

import com.altoque.altoque.Dto.Payment.EstadoCuentaDto;
import com.altoque.altoque.Dto.Payment.PagoRequestDto;
import com.altoque.altoque.Dto.Payment.PagoResponseDto;
import com.altoque.altoque.Service.OperacionesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.altoque.altoque.Dto.Payment.PagoRequestDto;
import com.altoque.altoque.Dto.Payment.PagoResponseDto;
import com.altoque.altoque.Entity.Usuario;
import com.altoque.altoque.Repository.UsuarioRepository;
import com.altoque.altoque.Service.OperacionesPagoService;
import com.altoque.altoque.Service.OperacionesService;
import com.altoque.altoque.Exception.ApiServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operaciones")
@CrossOrigin(origins = "*")
public class OperacionesController {

    @Autowired
    private OperacionesService operacionesService;

    @Autowired
    private OperacionesPagoService operacionesPagoService; // El nuevo servicio de pagos con caja

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/estado-cuenta/cliente/{clienteId}")
    public ResponseEntity<EstadoCuentaDto> obtenerEstadoCuenta(@PathVariable Integer clienteId) { // Integer explícito
        return ResponseEntity.ok(operacionesService.obtenerEstadoCuenta(clienteId));
    }

    // Endpoint para Procesar Pago (Efectivo y Online)
    // CORREGIDO: Ahora usa operacionesPagoService para persistir el Pago y actualizar Caja.
    // Mantenemos la ruta "/procesar-pago" para que tu frontend actual funcione sin cambios.
    @PostMapping("/procesar-pago")
    public ResponseEntity<PagoResponseDto> procesarPago(@RequestBody PagoRequestDto request) {
        Integer userId = getAuthenticatedUserId();

        // Delegamos al nuevo servicio que sí guarda en la tabla 'pago' y actualiza la 'caja'
        PagoResponseDto response = operacionesPagoService.procesarPago(request, userId);

        return ResponseEntity.ok(response);
    }


    private Integer getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ApiServerException("Usuario no encontrado"));
        return usuario.getIdUsuario();
    }


}