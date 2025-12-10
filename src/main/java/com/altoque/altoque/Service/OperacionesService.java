package com.altoque.altoque.Service;

import com.altoque.altoque.Dto.Payment.EstadoCuentaDto;
import com.altoque.altoque.Dto.Payment.PagoRequestDto;
import com.altoque.altoque.Dto.Payment.PagoResponseDto;
import com.altoque.altoque.Entity.Cliente;
import com.altoque.altoque.Entity.Cuota;
import com.altoque.altoque.Entity.Prestamo;
import com.altoque.altoque.Repository.CuotaRepository;
import com.altoque.altoque.Repository.PrestamoRepository;
import com.altoque.altoque.Exception.ApiServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class OperacionesService {

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    // Constante para el interés moratorio (1%)
    private static final BigDecimal MORA_PORCENTAJE = new BigDecimal("0.01");

    /**
     * Obtiene el estado de cuenta calculado dinámicamente.
     */
    public EstadoCuentaDto obtenerEstadoCuenta(Integer clienteId) {
        // Buscar prestamo activo del cliente
        Prestamo prestamo = prestamoRepository.findAll().stream()
                .filter(p -> p.getCliente().getIdCliente().equals(clienteId))
                .findFirst()
                .orElseThrow(() -> new ApiServerException("No se encontró préstamo para el cliente"));

        return calcularEstadoCuenta(prestamo);
    }

    private EstadoCuentaDto calcularEstadoCuenta(Prestamo prestamo) {
        EstadoCuentaDto dto = new EstadoCuentaDto();
        Cliente cliente = prestamo.getCliente();

        dto.setPrestamoId(prestamo.getIdPrestamo());
        dto.setTipoCliente(cliente.getTipoCliente());
        dto.setClienteNombre(cliente.getNombreCliente() + " " + cliente.getApellidoCliente());
        dto.setRazonSocial(cliente.getRazonSocial());

        dto.setDocumento(cliente.getDniCliente() != null ? cliente.getDniCliente() : cliente.getRuc());

        List<Cuota> cuotas = cuotaRepository.buscarPorPrestamoId(prestamo.getIdPrestamo());
        cuotas.sort(Comparator.comparingInt(Cuota::getNumeroCuota));

        List<EstadoCuentaDto.CuotaEstadoDto> cuotasDto = new ArrayList<>();
        BigDecimal deudaOriginalTotal = BigDecimal.ZERO;
        BigDecimal deudaPendienteTotal = BigDecimal.ZERO;

        for (Cuota cuota : cuotas) {
            EstadoCuentaDto.CuotaEstadoDto cDto = new EstadoCuentaDto.CuotaEstadoDto();

            // CONVERSIÓN SEGURA: Double (Entidad) -> BigDecimal (Lógica)
            BigDecimal montoOriginal = BigDecimal.valueOf(cuota.getMontoProgramado());
            BigDecimal montoPagado = cuota.getMontoPagado() != null
                    ? BigDecimal.valueOf(cuota.getMontoPagado())
                    : BigDecimal.ZERO;

            BigDecimal saldoPendiente = montoOriginal.subtract(montoPagado);

            // Ajuste por precisión: si el saldo es infinitesimalmente pequeño (ej. 0.0000001), lo tratamos como 0
            if (saldoPendiente.abs().compareTo(new BigDecimal("0.001")) < 0) {
                saldoPendiente = BigDecimal.ZERO;
            } else {
                // Redondear a 2 decimales para visualización limpia
                saldoPendiente = saldoPendiente.setScale(2, RoundingMode.HALF_UP);
            }

            // Lógica de Mora
            BigDecimal mora = BigDecimal.ZERO;
            boolean vencida = cuota.getFechaVencimiento().isBefore(LocalDate.now());

            if (vencida && saldoPendiente.compareTo(BigDecimal.ZERO) > 0) {
                mora = saldoPendiente.multiply(MORA_PORCENTAJE).setScale(2, RoundingMode.HALF_UP);
            }

            // Determinación de Estado
            String estado;
            if (saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
                estado = "Pagado";
                mora = BigDecimal.ZERO;
            } else if (vencida) {
                estado = "Mora Activa (1%)";
            } else if (montoPagado.compareTo(BigDecimal.ZERO) > 0) {
                estado = "Adelanto de cuota";
            } else {
                estado = "Pendiente de pago";
            }

            cDto.setId(cuota.getIdCuota());
            cDto.setNumeroCuota(cuota.getNumeroCuota());
            cDto.setFechaVencimiento(cuota.getFechaVencimiento());
            cDto.setCuotaOriginal(montoOriginal);
            cDto.setSaldoPendiente(saldoPendiente);
            cDto.setMoraGenerada(mora);
            cDto.setEstado(estado);
            cDto.setTotalAPagar(saldoPendiente.add(mora));

            cuotasDto.add(cDto);

            deudaOriginalTotal = deudaOriginalTotal.add(montoOriginal);
            deudaPendienteTotal = deudaPendienteTotal.add(cDto.getTotalAPagar());
        }

        dto.setCuotas(cuotasDto);
        dto.setDeudaOriginalTotal(deudaOriginalTotal);
        dto.setDeudaPendienteTotal(deudaPendienteTotal);

        return dto;
    }

    /**
     * Procesa un pago distribuyendo el monto tipo Cascada (Waterfall).
     */
    @Transactional
    public PagoResponseDto procesarPago(PagoRequestDto request) {
        Prestamo prestamo = prestamoRepository.findById(request.getPrestamoId())
                .orElseThrow(() -> new ApiServerException("Préstamo no encontrado"));

        List<Cuota> cuotas = cuotaRepository.buscarPorPrestamoId(prestamo.getIdPrestamo());
        cuotas.sort(Comparator.comparingInt(Cuota::getNumeroCuota));

        BigDecimal dineroDisponible = request.getMonto();
        List<String> coberturaLog = new ArrayList<>();

        if (dineroDisponible.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiServerException("El monto debe ser mayor a 0");
        }

        for (Cuota cuota : cuotas) {
            if (dineroDisponible.compareTo(BigDecimal.ZERO) == 0) break;

            // CONVERSIÓN SEGURA
            BigDecimal montoCuota = BigDecimal.valueOf(cuota.getMontoProgramado());
            BigDecimal montoPagadoActual = cuota.getMontoPagado() != null
                    ? BigDecimal.valueOf(cuota.getMontoPagado())
                    : BigDecimal.ZERO;

            BigDecimal saldoCapital = montoCuota.subtract(montoPagadoActual);

            // Limpieza de residuos decimales
            if (saldoCapital.abs().compareTo(new BigDecimal("0.001")) < 0) {
                saldoCapital = BigDecimal.ZERO;
            }

            // Calcular Mora
            BigDecimal moraCalculada = BigDecimal.ZERO;
            if (cuota.getFechaVencimiento().isBefore(LocalDate.now()) && saldoCapital.compareTo(BigDecimal.ZERO) > 0) {
                moraCalculada = saldoCapital.multiply(MORA_PORCENTAJE).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal deudaTotalCuota = saldoCapital.add(moraCalculada);

            // Si la deuda es cero (o casi cero), saltamos
            if (deudaTotalCuota.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Lógica de aplicación de pago (Waterfall)
            if (dineroDisponible.compareTo(deudaTotalCuota) >= 0) {
                // Pago COMPLETO de cuota + mora

                // Al guardar en entidad Double, seteamos el monto original completo
                cuota.setMontoPagado(montoCuota.doubleValue());
                cuota.setEstado("Pagado");

                dineroDisponible = dineroDisponible.subtract(deudaTotalCuota);

                coberturaLog.add("Cuota " + cuota.getNumeroCuota() + " (Completa) - S/ " + deudaTotalCuota);

            } else {
                // Pago PARCIAL
                BigDecimal pagoParaMora = dineroDisponible.min(moraCalculada);
                BigDecimal remanenteParaCapital = dineroDisponible.subtract(pagoParaMora);

                BigDecimal nuevoPagado = montoPagadoActual.add(remanenteParaCapital);

                // CONVERSIÓN DE VUELTA: BigDecimal -> Double para la entidad
                cuota.setMontoPagado(nuevoPagado.doubleValue());

                if (cuota.getFechaVencimiento().isBefore(LocalDate.now())) {
                    cuota.setEstado("Mora Activa (1%)");
                } else {
                    cuota.setEstado("Adelanto de cuota");
                }

                coberturaLog.add("Cuota " + cuota.getNumeroCuota() + " (Parcial) - S/ " + dineroDisponible);
                dineroDisponible = BigDecimal.ZERO;
            }

            cuotaRepository.save(cuota);
        }

        PagoResponseDto response = new PagoResponseDto();
        response.setMensaje("Pago procesado exitosamente");
        response.setMontoAplicado(request.getMonto().subtract(dineroDisponible));
        response.setDetallesCobertura(coberturaLog);

        EstadoCuentaDto nuevoEstado = calcularEstadoCuenta(prestamo);
        response.setDeudaRestante(nuevoEstado.getDeudaPendienteTotal());

        return response;
    }
}