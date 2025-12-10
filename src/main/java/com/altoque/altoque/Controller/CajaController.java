package com.altoque.altoque.Controller;

import com.altoque.altoque.Dto.CajaRequestDto;
import com.altoque.altoque.Dto.MovimientoCajaDto;
import com.altoque.altoque.Entity.Caja;
import com.altoque.altoque.Entity.Usuario;
import com.altoque.altoque.Repository.UsuarioRepository;
import com.altoque.altoque.Service.CajaService;
import com.altoque.altoque.Exception.ApiServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caja")
@CrossOrigin(origins = "*") // Ajusta según tus políticas CORS
public class CajaController {

    @Autowired
    private CajaService cajaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Helper para obtener el ID del usuario autenticado
    private Integer getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ApiServerException("Usuario no encontrado en el contexto de seguridad"));
        return usuario.getIdUsuario();
    }

    @PostMapping("/abrir")
    public ResponseEntity<?> abrirCaja(@RequestBody CajaRequestDto request) {
        Integer userId = getAuthenticatedUserId();
        Caja caja = cajaService.abrirCaja(userId, request.getSaldo());
        return ResponseEntity.ok(caja);
    }

    @GetMapping("/actual")
    public ResponseEntity<?> obtenerCajaActual() {
        Integer userId = getAuthenticatedUserId();
        // El servicio lanza excepción si no hay caja, la cual captura el GlobalExceptionHandler
        Caja caja = cajaService.obtenerCajaActual(userId);
        return ResponseEntity.ok(caja);
    }

    @PostMapping("/cerrar")
    public ResponseEntity<?> cerrarCaja(@RequestBody CajaRequestDto request) {
        Integer userId = getAuthenticatedUserId();
        Caja caja = cajaService.cerrarCaja(userId, request.getSaldo());
        return ResponseEntity.ok(caja);
    }

    // NUEVO ENDPOINT
    @GetMapping("/movimientos")
    public ResponseEntity<List<MovimientoCajaDto>> obtenerMovimientos() {
        Integer userId = getAuthenticatedUserId();
        List<MovimientoCajaDto> movimientos = cajaService.obtenerMovimientosCajaActual(userId);
        return ResponseEntity.ok(movimientos);
    }
}