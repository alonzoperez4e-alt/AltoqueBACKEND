package com.altoque.altoque.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Ignora cualquier campo extra que la API pueda devolver y que no mapeemos aquí
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiPeruResponseDto {

    private boolean success;
    private Data data;

    // Clase interna para el objeto "data" anidado en la respuesta
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String numero;
        private String nombre_completo;
        private String nombres;
        private String apellido_paterno;
        private String apellido_materno;
        // Agrega otros campos si los necesitas

        // Getters y Setters para Data
        public String getNumero() { return numero; }
        public void setNumero(String numero) { this.numero = numero; }
        public String getNombre_completo() { return nombre_completo; }
        public void setNombre_completo(String nombre_completo) { this.nombre_completo = nombre_completo; }
        public String getNombres() { return nombres; }
        public void setNombres(String nombres) { this.nombres = nombres; }
        public String getApellido_paterno() { return apellido_paterno; }
        public void setApellido_paterno(String apellido_paterno) { this.apellido_paterno = apellido_paterno; }
        public String getApellido_materno() { return apellido_materno; }
        public void setApellido_materno(String apellido_materno) { this.apellido_materno = apellido_materno; }
    }

    // Getters y Setters para ApiPeruResponseDTO
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }
}