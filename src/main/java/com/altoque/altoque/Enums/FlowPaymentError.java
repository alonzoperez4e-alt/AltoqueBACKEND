package com.altoque.altoque.Enums;

import java.util.Arrays;

/**
 * Catálogo de errores de pago según documentación de Flow API 3.0.
 * Utilizado para diagnosticar fallos en payment/getStatusExtended.
 */
public enum FlowPaymentError {

    TARJETA_INVALIDA(-1, "Tarjeta inválida"),
    LIMITE_REINTENTOS(-11, "Excede límite de reintentos de rechazos"),
    ERROR_CONEXION(-2, "Error de conexión"),
    MONTO_MAXIMO(-3, "Excede monto máximo"),
    FECHA_EXPIRACION(-4, "Fecha de expiración inválida"),
    ERROR_AUTENTICACION(-5, "Problema en autenticación"),
    RECHAZO_GENERAL(-6, "Rechazo general"),
    TARJETA_BLOQUEADA(-7, "Tarjeta bloqueada"),
    TARJETA_VENCIDA(-8, "Tarjeta vencida"),
    TRANSACCION_NO_SOPORTADA(-9, "Transacción no soportada"),
    PROBLEMA_TRANSACCION(-10, "Problema en la transacción"),
    ERROR_DESCONOCIDO(999, "Error desconocido");

    private final int code;
    private final String description;

    FlowPaymentError(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static FlowPaymentError fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElse(ERROR_DESCONOCIDO);
    }
}