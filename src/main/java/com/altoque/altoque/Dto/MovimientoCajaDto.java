package com.altoque.altoque.Dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MovimientoCajaDto {
    private Integer id;
    private LocalDateTime timestamp;
    private String clientName;
    private String clientDNI;
    private String type; // 'PAGO'
    private String method; // 'EFECTIVO', 'YAPE', etc.
    private Double systemAmount;
    private Double roundingAdjustment;
    private Double realAmount;
}