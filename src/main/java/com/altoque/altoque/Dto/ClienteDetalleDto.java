package com.altoque.altoque.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClienteDetalleDto {
    private Integer idCliente;

    // Identificación
    private String tipo; // "NATURAL" o "JURIDICA" (Mapeado desde tipoCliente)
    private boolean esNuevo;

    // Natural
    private String dniCliente;
    private String nombreCliente;
    private String apellidoCliente;
    private String fechaNacimiento;
    private Boolean esPep;

    // Jurídica
    private String ruc;
    private String razonSocial;
    private String direccionFiscal;
    private String fechaConstitucion;
    private String representanteLegalDni;
    private String representanteLegalNombre;

    // Comunes
    private String correoCliente;
    private String telefonoCliente;
    private String direccionCliente;
}