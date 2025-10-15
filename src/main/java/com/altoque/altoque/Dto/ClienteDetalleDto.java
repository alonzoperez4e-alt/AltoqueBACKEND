package com.altoque.altoque.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClienteDetalleDto {
    // --- CAMBIO ---
    // Se añade el ID del cliente. Será nulo para clientes nuevos
    // y tendrá un valor para los ya existentes o recién guardados.
    private Integer idCliente;

    private String dniCliente;
    private String nombreCliente;
    private String apellidoCliente;
    private String fechaNacimiento; // Se mantiene como String para la comunicación
    private Boolean esPep;
    private String correoCliente;
    private String telefonoCliente;
    private String direccionCliente;
    private boolean esNuevo;
}

