package com.altoque.altoque.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumeroALetrasUtil {

    private static final String[] UNIDADES = {"", "UN ", "DOS ", "TRES ", "CUATRO ", "CINCO ", "SEIS ", "SIETE ", "OCHO ", "NUEVE "};
    private static final String[] DECENAS = {"DIEZ ", "ONCE ", "DOCE ", "TRECE ", "CATORCE ", "QUINCE ", "DIECISEIS ", "DIECISIETE ", "DIECIOCHO ", "DIECINUEVE ", "VEINTE ", "TREINTA ", "CUARENTA ", "CINCUENTA ", "SESENTA ", "SETENTA ", "OCHENTA ", "NOVENTA "};
    private static final String[] CENTENAS = {"", "CIENTO ", "DOSCIENTOS ", "TRESCIENTOS ", "CUATROCIENTOS ", "QUINIENTOS ", "SEISCIENTOS ", "SETECIENTOS ", "OCHOCIENTOS ", "NOVECIENTOS "};

    public static String aLetras(BigDecimal cantidad, String moneda) {
        if (cantidad == null) return "CERO";

        // Separar enteros y decimales
        BigDecimal parteEntera = cantidad.setScale(0, RoundingMode.DOWN);
        int parteDecimal = cantidad.subtract(parteEntera).multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).intValue();

        String letras = convertir(parteEntera.longValue());

        // Ajuste gramatical para "UN" vs "UNO" al final si fuera necesario,
        // pero para moneda se suele usar "UN SOLES" -> "UN SOL" (lógica simple aquí)
        if (letras.trim().equals("UN")) {
            return "UN " + moneda.toUpperCase() + " CON " + String.format("%02d", parteDecimal) + "/100";
        }

        return letras + moneda.toUpperCase() + " CON " + String.format("%02d", parteDecimal) + "/100";
    }

    private static String convertir(long numero) {
        if (numero == 0) return "CERO ";
        if (numero < 10) return UNIDADES[(int) numero];
        if (numero < 20) return DECENAS[(int) numero - 10];
        if (numero < 100) {
            return DECENAS[(int) (numero / 10) + 8] + ((numero % 10 != 0) ? "Y " + UNIDADES[(int) (numero % 10)] : "");
        }
        if (numero < 1000) {
            return (numero == 100) ? "CIEN " : CENTENAS[(int) (numero / 100)] + convertir(numero % 100);
        }
        if (numero < 1000000) {
            String miles = (numero / 1000 == 1) ? "MIL " : convertir(numero / 1000) + "MIL ";
            return miles + ((numero % 1000 != 0) ? convertir(numero % 1000) : "");
        }
        if (numero < 1000000000) {
            String millones = (numero / 1000000 == 1) ? "UN MILLON " : convertir(numero / 1000000) + "MILLONES ";
            return millones + ((numero % 1000000 != 0) ? convertir(numero % 1000000) : "");
        }
        return "NÚMERO DEMASIADO GRANDE";
    }
}