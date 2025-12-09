package com.altoque.altoque.Controller;

import com.altoque.altoque.Dto.Payment.EstadoCuentaDto;
import com.altoque.altoque.Dto.Payment.PagoRequestDto;
import com.altoque.altoque.Dto.Payment.PagoResponseDto;
import com.altoque.altoque.Service.OperacionesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operaciones")
@CrossOrigin(origins = "*")
public class OperacionesController {

    @Autowired
    private OperacionesService operacionesService;

    @GetMapping("/estado-cuenta/cliente/{clienteId}")
    public ResponseEntity<EstadoCuentaDto> obtenerEstadoCuenta(@PathVariable Integer clienteId) { // Integer explícito
        return ResponseEntity.ok(operacionesService.obtenerEstadoCuenta(clienteId));
    }

    @PostMapping("/procesar-pago")
    public ResponseEntity<PagoResponseDto> procesarPago(@RequestBody PagoRequestDto request) {
        return ResponseEntity.ok(operacionesService.procesarPago(request));
    }
}