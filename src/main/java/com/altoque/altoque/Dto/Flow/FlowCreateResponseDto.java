package com.altoque.altoque.Dto.Flow;

/**
 * Respuesta mapeada desde el JSON de Flow tras llamar a payment/create.
 */
public class FlowCreateResponseDto {

    private String url;       // URL base de redirección (ej. https://sandbox.flow.cl/...)
    private String token;     // Token de la transacción
    private String flowOrder; // Número de orden interno de Flow

    // Constructor vacío
    public FlowCreateResponseDto() {}

    // Getters y Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getFlowOrder() { return flowOrder; }
    public void setFlowOrder(String flowOrder) { this.flowOrder = flowOrder; }

    /**
     * Helper para obtener la URL completa de redirección para el Frontend.
     * Formato: url + "?token=" + token
     */
    public String getRedirectUrl() {
        return this.url + "?token=" + this.token;
    }
}