package com.altoque.altoque.Dto;

// Usamos Lombok para reducir el código boilerplate (getters, setters, etc.)
// Asegúrate de tener la dependencia de Lombok en tu pom.xml
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Genera automáticamente getters, setters, toString, etc.
@NoArgsConstructor // Genera un constructor sin argumentos
public class ClienteConsultaDto {

    // Datos obtenidos de la API externa (apiPeru.dev)
    private String dniCliente;
    private String nombreCliente;
    private String apellidoCliente;

    // Dato crucial obtenido de nuestra base de datos interna
    private boolean tienePrestamoActivo;

    // Constructor para facilitar la creación del DTO
    public ClienteConsultaDto(String dniCliente, String nombreCliente, String apellidoCliente, boolean tienePrestamoActivo) {
        this.dniCliente = dniCliente;
        this.nombreCliente = nombreCliente;
        this.apellidoCliente = apellidoCliente;
        this.tienePrestamoActivo = tienePrestamoActivo;
    }
}
