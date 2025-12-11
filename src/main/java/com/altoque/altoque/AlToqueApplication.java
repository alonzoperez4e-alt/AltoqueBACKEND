package com.altoque.altoque;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AlToqueApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlToqueApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // Configurar la zona horaria por defecto a Perú (UTC-5)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
        System.out.println("Hora actual configurada: " + new java.util.Date());
    }

}
