package com.altoque.altoque.Controller;

import com.altoque.altoque.Dto.CuotaDto;
import com.altoque.altoque.Dto.DeclaracionUITRequestDto;
import com.altoque.altoque.Dto.PrestamoDto;
import com.altoque.altoque.Dto.ClienteDetalleDto; // Importante
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

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    @Autowired
    private PrestamoService prestamoService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private ClienteService clienteService;

    // --- 1. ENDPOINTS PRINCIPALES (NUEVA LÓGICA) ---

    // Registrar Préstamo (Genera cronograma automáticamente)
    @PostMapping("/registro")
    public ResponseEntity<?> registrarPrestamo(@Valid @RequestBody PrestamoDto prestamoDto, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
        }
        try {
            Prestamo nuevoPrestamo = prestamoService.registrarPrestamo(prestamoDto);
            return new ResponseEntity<>(nuevoPrestamo, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno: " + e.getMessage());
        }
    }

    // Obtener Préstamo por ID
    @GetMapping("/{id}")
    public ResponseEntity<Prestamo> obtenerPorId(@PathVariable Integer id) {
        Optional<Prestamo> prestamo = prestamoService.obtenerPorId(id);
        return prestamo.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Obtener Cronograma Real (Desde Base de Datos)
    @GetMapping("/{id}/cronograma")
    public ResponseEntity<List<CuotaDto>> obtenerCronograma(@PathVariable Integer id) {
        try {
            List<CuotaDto> cuotas = prestamoService.obtenerCronograma(id);
            if (cuotas.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(cuotas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Listar todos los préstamos
    @GetMapping
    public List<Prestamo> listarPrestamos() {
        return prestamoService.listarPrestamos();
    }

    // --- 2. ENDPOINTS DE BÚSQUEDA (COMPATIBILIDAD) ---

    // Endpoint simplificado para buscar por DNI en URL
    @GetMapping("/cliente/{dni}")
    public List<Prestamo> listarPrestamosPorDni(@PathVariable String dni) {
        return prestamoService.buscarPorClienteDni(dni);
    }

    // Endpoint POST flexible (DNI + Estado opcional)
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

    // --- 3. ENDPOINTS DE DOCUMENTOS (PDF) ---

    // Generar Contrato PDF (Usando datos reales del préstamo)
    @GetMapping("/{id}/contrato-pdf")
    public ResponseEntity<?> generarContratoPDF(@PathVariable Integer id) {
        try {
            Prestamo prestamo = prestamoService.obtenerPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado con ID: " + id));

            // Generar PDF usando el generador utilitario
            byte[] pdfBytes = DocumentoGenerator.generarContratoPrestamo(prestamo);

            String identificador = prestamo.getCliente().getTipoCliente().equals("JURIDICA")
                    ? prestamo.getCliente().getRuc()
                    : prestamo.getCliente().getDniCliente();

            String filename = "Contrato_Prestamo_" + identificador + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al generar contrato: " + e.getMessage());
        }
    }

    // Generar Cronograma PDF (Visualización imprimible)
    @GetMapping("/{id}/cronograma-pdf")
    public ResponseEntity<?> generarCronogramaPDF(@PathVariable Integer id) {
        try {
            Prestamo prestamo = prestamoService.obtenerPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado con ID: " + id));

            Cliente cliente = prestamo.getCliente();
            String nombreCliente = cliente.getTipoCliente().equals("JURIDICA")
                    ? cliente.getRazonSocial()
                    : cliente.getNombreCliente() + " " + cliente.getApellidoCliente();

            String identificador = cliente.getTipoCliente().equals("JURIDICA") ? cliente.getRuc() : cliente.getDniCliente();

            byte[] pdfBytes = DocumentoGenerator.generarCronogramaPDF(
                    nombreCliente,
                    prestamo.getMonto(),
                    prestamo.getTasaInteresAnual(),
                    prestamo.getNumeroCuotas()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=Cronograma_" + identificador + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al generar cronograma PDF: " + e.getMessage());
        }
    }

    // Generar Declaración Jurada PEP (Antes del préstamo)
    @PostMapping("/documentos/declaracion-pep")
    public ResponseEntity<byte[]> generarDeclaracionPep(@RequestBody ClienteDetalleDto clienteDto) {
        try {
            byte[] pdf = pdfService.generarDeclaracionPEPPorDatos(clienteDto);
            String id = clienteDto.getDniCliente() != null ? clienteDto.getDniCliente() : clienteDto.getRuc();
            String filename = "Declaracion_PEP_" + id + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Generar Declaración Jurada UIT (Antes del préstamo)
    @PostMapping("/documentos/declaracion-uit")
    public ResponseEntity<byte[]> generarDeclaracionUit(@RequestBody DeclaracionUITRequestDto request) {
        try {
            ClienteDetalleDto clienteDto = request.getClient();
            double monto = request.getAmount();

            byte[] pdf = pdfService.generarDeclaracionUITPorDatos(clienteDto, monto);

            String id = clienteDto.getDniCliente() != null ? clienteDto.getDniCliente() : clienteDto.getRuc();
            String filename = "Declaracion_Jurada_UIT_" + id + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        }
    }
}