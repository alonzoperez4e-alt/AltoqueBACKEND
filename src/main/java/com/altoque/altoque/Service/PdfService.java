package com.altoque.altoque.Service;

import com.altoque.altoque.Dto.ClienteDetalleDto;
import com.altoque.altoque.Entity.Cliente;
import com.altoque.altoque.Entity.Prestamo;
import com.altoque.altoque.Repository.ClienteRepository;
import com.altoque.altoque.Utils.DocumentoGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class PdfService {

    @Autowired
    private ClienteRepository clienteRepository;

    /**
     * Genera la declaración PEP utilizando los datos temporales del formulario,
     * sin necesidad de que el cliente esté guardado en la base de datos.
     * Esta es la solución para el error 500.
     * @param dto Los datos del cliente que vienen del frontend.
     * @return El PDF generado en bytes.
     */
    public byte[] generarDeclaracionPEPPorDatos(ClienteDetalleDto dto) {
        // 1. Se crea una entidad 'Cliente' temporal con los datos del DTO.
        Cliente clienteTemporal = new Cliente();
        clienteTemporal.setDniCliente(dto.getDniCliente());
        clienteTemporal.setNombreCliente(dto.getNombreCliente());
        clienteTemporal.setApellidoCliente(dto.getApellidoCliente());
        clienteTemporal.setDireccionCliente(dto.getDireccionCliente());
        clienteTemporal.setCorreoCliente(dto.getCorreoCliente());
        clienteTemporal.setTelefonoCliente(dto.getTelefonoCliente());
        clienteTemporal.setEsPep(dto.getEsPep());

        // 2. Se realiza la misma conversión de fecha que en ClienteServiceImpl.
        // Esto asegura que, si el usuario ingresó una fecha, se maneje correctamente.
        String fechaNacimientoStr = dto.getFechaNacimiento();
        if (fechaNacimientoStr != null && !fechaNacimientoStr.trim().isEmpty()) {
            LocalDate localDate = LocalDate.parse(fechaNacimientoStr);
            clienteTemporal.setFechaNacimiento(localDate.atStartOfDay());
        }

        // 3. Se pasa la entidad temporal al generador de documentos.
        return DocumentoGenerator.generarDeclaracionPEP(clienteTemporal);
    }

    /**
     * Genera la declaración PEP para un cliente que ya existe en la base de datos.
     * @param dni El DNI del cliente a buscar.
     * @return El PDF generado en bytes.
     */
    public byte[] generarDeclaracionPEPPorDni(String dni) {
        Cliente cliente = clienteRepository.findByDniCliente(dni)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con DNI: " + dni));
        return DocumentoGenerator.generarDeclaracionPEP(cliente);
    }
/*
    /**
     * Genera el contrato de préstamo para un cliente existente.
     * @param dni El DNI del cliente.
     * @param monto El monto del préstamo.
     * @return El PDF del contrato en bytes.

    public byte[] generarContratoPorDni(String dni, double monto) {
        Cliente cliente = clienteRepository.findByDniCliente(dni)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return DocumentoGenerator.generarContratoPrestamo(cliente, monto);
    }
    */

    // --- NUEVO MÉTODO AÑADIDO ---
    public byte[] generarDeclaracionUITPorDatos(ClienteDetalleDto clienteDto, double monto) {
        // 1. Convierte el DTO del cliente a una entidad Cliente temporal.
        Cliente clienteTemporal = mapDtoToEntity(clienteDto);

        // 2. Crea una entidad Préstamo temporal solo con el monto necesario.
        Prestamo prestamoTemporal = new Prestamo();
        prestamoTemporal.setMonto(monto);

        // 3. Llama al generador de documentos con las entidades temporales.
        return DocumentoGenerator.generarDeclaracionUIT(clienteTemporal, prestamoTemporal);
    }


    // --- FUNCIÓN DE AYUDA ---
    // Convierte un DTO a una entidad para poder usarla en el generador de PDFs.
    private Cliente mapDtoToEntity(ClienteDetalleDto dto) {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(dto.getIdCliente());
        cliente.setDniCliente(dto.getDniCliente());
        cliente.setNombreCliente(dto.getNombreCliente());
        cliente.setApellidoCliente(dto.getApellidoCliente());
        cliente.setCorreoCliente(dto.getCorreoCliente());
        cliente.setTelefonoCliente(dto.getTelefonoCliente());
        cliente.setDireccionCliente(dto.getDireccionCliente());
        cliente.setEsPep(dto.getEsPep());

        if (dto.getFechaNacimiento() != null && !dto.getFechaNacimiento().isEmpty()) {
            cliente.setFechaNacimiento(LocalDate.parse(dto.getFechaNacimiento()).atStartOfDay());
        }
        return cliente;
    }

}