package com.altoque.altoque.Service;

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
import java.time.LocalTime;
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

    private static final BigDecimal MORA_PORCENTAJE = new BigDecimal("0.01");

    /**
     * Procesa un pago manual desde ventanilla (Efectivo/Otros).
     * Busca la caja activa automáticamente.
     */
    @Transactional
    public PagoResponseDto procesarPago(PagoRequestDto request, Integer userId) {
        // 1. Validar Caja Abierta del usuario actual
        Caja cajaActual = cajaRepository.findByUsuario_IdUsuarioAndEstado(userId, "ABIERTA")
                .orElseThrow(() -> new ApiServerException("No se puede procesar pagos sin una caja abierta."));

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ApiServerException("Usuario no encontrado"));

        Prestamo prestamo = prestamoRepository.findById(request.getPrestamoId())
                .orElseThrow(() -> new ApiServerException("Préstamo no encontrado"));

        // Lógica de Redondeo (Solo Efectivo)
        Double montoSistema = request.getMonto().doubleValue();
        Double montoRealCaja = montoSistema;
        Double ajusteRedondeo = 0.0;

        if ("EFECTIVO".equalsIgnoreCase(request.getMetodoPago())) {
            BigDecimal bd = new BigDecimal(montoSistema).setScale(1, RoundingMode.HALF_UP);
            montoRealCaja = bd.doubleValue();
            ajusteRedondeo = montoRealCaja - montoSistema;
        }

        String nroOperacion = UUID.randomUUID().toString(); // Generamos uno interno

        // Delegar al núcleo
        return ejecutarLogicaPago(
                prestamo,
                usuario,
                cajaActual,
                montoSistema,
                montoRealCaja,
                ajusteRedondeo,
                request.getMetodoPago(),
                nroOperacion,
                null // Sin orden externa
        );
    }

    /**
     * Procesa un pago confirmado por FLOW.
     * Utiliza la caja y usuario específicos que iniciaron la transacción (Metadata).
     */
    @Transactional
    public PagoResponseDto procesarPagoFlow(Integer prestamoId, Double monto, Integer cajaId, Integer userId, String flowOrder, String commerceOrder) {

        Prestamo prestamo = prestamoRepository.findById(Math.toIntExact(prestamoId))
                .orElseThrow(() -> new ApiServerException("Préstamo no encontrado (Flow)"));

        Usuario usuario = usuarioRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new ApiServerException("Usuario no encontrado (Flow)"));

        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new ApiServerException("La caja asociada al pago Flow ya no existe o es inválida."));

        // Validar si la caja sigue abierta?
        // Depende de tu regla de negocio. Si el usuario cerró caja mientras el cliente pagaba,
        // técnicamente entra en la siguiente o se reabre.
        // Por ahora asumimos que entra en la caja que se indicó, aunque esté cerrada (auditoría posterior)
        // O lanzamos error si es estricto.

        // Para Flow no hay redondeo de efectivo
        Double montoSistema = monto;
        Double montoReal = monto;
        Double ajuste = 0.0;

        return ejecutarLogicaPago(
                prestamo,
                usuario,
                caja,
                montoSistema,
                montoReal,
                ajuste,
                "FLOW",
                flowOrder,
                commerceOrder
        );
    }

    /**
     * NÚCLEO CENTRAL DE PAGO (Waterfall de Cuotas)
     * Reutilizado para garantizar consistencia contable.
     */
    private PagoResponseDto ejecutarLogicaPago(
            Prestamo prestamo,
            Usuario usuario,
            Caja caja,
            Double montoSistema,
            Double montoRealCaja,
            Double ajusteRedondeo,
            String metodoPago,
            String nroOperacion,
            String ordenExterna
    ) {
        if (montoSistema <= 0) throw new ApiServerException("El monto debe ser mayor a 0");

        BigDecimal dineroDisponible = BigDecimal.valueOf(montoSistema);

        // Obtener cuotas y ordenar
        List<Cuota> cuotas = cuotaRepository.findByPrestamo_IdPrestamo(prestamo.getIdPrestamo());
        cuotas.sort(Comparator.comparingInt(Cuota::getNumeroCuota));

        List<String> coberturaLog = new ArrayList<>();

        // --- LÓGICA DE AMORTIZACIÓN ---
        for (Cuota cuota : cuotas) {
            if (dineroDisponible.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal montoCuota = BigDecimal.valueOf(cuota.getMontoProgramado());
            BigDecimal montoPagadoActual = cuota.getMontoPagado() != null ? BigDecimal.valueOf(cuota.getMontoPagado()) : BigDecimal.ZERO;
            BigDecimal saldoCapital = montoCuota.subtract(montoPagadoActual);

            if (saldoCapital.abs().compareTo(new BigDecimal("0.001")) < 0) saldoCapital = BigDecimal.ZERO;

            // Mora
            BigDecimal moraCalculada = BigDecimal.ZERO;
            if (!"PAGADO".equalsIgnoreCase(cuota.getEstado()) &&
                    cuota.getFechaVencimiento().isBefore(LocalDate.now()) &&
                    saldoCapital.compareTo(BigDecimal.ZERO) > 0) {
                moraCalculada = saldoCapital.multiply(MORA_PORCENTAJE).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal deudaTotalCuota = saldoCapital.add(moraCalculada);
            if (deudaTotalCuota.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Aplicar
            if (dineroDisponible.compareTo(deudaTotalCuota) >= 0) {
                // Pago Completo Cuota
                cuota.setMontoPagado(montoCuota.doubleValue());
                cuota.setInteresMora(moraCalculada.doubleValue());
                cuota.setEstado("PAGADO");

                dineroDisponible = dineroDisponible.subtract(deudaTotalCuota);
                coberturaLog.add("Cuota " + cuota.getNumeroCuota() + " (Completa)");
            } else {
                // Pago Parcial
                BigDecimal pagoParaMora = dineroDisponible.min(moraCalculada);
                BigDecimal remanenteParaCapital = dineroDisponible.subtract(pagoParaMora);

                cuota.setMontoPagado(montoPagadoActual.add(remanenteParaCapital).doubleValue());
                cuota.setInteresMora(moraCalculada.doubleValue()); // Nota: Esto asume que pagas la mora calculada hasta hoy

                if (cuota.getFechaVencimiento().isBefore(LocalDate.now())) {
                    cuota.setEstado("MORA ACTIVA");
                } else {
                    cuota.setEstado("ADELANTO");
                }

                coberturaLog.add("Cuota " + cuota.getNumeroCuota() + " (Parcial)");
                dineroDisponible = BigDecimal.ZERO;
            }
            cuotaRepository.save(cuota);
        }

        // --- PERSISTENCIA DEL PAGO ---
        Pago nuevoPago = new Pago();
        nuevoPago.setPrestamo(prestamo);
        nuevoPago.setUsuario(usuario);
        nuevoPago.setCaja(caja); // Vinculación crítica
        nuevoPago.setFechaPago(LocalDateTime.now()); // Usar LocalDate para fecha
        nuevoPago.setMonto(montoSistema);        // Monto Total deuda reducida
        nuevoPago.setAjusteRedondeo(ajusteRedondeo);
        nuevoPago.setMetodoPago(metodoPago);
        nuevoPago.setTipoComprobante("TICKET"); // O FACTURA/BOLETA según cliente
        nuevoPago.setNroOperacion(nroOperacion);
        nuevoPago.setOrdenExterna(ordenExterna); // Guardamos LOAN-X-X

        Pago pagoGuardado = pagoRepository.save(nuevoPago);

        // --- ACTUALIZAR CAJA ---
        boolean esEfectivo = "EFECTIVO".equalsIgnoreCase(metodoPago);
        // Si es efectivo, entra montoReal (con redondeo). Si es digital, entra montoSistema exacto.
        cajaService.registrarMovimiento(
                caja,
                esEfectivo ? montoRealCaja : montoSistema,
                ajusteRedondeo,
                esEfectivo // true=suma a efectivo, false=suma a digital
        );

        // --- RESPUESTA ---
        PagoResponseDto response = new PagoResponseDto();
        response.setIdPago(pagoGuardado.getIdPago().intValue());
        response.setStatus("approved");
        response.setMensaje("Pago registrado correctamente en cuotas y caja.");
        response.setMontoAplicado(BigDecimal.valueOf(montoSistema).subtract(dineroDisponible));
        response.setDetallesCobertura(coberturaLog);

        return response;
    }
}