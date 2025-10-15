package com.altoque.altoque.Controller;

import com.altoque.altoque.Dto.ClienteConsultaDto;
import com.altoque.altoque.Dto.ClienteDetalleDto; // NEW: Import the new DTO
import com.altoque.altoque.Entity.Cliente;
import com.altoque.altoque.Service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    // Endpoint for initial DNI check (remains the same)
    @GetMapping("/consulta/{dni}")
    public Mono<ResponseEntity<ClienteConsultaDto>> consultarDni(@PathVariable String dni) {
        return clienteService.consultarClienteYEstadoPrestamo(dni)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // NEW ENDPOINT: Get full client details for the form
    /**
     * Obtiene los detalles completos de un cliente para rellenar el formulario de registro/actualización.
     * Si el cliente existe en la BD, devuelve todos sus datos.
     * Si no existe, consulta la API externa y devuelve los datos básicos (DNI, nombre, apellido).
     * @param dni DNI del cliente a consultar.
     * @return Mono con ClienteDetalleDto para el frontend.
     */
    @GetMapping("/detalles/{dni}")
    public Mono<ResponseEntity<ClienteDetalleDto>> obtenerDetallesParaFormulario(@PathVariable String dni) {
        return clienteService.obtenerOPrepararClienteParaFormulario(dni)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    // MODIFIED ENDPOINT: Register a new client or update an existing one
    /**
     * Registra un nuevo cliente si no existe por DNI, o actualiza los datos
     * de un cliente existente.
     * @param cliente El objeto Cliente con los datos del formulario.
     * @return El cliente guardado (con su ID) y el código de estado HTTP correspondiente.
     */
    @PostMapping("/registrarOActualizar")
    public ResponseEntity<?> registrarOActualizarCliente(@RequestBody ClienteDetalleDto cliente) {
        try {
            // The service logic will handle whether to create or update
            Cliente clienteGuardado = clienteService.registrarOActualizar(cliente);

            // Check if the client was created or updated to return the correct status code
            boolean esNuevo = cliente.getDniCliente() == null;
            if (esNuevo) {
                // Return 201 CREATED for new resources
                return new ResponseEntity<>(clienteGuardado, HttpStatus.CREATED);
            } else {
                // Return 200 OK for updated resources
                return new ResponseEntity<>(clienteGuardado, HttpStatus.OK);
            }
        } catch (Exception e) {
            // Generic error handler
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar la solicitud: " + e.getMessage());
        }
    }
}