package com.altoque.altoque.Dto.Payment;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PagoRequestDto {
    private Integer prestamoId; // Integer
    private BigDecimal monto;
    private String metodoPago;
    private String descripcion;
}