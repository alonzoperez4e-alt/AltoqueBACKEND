package com.altoque.altoque.Dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrestamoDto {

    @NotNull(message = "El ID del cliente es obligatorio")
    private Integer idCliente;

    @NotNull(message = "El monto del préstamo es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor que 0")
    @DecimalMax(value = "150000.00", message = "El monto no puede superar los 150,000")
    private Double monto;

    @NotNull(message = "La tasa de interés anual es obligatoria")
    @DecimalMin(value = "0.01", message = "La tasa de interés debe ser mayor que 0")
    @DecimalMax(value = "100.00", message = "La tasa de interés no puede ser mayor al 100%")
    private Double tasaInteresAnual;

    @NotNull(message = "El número de cuotas es obligatorio")
    @Min(value = 1, message = "Debe haber al menos una cuota")
    @Max(value = 60, message = "El máximo permitido es de 60 cuotas (5 años)")
    private Integer numeroCuotas;

    @NotNull(message = "La fecha del préstamo es obligatoria")
    @FutureOrPresent(message = "La fecha del préstamo no puede ser anterior al día actual")
    private java.time.LocalDate fechaPrestamo;
}