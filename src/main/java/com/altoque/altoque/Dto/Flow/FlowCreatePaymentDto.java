package com.altoque.altoque.Dto.Flow;

import java.util.Map;

public class FlowCreatePaymentDto {

    private String commerceOrder;
    private String subject;
    private Double amount;
    private String email;
    private String urlReturn;

    // Campo nuevo para transportar datos internos (CajaId, UserId)
    private Map<String, Object> metadata;

    // Getters y Setters
    public String getCommerceOrder() { return commerceOrder; }
    public void setCommerceOrder(String commerceOrder) { this.commerceOrder = commerceOrder; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUrlReturn() { return urlReturn; }
    public void setUrlReturn(String urlReturn) { this.urlReturn = urlReturn; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}