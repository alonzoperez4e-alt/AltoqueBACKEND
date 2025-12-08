package com.altoque.altoque.Service.Impl;

import com.altoque.altoque.Dto.CuotaDto;
import com.altoque.altoque.Dto.PrestamoDto;
import com.altoque.altoque.Entity.Cliente;
import com.altoque.altoque.Entity.Cuota;
import com.altoque.altoque.Entity.Prestamo;
import com.altoque.altoque.Exception.ApiClientException;
import com.altoque.altoque.Repository.ClienteRepository;
import com.altoque.altoque.Repository.CuotaRepository;
import com.altoque.altoque.Repository.PrestamoRepository;
import com.altoque.altoque.Service.PrestamoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PrestamoServiceImpl implements PrestamoService {

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    private static final double UIT = 5150.00;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Override
    public List<Prestamo> listarPrestamos() {
        return prestamoRepository.findAll();
    }

    @Override
    public Optional<Prestamo> obtenerPorId(Integer id) {
        return prestamoRepository.findById(id);
    }

    @Override
    @Transactional // Importante: Asegura que si falla la generación de cuotas, no se guarde el préstamo
    public Prestamo registrarPrestamo(PrestamoDto dto) {
        // Validar cliente
        Cliente cliente = clienteRepository.findById(dto.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Validar si ya tiene préstamo activo (Regla de negocio)
        boolean tieneActivo = prestamoRepository.existsByCliente_IdClienteAndEstado(dto.getIdCliente(), "activo");
        if (tieneActivo) {
            throw new IllegalArgumentException("El cliente ya tiene un préstamo activo.");
        }

        // Validaciones numéricas
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

        // Declaraciones según condiciones (Lógica existente)
        if (dto.getMonto() > UIT) prestamo.setDeclaracionImpresa(true);
        if (cliente.getEsPep() != null && cliente.getEsPep()) prestamo.setDeclaracionPepImpresa(true);

        // 1. Guardar Préstamo primero para obtener ID
        Prestamo prestamoGuardado = prestamoRepository.save(prestamo);

        // 2. Generar Cronograma (Método Francés) y guardar cuotas
        generarYGuardarCuotas(prestamoGuardado);

        return prestamoGuardado;
    }

    // Lógica financiera: Método Francés (Cuota Fija)
    private void generarYGuardarCuotas(Prestamo prestamo) {
        double monto = prestamo.getMonto();
        double tasaAnual = prestamo.getTasaInteresAnual();
        int nCuotas = prestamo.getNumeroCuotas();

        // Tasa Mensual = (Tasa Anual / 12) / 100
        double tasaMensual = (tasaAnual / 12.0) / 100.0;

        // Fórmula Cuota Fija (Método Francés)
        // C = P * [ i / (1 - (1+i)^-n) ]
        double cuotaFija;
        if (tasaMensual > 0) {
            cuotaFija = (monto * tasaMensual) / (1 - Math.pow(1 + tasaMensual, -nCuotas));
        } else {
            cuotaFija = monto / nCuotas; // Caso borde tasa 0%
        }

        // Redondeo a 2 decimales para consistencia financiera
        cuotaFija = Math.round(cuotaFija * 100.0) / 100.0;

        List<Cuota> cuotas = new ArrayList<>();
        LocalDate fechaVencimiento = prestamo.getFechaPrestamo();

        for (int i = 1; i <= nCuotas; i++) {
            fechaVencimiento = fechaVencimiento.plusMonths(1); // Vence cada mes siguiente a la fecha del préstamo

            // Creamos la entidad Cuota
            // Nota: Se asume que tu entidad Cuota tiene un constructor adecuado o setters
            Cuota cuota = new Cuota();
            cuota.setPrestamo(prestamo);
            cuota.setNumeroCuota(i);
            cuota.setFechaVencimiento(fechaVencimiento);
            cuota.setMontoProgramado(cuotaFija);
            cuota.setEstado("PENDIENTE");
            cuota.setMontoPagado(0.0);
            cuota.setInteresMora(0.0);

            cuotas.add(cuota);
        }

        cuotaRepository.saveAll(cuotas);
    }

    // Nuevo método para exponer el cronograma al Frontend
    public List<CuotaDto> obtenerCronograma(Integer idPrestamo) {
        // Asumimos que tienes el método findByPrestamo_IdPrestamoOrderByNumeroCuotaAsc en CuotaRepository
        return cuotaRepository.findByPrestamo_IdPrestamoOrderByNumeroCuotaAsc(idPrestamo)
                .stream()
                .map(c -> {
                    CuotaDto dto = new CuotaDto();
                    dto.setIdCuota(c.getIdCuota());
                    dto.setNumeroCuota(c.getNumeroCuota());
                    dto.setFechaVencimiento(c.getFechaVencimiento());
                    dto.setMontoProgramado(c.getMontoProgramado());
                    dto.setMontoPagado(c.getMontoPagado());
                    dto.setInteresMora(c.getInteresMora());
                    dto.setEstado(c.getEstado());
                    // Calculamos el saldo pendiente real (Monto + Mora - Pagado)
                    double total = (c.getMontoProgramado() + (c.getInteresMora() != null ? c.getInteresMora() : 0))
                            - (c.getMontoPagado() != null ? c.getMontoPagado() : 0);
                    dto.setTotalExigible(Math.max(0, total));
                    return dto;
                })
                .collect(Collectors.toList());
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
        Optional<Cliente> clienteOpt = clienteRepository.findByDniCliente(dniCliente);
        if (clienteOpt.isEmpty()) {
            return List.of();
        }
        return prestamoRepository.findByCliente_IdCliente(clienteOpt.get().getIdCliente());
    }

    @Override
    public List<Prestamo> buscarPorClienteDniYEstado(String dniCliente, String estado) {
        Optional<Cliente> clienteOpt = clienteRepository.findByDniCliente(dniCliente);
        if (clienteOpt.isEmpty()) {
            return List.of();
        }
        return prestamoRepository.findByCliente_IdClienteAndEstado(clienteOpt.get().getIdCliente(), estado);
    }
}