package com.altoque.altoque.Utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utilidad de seguridad para la integración con FLOW API.
 * Encargada de generar firmas HMAC-SHA256 para autenticar las peticiones.
 */
public class FlowSignatureUtil {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Genera la firma digital requerida por Flow.
     * * Reglas:
     * 1. Ordena los parámetros alfabéticamente.
     * 2. Concatena clave y valor (key + value).
     * 3. Firma usando HMAC-SHA256 con la secretKey.
     * * @param params Mapa con los parámetros a enviar (apiKey, amount, currency, etc).
     * @param secretKey La llave secreta proporcionada por Flow (Sandbox o Producción).
     * @return El hash HMAC-SHA256 en formato Hexadecimal.
     * @throws Exception Si ocurre un error en el algoritmo de encriptación.
     */
    public static String calculateSignature(Map<String, Object> params, String secretKey) throws Exception {
        // 1. Ordenar los parámetros de forma alfabética ascendente
        // TreeMap ordena automáticamente por las claves (Keys)
        Map<String, Object> sortedParams = new TreeMap<>(params);

        StringBuilder toSign = new StringBuilder();

        // 2. Concatenar: Nombre_del_parametro valor
        // Nota: Se excluye el parámetro 's' si estuviera presente, ya que es la firma misma.
        for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Ignorar el campo de firma si llegase a estar en el mapa
            if (!"s".equals(key)) {
                toSign.append(key).append(value.toString());
            }
        }

        // 3. Firmar el string concatenado
        return hmacSHA256(secretKey, toSign.toString());
    }

    /**
     * Función auxiliar para calcular HMAC-SHA256
     */
    private static String hmacSHA256(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance(ALGORITHM);
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        sha256_HMAC.init(secret_key);

        byte[] rawHmac = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

        return bytesToHex(rawHmac);
    }

    /**
     * Convierte el array de bytes a representación Hexadecimal (String)
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}