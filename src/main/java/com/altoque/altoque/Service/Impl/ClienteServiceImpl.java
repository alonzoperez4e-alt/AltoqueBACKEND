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
    public Mono<ClienteConsultaDto> consultarClienteYEstadoPrestamo(String dni) {
        // ... (Este método no necesita cambios)
        return this.consultarApiExternaAsync(dni)
                .flatMap(clienteExterno -> {
                    Optional<Cliente> clienteLocalOpt = clienteRepository.findByDniCliente(dni);
                    boolean tieneActivo = clienteLocalOpt
                            .map(cliente -> prestamoRepository.existsByCliente_IdClienteAndEstado(cliente.getIdCliente(), "activo"))
                            .orElse(false);
                    return Mono.just(new ClienteConsultaDto(
                            clienteExterno.getDniCliente(),
                            clienteExterno.getNombreCliente(),
                            clienteExterno.getApellidoCliente(),
                            tieneActivo
                    ));
                });
    }

    @Override
    public Mono<ClienteDetalleDto> obtenerOPrepararClienteParaFormulario(String dni) {
        Optional<Cliente> clienteOpt = clienteRepository.findByDniCliente(dni);

        if (clienteOpt.isPresent()) {
            Cliente c = clienteOpt.get();
            ClienteDetalleDto dto = new ClienteDetalleDto();
            dto.setIdCliente(c.getIdCliente());
            dto.setDniCliente(c.getDniCliente());
            dto.setNombreCliente(c.getNombreCliente());
            dto.setApellidoCliente(c.getApellidoCliente());
            // --- INICIO DE LA CORRECCIÓN ---
            // Convierte LocalDateTime de la entidad a String para el DTO
            if (c.getFechaNacimiento() != null) {
                dto.setFechaNacimiento(c.getFechaNacimiento().toLocalDate().toString());
            }
            // --- FIN DE LA CORRECCIÓN ---
            dto.setEsPep(c.getEsPep());
            dto.setCorreoCliente(c.getCorreoCliente());
            dto.setTelefonoCliente(c.getTelefonoCliente());
            dto.setDireccionCliente(c.getDireccionCliente());
            dto.setEsNuevo(false);
            return Mono.just(dto);
        } else {
            return consultarApiExternaAsync(dni).map(c -> {
                ClienteDetalleDto dto = new ClienteDetalleDto();
                dto.setDniCliente(c.getDniCliente());
                dto.setNombreCliente(c.getNombreCliente());
                dto.setApellidoCliente(c.getApellidoCliente());
                dto.setEsNuevo(true);
                return dto;
            });
        }
    }

    @Override
    public Cliente registrarOActualizar(ClienteDetalleDto clienteDto) {
        Cliente clienteParaGuardar = clienteRepository.findByDniCliente(clienteDto.getDniCliente())
                .orElseGet(Cliente::new);

        clienteParaGuardar.setDniCliente(clienteDto.getDniCliente());
        clienteParaGuardar.setNombreCliente(clienteDto.getNombreCliente());
        clienteParaGuardar.setApellidoCliente(clienteDto.getApellidoCliente());
        clienteParaGuardar.setDireccionCliente(clienteDto.getDireccionCliente());
        clienteParaGuardar.setCorreoCliente(clienteDto.getCorreoCliente());
        clienteParaGuardar.setTelefonoCliente(clienteDto.getTelefonoCliente());
        clienteParaGuardar.setEsPep(clienteDto.getEsPep());

        // --- INICIO DE LA CORRECCIÓN ---
        // Convierte el String que viene del DTO a LocalDateTime para la entidad
        String fechaNacimientoStr = clienteDto.getFechaNacimiento();
        if (fechaNacimientoStr != null && !fechaNacimientoStr.trim().isEmpty()) {
            LocalDate localDate = LocalDate.parse(fechaNacimientoStr);
            clienteParaGuardar.setFechaNacimiento(localDate.atStartOfDay());
        } else {
            clienteParaGuardar.setFechaNacimiento(null);
        }
        // --- FIN DE LA CORRECCIÓN ---

        return clienteRepository.save(clienteParaGuardar);
    }

    // --- MÉTODOS DE LA INTERFAZ QUE NO SE USAN EN ESTE FLUJO PERO DEBEN ESTAR ---

    @Override
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @Override
    public Optional<Cliente> obtenerPorId(Integer id) {
        return clienteRepository.findById(id);
    }

    @Override
    public Optional<Cliente> obtenerPorDni(String dniCliente) {
        return clienteRepository.findByDniCliente(dniCliente);
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    @Override
    public boolean existePorDniCliente(String dniCliente) {
        return clienteRepository.existsByDniCliente(dniCliente);
    }

    @Override
    public Mono<Cliente> consultarApiExternaAsync(String dni) {
        DniRequestDto requestBody = new DniRequestDto(dni);
        return webClient.post()
                .uri("")
                .header("Authorization", "Bearer " + apiPeruToken)
                .header("Content-Type", "application/json")
                .body(Mono.just(requestBody), DniRequestDto.class)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ApiClientException("DNI no encontrado o solicitud inválida."))
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new ApiServerException("El servicio externo de DNI no está disponible."))
                )
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