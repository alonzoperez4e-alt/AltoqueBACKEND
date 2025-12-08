package com.altoque.altoque.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok genera getters, setters, toString, equals, hashCode automáticamente
@NoArgsConstructor // Lombok genera un constructor vacío
public class ClienteConsultaDto {

    // Identificación
    private String tipo; // "NATURAL" o "JURIDICA"

    // Datos Persona Natural (DNI)
    private String dniCliente;
    private String nombreCliente;
    private String apellidoCliente;

    // Datos Persona Jurídica (RUC)
    private String ruc;
    private String razonSocial;

    // Estado del Cliente (Crucial para el Dashboard)
    private boolean tienePrestamoActivo;

    // Constructor para Persona Natural (Compatibilidad con código existente)
    public ClienteConsultaDto(String dniCliente, String nombreCliente, String apellidoCliente, boolean tienePrestamoActivo) {
        this.tipo = "NATURAL";
        this.dniCliente = dniCliente;
        this.nombreCliente = nombreCliente;
        this.apellidoCliente = apellidoCliente;
        this.tienePrestamoActivo = tienePrestamoActivo;
    }
}