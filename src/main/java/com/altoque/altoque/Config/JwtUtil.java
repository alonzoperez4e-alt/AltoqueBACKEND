package com.altoque.altoque.Config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct; // Asegúrate de tener esta dependencia o usar javax.annotation
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    private final long EXPIRATION_TIME = 1000 * 60 * 60; // 1 hora
    private Key signingKey;

    // Inicializamos la llave una sola vez después de que Spring inyecte el valor
    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            logger.error("PELIGRO: La clave JWT es nula o muy corta. Verifica application.properties.");
            throw new IllegalArgumentException("La clave JWT debe tener al menos 32 caracteres.");
        }
        // Logueamos (parcialmente oculto) para confirmar que se cargó la clave correcta
        logger.info("JWT Secret cargado correctamente. Longitud: {}. Inicio: {}...",
                secret.length(), secret.substring(0, 4));

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private Key getSigningKey() {
        return this.signingKey;
    }

    public String generarToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String obtenerUsername(String token) {
        // Limpieza defensiva del token por si viene con prefijo o comillas
        String cleanToken = limpiarToken(token);

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(cleanToken)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            logger.error("Error al obtener username del token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validarToken(String token) {
        String cleanToken = limpiarToken(token);
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(cleanToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Firma JWT inválida. ¿El token fue manipulado o cambió la clave secreta? Error: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Token JWT mal formado: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.warn("Token JWT expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Token JWT no soportado: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Cadena claims JWT vacía o nula: {}", e.getMessage());
        }
        return false;
    }

    // Método auxiliar para limpiar errores comunes de frontend
    private String limpiarToken(String token) {
        if (token != null) {
            // Elimina comillas dobles si se guardó con JSON.stringify
            String cleaned = token.replace("\"", "").trim();
            // Elimina prefijo Bearer si viene pegado accidentalmente
            if (cleaned.startsWith("Bearer ")) {
                cleaned = cleaned.substring(7);
            }
            return cleaned;
        }
        return token;
    }
}