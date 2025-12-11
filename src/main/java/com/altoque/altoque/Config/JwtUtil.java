package com.altoque.altoque.Config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails; // Importante
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10 horas (ajustado para operación bancaria estándar)
    private Key signingKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            logger.error("PELIGRO: La clave JWT es nula o muy corta. Verifica application.properties.");
            throw new IllegalArgumentException("La clave JWT debe tener al menos 32 caracteres.");
        }
        logger.info("JWT Secret cargado correctamente.");
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
        String cleanToken = limpiarToken(token);

        // Validación estructural básica antes de parsear
        if (cleanToken == null || cleanToken.isEmpty() || !cleanToken.contains(".")) {
            // logger.warn("Intento de parsear token inválido o vacío");
            return null; // Retornamos null para que el filtro lo maneje
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(cleanToken)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            // No lanzamos la excepción aquí, dejamos que el filtro decida qué hacer
            // logger.debug("Error extrayendo username: " + e.getMessage());
            return null;
        }
    }

    public boolean validarToken(String token, UserDetails userDetails) { // Firma ajustada para recibir UserDetails
        String cleanToken = limpiarToken(token);

        if (cleanToken == null || cleanToken.isEmpty()) return false;

        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(cleanToken);

            String username = claims.getBody().getSubject();
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(claims));

        } catch (SignatureException e) {
            logger.error("Firma JWT inválida: {}", e.getMessage());
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

    private boolean isTokenExpired(Jws<Claims> claims) {
        return claims.getBody().getExpiration().before(new Date());
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
            // Elimina cadena "null" literal
            if ("null".equals(cleaned) || "undefined".equals(cleaned)) {
                return null;
            }
            return cleaned;
        }
        return null;
    }
}