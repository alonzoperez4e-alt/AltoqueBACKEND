package com.altoque.altoque.Dto;

import lombok.Data;

// Esta clase sirve como un contenedor específico para los datos
// que el frontend envía para generar la declaración UIT.
@Data
public class DeclaracionUITRequestDto {
    private ClienteDetalleDto client;
    private double amount;
}
