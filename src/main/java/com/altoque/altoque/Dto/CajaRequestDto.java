package com.altoque.altoque.Dto;

import lombok.Data;

@Data
public class CajaRequestDto {
    private Double saldo; // Usado para saldo inicial (apertura) o saldo real (cierre)
}