package com.altoque.altoque.Service.Impl;

import com.altoque.altoque.Dto.PrestamoDto;
import com.altoque.altoque.Entity.Cliente;
import com.altoque.altoque.Entity.Prestamo;
import com.altoque.altoque.Repository.ClienteRepository;
import com.altoque.altoque.Repository.PrestamoRepository;
import com.altoque.altoque.Service.PrestamoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PrestamoServiceImpl implements PrestamoService {

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    private static final double UIT = 5150.00;

    @Override
    public List<Prestamo> listarPrestamos() {
        return prestamoRepository.findAll();
    }

    @Override
    public Optional<Prestamo> obtenerPorId(Integer id) {
        return prestamoRepository.findById(id);
    }

    public Prestamo registrarPrestamo(PrestamoDto dto) {
        // Validar cliente
        Cliente cliente = clienteRepository.findById(dto.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Validaciones
        if (dto.getMonto() <= 0 || dto.getMonto() > 150000)
            throw new IllegalArgumentException("Monto inválido. Debe ser >0 y <=150,000");

        if (dto.getTasaInteresAnual() <= 0 || dto.getTasaInteresAnual() > 100)
            throw new IllegalArgumentException("Interés anual inválido. Debe ser >0 y <=100");

        if (dto.getNumeroCuotas() <= 0 || dto.getNumeroCuotas() > 60)
            throw new IllegalArgumentException("Número de cuotas inválido. Máximo 60");

        if (dto.getFechaPrestamo().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("La fecha del préstamo no puede ser anterior al día actual");

        // Crear préstamo
        Prestamo prestamo = new Prestamo();
        prestamo.setCliente(cliente);
        prestamo.setMonto(dto.getMonto());
        prestamo.setTasaInteresAnual(dto.getTasaInteresAnual());
        prestamo.setNumeroCuotas(dto.getNumeroCuotas());
        prestamo.setFechaPrestamo(dto.getFechaPrestamo());
        prestamo.setEstado("activo");

        // Declaraciones según condiciones
        if (dto.getMonto() > UIT) prestamo.setDeclaracionImpresa(true);
        if (cliente.getEsPep() != null && cliente.getEsPep()) prestamo.setDeclaracionPepImpresa(true);

        return prestamoRepository.save(prestamo);
    }

    @Override
    public Prestamo actualizarPrestamo(Prestamo prestamo) {
        return prestamoRepository.save(prestamo);
    }

    @Override
    public void eliminarPrestamo(Integer id) {
        prestamoRepository.deleteById(id);
    }
    // ======= Nuevos métodos para buscar por DNI =======
    @Override
    public List<Prestamo> buscarPorClienteId(Integer idCliente) {
        return prestamoRepository.findByCliente_IdCliente(idCliente);
    }

    @Override
    public List<Prestamo> buscarPorClienteDni(String dniCliente) {
        Optional<Cliente> clienteOpt = clienteRepository.findClientesByDniCliente(dniCliente);
        if (clienteOpt.isEmpty()) {
            return List.of();
        }
        return prestamoRepository.findByCliente_IdCliente(clienteOpt.get().getIdCliente());
    }

    @Override
    public List<Prestamo> buscarPorClienteDniYEstado(String dniCliente, String estado) {
        Optional<Cliente> clienteOpt = clienteRepository.findClientesByDniCliente(dniCliente);
        if (clienteOpt.isEmpty()) {
            return List.of();
        }
        return prestamoRepository.findByCliente_IdClienteAndEstado(clienteOpt.get().getIdCliente(), estado);
    }
}