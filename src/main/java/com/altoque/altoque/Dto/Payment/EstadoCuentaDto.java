package com.altoque.altoque.Dto.Payment;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class EstadoCuentaDto {
    private Integer prestamoId; // Integer para coincidir con tu entidad
    private String tipoCliente;
    private String clienteNombre;
    private String razonSocial;
    private String documento;
    private BigDecimal deudaOriginalTotal;
    private BigDecimal deudaPendienteTotal;
    private List<CuotaEstadoDto> cuotas;

    @Data
    public static class CuotaEstadoDto {
        private Integer id; // Integer para coincidir con tu entidad
        private Integer numeroCuota;
        private LocalDate fechaVencimiento;
        private BigDecimal cuotaOriginal;
        private BigDecimal saldoPendiente;
        private BigDecimal moraGenerada;
        private String estado;
        private BigDecimal totalAPagar;
    }
}