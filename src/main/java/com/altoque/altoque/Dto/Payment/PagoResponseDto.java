package com.altoque.altoque.Dto.Payment;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PagoResponseDto {
    private Integer idPago;
    private String mensaje;
    private BigDecimal montoAplicado;
    private BigDecimal deudaRestante;
    private List<String> detallesCobertura; // "Cuota 1 (Completa)", "Cuota 2 (Parcial)"
    private String status;
}