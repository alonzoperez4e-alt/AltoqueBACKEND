package com.altoque.altoque.Dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PagoDto {
    private String titulo;      // Ej: "Cuota #1 - Préstamo 102"
    private BigDecimal precio;  // Ej: 150.00
    private Integer cantidad;   // Generalmente 1
    private String idCuota;     // NUEVO: ID único de la cuota para referencia externa
}