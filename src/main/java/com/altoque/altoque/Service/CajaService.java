package com.altoque.altoque.Service;

import com.altoque.altoque.Dto.MovimientoCajaDto;
import com.altoque.altoque.Entity.Caja;
import com.altoque.altoque.Entity.Pago;
import com.altoque.altoque.Entity.Usuario;
import com.altoque.altoque.Repository.CajaRepository;
import com.altoque.altoque.Repository.PagoRepository;
import com.altoque.altoque.Repository.UsuarioRepository;
import com.altoque.altoque.Exception.ApiServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CajaService {

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PagoRepository pagoRepository;

    // Abrir Caja
    @Transactional
    public Caja abrirCaja(Integer idUsuario, Double saldoInicial) {
        Optional<Caja> cajaAbierta = cajaRepository.findByUsuario_IdUsuarioAndEstado(idUsuario, "ABIERTA");
        if (cajaAbierta.isPresent()) {
            throw new ApiServerException("Ya tienes una caja abierta.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiServerException("Usuario no encontrado"));

        Caja nuevaCaja = new Caja();
        nuevaCaja.setUsuario(usuario);
        nuevaCaja.setSaldoInicial(saldoInicial);
        nuevaCaja.setSaldoFinalEsperado(saldoInicial); // Inicialmente es igual al saldo inicial
        nuevaCaja.setEstado("ABIERTA");

        return cajaRepository.save(nuevaCaja);
    }

    // Obtener Resumen de Caja Actual (Para el Dashboard)
    public Caja obtenerCajaActual(Integer idUsuario) {
        return cajaRepository.findByUsuario_IdUsuarioAndEstado(idUsuario, "ABIERTA")
                .orElseThrow(() -> new ApiServerException("No hay caja abierta para este usuario."));
    }

    // Cerrar Caja (Arqueo)
    @Transactional
    public Caja cerrarCaja(Integer idUsuario, Double saldoFinalReal) {
        Caja caja = obtenerCajaActual(idUsuario);

        caja.setSaldoFinalReal(saldoFinalReal);
        caja.setFechaCierre(LocalDateTime.now());
        caja.setEstado("CERRADA");

        // Aquí podrías guardar la diferencia si quisieras una columna extra (real - esperado)

        return cajaRepository.save(caja);
    }

    // Método interno para actualizar montos cuando se hace un pago
    @Transactional
    public void registrarMovimiento(Caja caja, Double monto, Double ajuste, boolean esEfectivo) {
        if(esEfectivo) {
            caja.setTotalEfectivoSistema(caja.getTotalEfectivoSistema() + monto);
            caja.setTotalAjusteRedondeo(caja.getTotalAjusteRedondeo() + ajuste);
        } else {
            caja.setTotalDigitalSistema(caja.getTotalDigitalSistema() + monto);
        }

        // Recalcular saldo esperado
        caja.setSaldoFinalEsperado(
                caja.getSaldoInicial() +
                        caja.getTotalEfectivoSistema() +
                        caja.getTotalDigitalSistema()
        );

        cajaRepository.save(caja);
    }

    // NUEVO MÉTODO: Obtener movimientos de la caja abierta
    public List<MovimientoCajaDto> obtenerMovimientosCajaActual(Integer idUsuario) {
        // 1. Obtener la caja activa
        Caja caja = cajaRepository.findByUsuario_IdUsuarioAndEstado(idUsuario, "ABIERTA")
                .orElseThrow(() -> new ApiServerException("No hay caja abierta para consultar movimientos."));

        // 2. Obtener los pagos asociados a esa caja
        List<Pago> pagos = pagoRepository.findByCaja_IdCajaOrderByFechaPagoDesc(caja.getIdCaja());

        // 3. Convertir a DTO
        return pagos.stream().map(pago -> {
            MovimientoCajaDto dto = new MovimientoCajaDto();
            dto.setId(pago.getIdPago());
            dto.setTimestamp(pago.getFechaPago());

            // Datos del cliente desde el préstamo
            if(pago.getPrestamo() != null && pago.getPrestamo().getCliente() != null) {
                // Manejo de nombre según tipo de cliente (Natural vs Jurídico)
                String nombre = pago.getPrestamo().getCliente().getRazonSocial() != null
                        ? pago.getPrestamo().getCliente().getRazonSocial()
                        : pago.getPrestamo().getCliente().getNombreCliente() + " " + pago.getPrestamo().getCliente().getApellidoCliente();

                String doc = pago.getPrestamo().getCliente().getRuc() != null
                        ? pago.getPrestamo().getCliente().getRuc()
                        : pago.getPrestamo().getCliente().getDniCliente();

                dto.setClientName(nombre);
                dto.setClientDNI(doc);
            } else {
                dto.setClientName("Cliente Desconocido");
                dto.setClientDNI("-");
            }

            dto.setType("PAGO"); // Por ahora solo registramos pagos
            dto.setMethod(pago.getMetodoPago());
            dto.setSystemAmount(pago.getMontoTotal());
            dto.setRoundingAdjustment(pago.getAjusteRedondeo());

            // Calculamos el real: Sistema + Ajuste (Ej: 10.54 + (-0.04) = 10.50)
            // Solo sumamos ajuste si es efectivo, aunque logicamente en otros medios el ajuste es 0
            dto.setRealAmount(pago.getMontoTotal() + (pago.getAjusteRedondeo() != null ? pago.getAjusteRedondeo() : 0.0));

            return dto;
        }).collect(Collectors.toList());
    }
}