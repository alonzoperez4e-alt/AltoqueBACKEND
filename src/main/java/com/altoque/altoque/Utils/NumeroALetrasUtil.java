package com.altoque.altoque.Utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class NumeroALetrasUtil {

    public static String aLetras(BigDecimal monto, String monedaTexto) {
        DecimalFormat df = new DecimalFormat("0.00");
        String s = df.format(monto);
        String[] partes = s.split("\\.");
        String enteros = partes[0];
        String decimales = partes[1];

        //TODO: reemplazar por una conversión real a letras
        return enteros + " Y " + decimales + "/100 " + monedaTexto;
    }
}
