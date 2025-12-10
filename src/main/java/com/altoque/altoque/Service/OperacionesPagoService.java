package com.altoque.altoque.Service;

import com.altoque.altoque.Dto.Payment.EstadoCuentaDto;
import com.altoque.altoque.Dto.Payment.PagoRequestDto;
import com.altoque.altoque.Dto.Payment.PagoResponseDto;
import com.altoque.altoque.Entity.*;
import com.altoque.altoque.Repository.*;
import com.altoque.altoque.Exception.ApiServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class OperacionesPagoService {

    @Autowired
    private PrestamoRepository prestamoRepository;
    @Autowired
    private CuotaRepository cuotaRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private CajaRepository cajaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CajaService cajaService;

    // Constante de mora basada en tu código (1%)
    private static final BigDecimal MORA_PORCENTAJE = new BigDecimal("0.01");

    @Transactional
    public PagoResponseDto procesarPago(PagoRequestDto request, Integer userId) {
        // --- 1. VALIDACIONES INICIALES Y CAJA ---
        Caja cajaActual = cajaRepository.findByUsuario_IdUsuarioAndEstado(userId, "ABIERTA")
                .orElseThrow(() -> new ApiServerException("No se puede procesar pagos sin una caja abierta."));

        Prestamo prestamo = prestamoRepository.findById(request.getPrestamoId())
                .orElseThrow(() -> new ApiServerException("Préstamo no encontrado"));

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ApiServerException("Usuario no encontrado"));

        // Validar monto positivo
        if (request.getMonto().doubleValue() <= 0) {
            throw new ApiServerException("El monto debe ser mayor a 0");
        }

        // --- 2. CÁLCULO DE MONTOS Y REDONDEO (Lógica de Caja) ---
        // Monto Sistema: El valor exacto que el cliente quiere pagar de su deuda (ej: 10.54)
        Double montoSistemaDouble = request.getMonto().doubleValue();

        // Monto Real: Lo que entra físicamente a la caja (ej: 10.50 por redondeo)
        Double montoRealCaja = montoSistemaDouble;
        Double ajusteRedondeo = 0.0;

        // Solo aplicamos redondeo si es Efectivo
        if ("EFECTIVO".equalsIgnoreCase(request.getMetodoPago())) {
            // Regla Perú: Redondeo al 0.10 más cercano (o 0.05 según configuración, usaremos tu ejemplo 0.1)
            BigDecimal bd = new BigDecimal(montoSistemaDouble).setScale(1, RoundingMode.HALF_UP);
            montoRealCaja = bd.doubleValue();
            ajusteRedondeo = montoRealCaja - montoSistemaDouble;
        }

        // --- 3. LOGICA WATERFALL (Tu algoritmo de negocio) ---
        // Usamos BigDecimal para la lógica precisa de amortización
        BigDecimal dineroDisponibleParaDeuda = BigDecimal.valueOf(montoSistemaDouble);

        List<Cuota> cuotas = cuotaRepository.findByPrestamo_IdPrestamo(prestamo.getIdPrestamo()); // Ajustado a JPA estándar
        cuotas.sort(Comparator.comparingInt(Cuota::getNumeroCuota));

        List<String> coberturaLog = new ArrayList<>();

        for (Cuota cuota : cuotas) {
            if (dineroDisponibleParaDeuda.compareTo(BigDecimal.ZERO) <= 0) break;

            // Coversión segura para cálculos
            BigDecimal montoCuota = BigDecimal.valueOf(cuota.getMontoProgramado());
            BigDecimal montoPagadoActual = cuota.getMontoPagado() != null
                    ? BigDecimal.valueOf(cuota.getMontoPagado())
                    : BigDecimal.ZERO;

            BigDecimal saldoCapital = montoCuota.subtract(montoPagadoActual);

            // Limpieza de residuos decimales (tu lógica)
            if (saldoCapital.abs().compareTo(new BigDecimal("0.001")) < 0) {
                saldoCapital = BigDecimal.ZERO;
            }

            // Calcular Mora
            BigDecimal moraCalculada = BigDecimal.ZERO;
            // Nota: Agregué chequeo de estado != Pagado para no cobrar mora a cuotas ya pagadas
            if (!"PAGADO".equalsIgnoreCase(cuota.getEstado()) &&
                    cuota.getFechaVencimiento().isBefore(LocalDate.now()) &&
                    saldoCapital.compareTo(BigDecimal.ZERO) > 0) {
                moraCalculada = saldoCapital.multiply(MORA_PORCENTAJE).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal deudaTotalCuota = saldoCapital.add(moraCalculada);

            if (deudaTotalCuota.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Aplicación del Pago
            if (dineroDisponibleParaDeuda.compareTo(deudaTotalCuota) >= 0) {
                // PAGO COMPLETO
                cuota.setMontoPagado(montoCuota.doubleValue()); // Se guarda el monto original programado
                // Aquí podrías guardar la mora pagada en otro campo si tuvieras 'interes_mora_pagado'
                cuota.setInteresMora(moraCalculada.doubleValue());
                cuota.setEstado("PAGADO");

                dineroDisponibleParaDeuda = dineroDisponibleParaDeuda.subtract(deudaTotalCuota);
                coberturaLog.add("Cuota " + cuota.getNumeroCuota() + " (Completa) - S/ " + deudaTotalCuota);
            } else {
                // PAGO PARCIAL
                BigDecimal pagoParaMora = dineroDisponibleParaDeuda.min(moraCalculada);
                BigDecimal remanenteParaCapital = dineroDisponibleParaDeuda.subtract(pagoParaMora);

                BigDecimal nuevoPagado = montoPagadoActual.add(remanenteParaCapital);

                cuota.setMontoPagado(nuevoPagado.doubleValue());

                // Actualizamos la mora generada en la entidad para que se refleje
                cuota.setInteresMora(moraCalculada.doubleValue());

                if (cuota.getFechaVencimiento().isBefore(LocalDate.now())) {
                    cuota.setEstado("MORA ACTIVA"); // Estandarizando estado
                } else {
                    cuota.setEstado("ADELANTO");
                }

                coberturaLog.add("Cuota " + cuota.getNumeroCuota() + " (Parcial) - S/ " + dineroDisponibleParaDeuda);
                dineroDisponibleParaDeuda = BigDecimal.ZERO;
            }
            cuotaRepository.save(cuota);
        }

        // --- 4. PERSISTENCIA DEL PAGO (Lo que faltaba) ---
        Pago nuevoPago = new Pago();
        nuevoPago.setPrestamo(prestamo);
        nuevoPago.setUsuario(usuario);
        nuevoPago.setCaja(cajaActual);
        nuevoPago.setFechaPago(LocalDateTime.now());
        nuevoPago.setMontoTotal(montoSistemaDouble); // Guardamos que se pagó 10.54 de deuda
        nuevoPago.setAjusteRedondeo(ajusteRedondeo); // Guardamos -0.04 de ajuste
        nuevoPago.setMetodoPago(request.getMetodoPago());
        nuevoPago.setTipoComprobante("TICKET");
        nuevoPago.setNroOperacion(UUID.randomUUID().toString());

        pagoRepository.save(nuevoPago);

        // --- 5. ACTUALIZAR CAJA ---
        // Registramos en caja: Si es efectivo entra el montoReal (10.50), si es digital entra todo (10.54)
        boolean esEfectivo = "EFECTIVO".equalsIgnoreCase(request.getMetodoPago());
        cajaService.registrarMovimiento(
                cajaActual,
                esEfectivo ? montoRealCaja : montoSistemaDouble,
                ajusteRedondeo,
                esEfectivo
        );

        // --- 6. PREPARAR RESPUESTA ---
        PagoResponseDto response = new PagoResponseDto();
        response.setIdPago(nuevoPago.getIdPago()); // Devolvemos el ID del pago real generado
        response.setStatus("approved");
        response.setMensaje("Pago procesado y registrado en caja.");
        response.setMontoAplicado(BigDecimal.valueOf(montoSistemaDouble).subtract(dineroDisponibleParaDeuda));
        response.setDetallesCobertura(coberturaLog);

        // Recalcular estado de cuenta (puedes extraer este método del servicio antiguo o implementarlo aquí)
        // Por ahora devolvemos null o calculamos básico, idealmente reusa tu lógica de EstadoCuenta
        // response.setDeudaRestante(...);

        return response;
    }
}