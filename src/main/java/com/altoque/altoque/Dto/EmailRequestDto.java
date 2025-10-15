package com.altoque.altoque.Dto;

import lombok.Data;

// Usando Lombok para generar getters y setters automáticamente
@Data
public class EmailRequestDto{
    private Integer clienteId;
    private Integer prestamoId;
    private String emailDestino; // Opcional, si quieres enviarlo a un correo diferente al del cliente

    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        this.clienteId = clienteId;
    }

    public Integer getPrestamoId() {
        return prestamoId;
    }

    public void setPrestamoId(Integer prestamoId) {
        this.prestamoId = prestamoId;
    }

    public String getEmailDestino() {
        return emailDestino;
    }

    public void setEmailDestino(String emailDestino) {
        this.emailDestino = emailDestino;
    }
}
