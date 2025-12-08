package com.altoque.altoque.Service.Impl;

import com.altoque.altoque.Dto.ApiPeruResponseDto;
import com.altoque.altoque.Dto.ClienteConsultaDto;
import com.altoque.altoque.Dto.ClienteDetalleDto;
import com.altoque.altoque.Dto.DniRequestDto;
import com.altoque.altoque.Entity.Cliente;
import com.altoque.altoque.Exception.ApiClientException;
import com.altoque.altoque.Exception.ApiServerException;
import com.altoque.altoque.Repository.ClienteRepository;
import com.altoque.altoque.Repository.PrestamoRepository;
import com.altoque.altoque.Service.ClienteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteServiceImpl implements ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    private static final Logger log = LoggerFactory.getLogger(ClienteServiceImpl.class);
    private final WebClient webClient;
    private final String apiPeruToken;

    @Autowired
    public ClienteServiceImpl(
            ClienteRepository clienteRepository,
            PrestamoRepository prestamoRepository,
            WebClient.Builder webClientBuilder,
            @Value("${api.peru.baseurl}") String apiPeruBaseUrl,
            @Value("${api.peru.token}") String apiPeruToken
    ) {
        this.clienteRepository = clienteRepository;
        this.prestamoRepository = prestamoRepository;
        this.apiPeruToken = apiPeruToken;
        this.webClient = webClientBuilder.baseUrl(apiPeruBaseUrl).build();
    }

    @Override
    public Mono<ClienteConsultaDto> consultarClienteYEstadoPrestamo(String documento) {
        // Lógica unificada para el Dashboard: detecta si es DNI o RUC
        if (documento.length() == 8) {
            return consultarPorDni(documento);
        } else if (documento.length() == 11) {
            return consultarPorRuc(documento);
        } else {
            return Mono.error(new IllegalArgumentException("Longitud de documento inválida"));
        }
    }

    // Lógica DNI (Existente + API Externa)
    private Mono<ClienteConsultaDto> consultarPorDni(String dni) {
        return this.consultarApiExternaAsync(dni)
                .flatMap(clienteExterno -> {
                    Optional<Cliente> clienteLocalOpt = clienteRepository.findByDniCliente(dni);
                    boolean tieneActivo = clienteLocalOpt
                            .map(cliente -> prestamoRepository.existsByCliente_IdClienteAndEstado(cliente.getIdCliente(), "activo"))
                            .orElse(false);

                    ClienteConsultaDto dto = new ClienteConsultaDto(
                            clienteExterno.getDniCliente(),
                            clienteExterno.getNombreCliente(),
                            clienteExterno.getApellidoCliente(),
                            tieneActivo
                    );
                    dto.setTipo("NATURAL");
                    return Mono.just(dto);
                });
    }

    // Lógica RUC (Búsqueda solo en BD Local por ahora - Mock si no existe)
    private Mono<ClienteConsultaDto> consultarPorRuc(String ruc) {
        Optional<Cliente> clienteLocalOpt = clienteRepository.findByRuc(ruc);

        if (clienteLocalOpt.isPresent()) {
            Cliente c = clienteLocalOpt.get();
            boolean tieneActivo = prestamoRepository.existsByCliente_IdClienteAndEstado(c.getIdCliente(), "activo");

            ClienteConsultaDto dto = new ClienteConsultaDto();
            dto.setRuc(c.getRuc());
            dto.setRazonSocial(c.getRazonSocial());
            dto.setTipo("JURIDICA");
            dto.setTienePrestamoActivo(tieneActivo);

            return Mono.just(dto);
        } else {
            // Si no existe, devolvemos vacío para que el frontend habilite el registro
            return Mono.empty();
        }
    }

    @Override
    public Mono<ClienteDetalleDto> obtenerOPrepararClienteParaFormulario(String documento) {
        if (documento.length() == 11) {
            // Lógica RUC
            Optional<Cliente> clienteOpt = clienteRepository.findByRuc(documento);
            if (clienteOpt.isPresent()) {
                return Mono.just(mapToDetalleDto(clienteOpt.get()));
            } else {
                // Retornar DTO vacío preparado para registro nuevo
                ClienteDetalleDto dto = new ClienteDetalleDto();
                dto.setRuc(documento);
                dto.setTipo("JURIDICA");
                dto.setEsNuevo(true);
                return Mono.just(dto);
            }
        } else {
            // Lógica DNI (Existente)
            Optional<Cliente> clienteOpt = clienteRepository.findByDniCliente(documento);
            if (clienteOpt.isPresent()) {
                return Mono.just(mapToDetalleDto(clienteOpt.get()));
            } else {
                return consultarApiExternaAsync(documento).map(c -> {
                    ClienteDetalleDto dto = new ClienteDetalleDto();
                    dto.setDniCliente(c.getDniCliente());
                    dto.setNombreCliente(c.getNombreCliente());
                    dto.setApellidoCliente(c.getApellidoCliente());
                    dto.setTipo("NATURAL");
                    dto.setEsNuevo(true);
                    return dto;
                });
            }
        }
    }

    @Override
    public Cliente registrarOActualizar(ClienteDetalleDto dto) {
        Cliente cliente;

        if ("JURIDICA".equals(dto.getTipo()) || (dto.getRuc() != null && !dto.getRuc().isEmpty())) {
            // Registro JURIDICO
            cliente = clienteRepository.findByRuc(dto.getRuc()).orElseGet(Cliente::new);
            cliente.setTipoCliente("JURIDICA");
            cliente.setRuc(dto.getRuc());
            cliente.setRazonSocial(dto.getRazonSocial());
            cliente.setDireccionFiscal(dto.getDireccionFiscal());
            cliente.setRepresentanteLegalDni(dto.getRepresentanteLegalDni());
            cliente.setRepresentanteLegalNombre(dto.getRepresentanteLegalNombre());

            if (dto.getFechaConstitucion() != null && !dto.getFechaConstitucion().trim().isEmpty()) {
                cliente.setFechaConstitucion(LocalDate.parse(dto.getFechaConstitucion()).atStartOfDay());
            }
        } else {
            // Registro NATURAL
            cliente = clienteRepository.findByDniCliente(dto.getDniCliente()).orElseGet(Cliente::new);
            cliente.setTipoCliente("NATURAL");
            cliente.setDniCliente(dto.getDniCliente());
            cliente.setNombreCliente(dto.getNombreCliente());
            cliente.setApellidoCliente(dto.getApellidoCliente());

            if (dto.getFechaNacimiento() != null && !dto.getFechaNacimiento().trim().isEmpty()) {
                cliente.setFechaNacimiento(LocalDate.parse(dto.getFechaNacimiento()).atStartOfDay());
            }
        }

        // Datos Comunes
        cliente.setDireccionCliente(dto.getDireccionCliente());
        cliente.setCorreoCliente(dto.getCorreoCliente());
        cliente.setTelefonoCliente(dto.getTelefonoCliente());
        cliente.setEsPep(dto.getEsPep());

        return clienteRepository.save(cliente);
    }

    // Mapper auxiliar
    private ClienteDetalleDto mapToDetalleDto(Cliente c) {
        ClienteDetalleDto dto = new ClienteDetalleDto();
        dto.setIdCliente(c.getIdCliente());
        dto.setTipo(c.getTipoCliente());
        dto.setEsNuevo(false);
        dto.setDireccionCliente(c.getDireccionCliente());
        dto.setCorreoCliente(c.getCorreoCliente());
        dto.setTelefonoCliente(c.getTelefonoCliente());
        dto.setEsPep(c.getEsPep());

        if ("JURIDICA".equals(c.getTipoCliente())) {
            dto.setRuc(c.getRuc());
            dto.setRazonSocial(c.getRazonSocial());
            dto.setDireccionFiscal(c.getDireccionFiscal());
            dto.setRepresentanteLegalDni(c.getRepresentanteLegalDni());
            dto.setRepresentanteLegalNombre(c.getRepresentanteLegalNombre());
            if (c.getFechaConstitucion() != null) dto.setFechaConstitucion(c.getFechaConstitucion().toLocalDate().toString());
        } else {
            dto.setDniCliente(c.getDniCliente());
            dto.setNombreCliente(c.getNombreCliente());
            dto.setApellidoCliente(c.getApellidoCliente());
            if (c.getFechaNacimiento() != null) dto.setFechaNacimiento(c.getFechaNacimiento().toLocalDate().toString());
        }
        return dto;
    }

    // --- MÉTODOS EXISTENTES SIN CAMBIOS ---
    @Override
    public List<Cliente> listarClientes() { return clienteRepository.findAll(); }
    @Override
    public Optional<Cliente> obtenerPorId(Integer id) { return clienteRepository.findById(id); }
    @Override
    public Optional<Cliente> obtenerPorDni(String dniCliente) { return clienteRepository.findByDniCliente(dniCliente); }
    @Override
    public Cliente guardar(Cliente cliente) { return clienteRepository.save(cliente); }
    @Override
    public boolean existePorDniCliente(String dniCliente) { return clienteRepository.existsByDniCliente(dniCliente); }

    @Override
    public Mono<Cliente> consultarApiExternaAsync(String dni) {
        DniRequestDto requestBody = new DniRequestDto(dni);
        return webClient.post()
                .uri("")
                .header("Authorization", "Bearer " + apiPeruToken)
                .header("Content-Type", "application/json")
                .body(Mono.just(requestBody), DniRequestDto.class)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new ApiClientException("DNI no encontrado o solicitud inválida.")))
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new ApiServerException("El servicio externo de DNI no está disponible.")))
                .bodyToMono(ApiPeruResponseDto.class)
                .filter(response -> response.isSuccess() && response.getData() != null)
                .map(apiResponse -> {
                    ApiPeruResponseDto.Data data = apiResponse.getData();
                    Cliente cliente = new Cliente();
                    cliente.setDniCliente(data.getNumero());
                    cliente.setNombreCliente(data.getNombres());
                    cliente.setApellidoCliente(data.getApellido_paterno() + " " + data.getApellido_materno());
                    return cliente;
                })
                .doOnError(error -> log.error("Error en la consulta de DNI: {}", error.getMessage()));
    }
}