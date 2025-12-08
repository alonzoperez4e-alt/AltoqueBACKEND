package com.altoque.altoque.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class CuotaDto {
    private Integer idCuota;
    private Integer numeroCuota;
    private LocalDate fechaVencimiento;
    private Double montoProgramado;
    private Double montoPagado;
    private Double interesMora;
    private String estado;

    // Total exigible actual (Monto + Mora)
    private Double totalExigible;

    public CuotaDto(Integer idCuota, Integer numeroCuota, LocalDate fechaVencimiento, Double montoProgramado, String estado) {
        this.idCuota = idCuota;
        this.numeroCuota = numeroCuota;
        this.fechaVencimiento = fechaVencimiento;
        this.montoProgramado = montoProgramado;
        this.estado = estado;
        this.montoPagado = 0.0;
        this.interesMora = 0.0;
        this.totalExigible = montoProgramado;
    }
}