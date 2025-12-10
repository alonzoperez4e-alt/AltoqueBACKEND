package com.altoque.altoque.Dto.Comprobante;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ComprobanteItemDto {

    private BigDecimal cantidad;      // Cantidad
    private String unidadMedida;      // "UNIDAD" / "NIU"
    private String descripcion;       // Descripción del servicio
    private BigDecimal valorUnitario; // Valor unitario sin IGV
    private BigDecimal importeTotal;  // Importe total (incluye IGV)
}
