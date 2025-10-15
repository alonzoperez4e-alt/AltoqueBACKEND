package com.altoque.altoque.Service;

import com.altoque.altoque.Dto.ClienteConsultaDto;
import com.altoque.altoque.Dto.ClienteDetalleDto; // NEW: Import DTO
import com.altoque.altoque.Entity.Cliente;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Optional;

public interface ClienteService {

    // --- Métodos existentes ---
    Mono<ClienteConsultaDto> consultarClienteYEstadoPrestamo(String dni);
    List<Cliente> listarClientes();
    Optional<Cliente> obtenerPorId(Integer id);
    Optional<Cliente> obtenerPorDni(String dniCliente);
    Cliente guardar (Cliente cliente); // Este se usará internamente
    boolean existePorDniCliente(String dniCliente);
    Mono<Cliente> consultarApiExternaAsync(String dni);

    // --- NUEVOS MÉTODOS PARA LA LÓGICA DEL FORMULARIO ---

    /**
     * Prepara los datos para el formulario de cliente.
     * Si el cliente ya existe en la BD local, retorna sus datos.
     * Si no, consulta la API externa para obtener nombre y apellido.
     * @param dni El DNI del cliente.
     * @return Un Mono que emite un ClienteDetalleDto.
     */
    Mono<ClienteDetalleDto> obtenerOPrepararClienteParaFormulario(String dni);

    /**
     * Guarda un cliente nuevo o actualiza uno existente basado en el DNI.
     * @param clienteData Los datos del cliente provenientes del formulario.
     * @return El cliente persistido en la base de datos.
     */
    Cliente registrarOActualizar(ClienteDetalleDto clienteData);
}
