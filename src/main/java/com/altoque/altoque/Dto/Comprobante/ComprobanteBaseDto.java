package com.altoque.altoque.Dto.Comprobante;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ComprobanteBaseDto {

    // Datos del comprobante
    private String tipoComprobante;   // "FACTURA ELECTRONICA" / "BOLETA DE VENTA ELECTRONICA"
    private String serie;             // "F001", "B001"
    private String numero;            // "00000001"

    // Emisor
    private String rucEmisor;
    private String razonSocialEmisor;
    private String direccionEmisor;

    // Cliente / Receptor
    private String tipoDocCliente;    // "1" DNI, "6" RUC
    private String numeroDocCliente;
    private String nombreCliente;
    private String direccionCliente;

    // Datos generales
    private LocalDate fechaEmision;
    private String moneda;            // "PEN"
    private String tipoMonedaTexto;   // "SOLES"
    private String formaPago;         // "CONTADO" / "CREDITO"

    // Detalle
    private List<ComprobanteItemDto> items;

    // Totales
    private BigDecimal opGravadas;
    private BigDecimal opExoneradas;
    private BigDecimal opInafectas;
    private BigDecimal descuentoGlobal;
    private BigDecimal otrosCargos;
    private BigDecimal igvTotal;
    private BigDecimal importeTotal;

    // Otros
    private String observacion;
    private String leyendaImporteEnLetras; // "CIENTO DIECIOCHO Y 00/100 SOLES"
    private String notaRepresentacion;     // texto SUNAT pie de página
}
