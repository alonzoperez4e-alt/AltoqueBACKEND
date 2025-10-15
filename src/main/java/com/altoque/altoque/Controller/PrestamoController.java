package com.altoque.altoque.Controller;

import com.altoque.altoque.Dto.DeclaracionUITRequestDto;
import com.altoque.altoque.Dto.PrestamoDto;
import com.altoque.altoque.Entity.Cliente;
import com.altoque.altoque.Entity.Prestamo;
import com.altoque.altoque.Service.PrestamoService;
import com.altoque.altoque.Service.ClienteService;
import com.altoque.altoque.Service.PdfService;
import com.altoque.altoque.Utils.DocumentoGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.altoque.altoque.Dto.ClienteDetalleDto;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    @Autowired
    private PrestamoService prestamoService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private ClienteService clienteService;

    // --- NUEVO ENDPOINT: OBTENER PRÉSTAMO POR ID ---
    // Útil si se necesita recargar la página de detalles.
    @GetMapping("/{id}")
    public ResponseEntity<Prestamo> getPrestamoById(@PathVariable Integer id) {
        return prestamoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- NUEVO ENDPOINT: GENERAR CONTRATO PDF ---
    // Endpoint específico para descargar el contrato de un préstamo existente.
    @GetMapping("/{id}/contrato-pdf")
    public ResponseEntity<?> generarContratoPDF(@PathVariable Integer id) {
        try {
            Prestamo prestamo = prestamoService.obtenerPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado con ID: " + id));

            // --- CAMBIO ---
            // Se pasa el objeto 'prestamo' completo al generador.
            byte[] pdfBytes = DocumentoGenerator.generarContratoPrestamo(prestamo);

            String filename = "Contrato_Prestamo_" + prestamo.getCliente().getDniCliente() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al generar el contrato: " + e.getMessage());
        }
    }

    // LISTAR TODOS LOS PRÉSTAMOS
    @GetMapping("/listar")
    public ResponseEntity<List<Prestamo>> listarPrestamos() {
        List<Prestamo> prestamos = prestamoService.listarPrestamos();
        return ResponseEntity.ok(prestamos);
    }

    // REGISTRAR PRESTAMO
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarPrestamo(
            @Valid @RequestBody PrestamoDto prestamoDto,
            BindingResult result) {

        // 1. Se mantiene la validación de los datos de entrada.
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
        }

        try {
            // 2. Se llama al servicio para guardar el préstamo en la base de datos.
            Prestamo prestamoGuardado = prestamoService.registrarPrestamo(prestamoDto);

            // 3. Se devuelve el objeto del préstamo recién creado con un estado 201 (CREATED).
            //    Esto es más específico que un 200 OK para operaciones de creación.
            return new ResponseEntity<>(prestamoGuardado, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            // Si el servicio lanza un error de validación (ej. monto inválido), se devuelve un 400.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Para cualquier otro error inesperado, se devuelve un 500.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar el préstamo: " + e.getMessage());
        }
    }

    //GENERAR CRONOGRAMA
    @GetMapping("/{id}/cronograma-pdf")
    public ResponseEntity<?> generarCronogramaPDF(@PathVariable Integer id) {
        try {
            // 1️⃣ Buscar préstamo por ID
            Prestamo prestamo = prestamoService.obtenerPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado con ID: " + id));

            Cliente cliente = prestamo.getCliente();

            // 2️⃣ Generar el PDF con los datos del préstamo
            byte[] pdfBytes = DocumentoGenerator.generarCronogramaPDF(
                    cliente.getNombreCliente() + " " + cliente.getApellidoCliente(),
                    prestamo.getMonto(),
                    prestamo.getTasaInteresAnual(),
                    prestamo.getNumeroCuotas()
            );

            // 3️⃣ Devolver el PDF como descarga
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=Cronograma_" + cliente.getDniCliente() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al generar el cronograma de pagos: " + e.getMessage());
        }
    }

    // ============================================
    // BUSCAR PRÉSTAMOS POR DNI (Y OPCIONALMENTE ESTADO)
    // ============================================
    @PostMapping("/buscarPorDni")
    public ResponseEntity<?> buscarPorDni(@RequestBody Map<String, String> body) {
        try {
            String dni = body.get("dni");
            if (dni == null || dni.isBlank()) {
                return ResponseEntity.badRequest().body("El campo 'dni' es obligatorio.");
            }

            String estado = body.get("estado"); // opcional
            List<Prestamo> prestamos;

            if (estado != null && !estado.isBlank()) {
                prestamos = prestamoService.buscarPorClienteDniYEstado(dni, estado);
            } else {
                prestamos = prestamoService.buscarPorClienteDni(dni);
            }

            if (prestamos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No se encontraron préstamos para el cliente con DNI: " + dni);
            }

            return ResponseEntity.ok(prestamos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error buscando préstamos: " + e.getMessage());
        }
    }
    // Ubicado en tu archivo PrestamoController.java


    @PostMapping("/documentos/declaracion-pep")
    public ResponseEntity<byte[]> generarDeclaracionPep(@RequestBody ClienteDetalleDto clienteDto) {
        // Se llama al método correcto que existe en tu PdfService
        byte[] pdf = pdfService.generarDeclaracionPEPPorDatos(clienteDto);

        String filename = "Declaracion_PEP_" + clienteDto.getDniCliente() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // El endpoint original que busca por DNI puede ser eliminado o mantenido para otros usos
    @GetMapping("/documentos/declaracion-pep/{dni}")
    public ResponseEntity<byte[]> generarDeclaracionPepPorDni(@PathVariable String dni) {
        byte[] pdf = pdfService.generarDeclaracionPEPPorDni(dni);
        String filename = "Declaracion_PEP_" + dni + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
/*
    // Generar contrato/Declaración por monto > UIT (por DNI)
    @GetMapping("/documentos/contrato-prestamo/{dni}")
    public ResponseEntity<byte[]> generarContratoPrestamo(
            @PathVariable String dni,
            @RequestParam double monto) {

        byte[] pdf = pdfService.generarContratoPorDni(dni, monto);
        String filename = "Contrato_Prestamo_" + dni + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }*/

    // --- ENDPOINT ACTUALIZADO ---
    // Ahora recibe un objeto específico (DeclaracionUitRequestDto) en lugar de un Map genérico.
    // Esto hace que el código sea más seguro y claro.
    @PostMapping("/documentos/declaracion-uit")
    public ResponseEntity<byte[]> generarDeclaracionUit(@RequestBody DeclaracionUITRequestDto request) {
        try {
            // 1. Extraemos los datos del cliente y el monto directamente del DTO.
            ClienteDetalleDto clienteDto = request.getClient();
            double monto = request.getAmount();

            // 2. Llamamos al servicio para que genere el PDF con estos datos.
            byte[] pdf = pdfService.generarDeclaracionUITPorDatos(clienteDto, monto);

            // 3. Devolvemos el PDF como una descarga.
            String filename = "Declaracion_Jurada_UIT_" + clienteDto.getDniCliente() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (Exception e) {
            // Se mantiene el manejo de errores por si la generación del PDF falla internamente.
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        }
    }




}