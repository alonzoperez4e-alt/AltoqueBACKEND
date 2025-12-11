package com.altoque.altoque.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. REGLA CRÍTICA FLOW: El Webhook debe ser público SIEMPRE
                        .requestMatchers("/api/flow/confirm", "/flow/confirm").permitAll()

                        // 2. REGLA CRÍTICA FLOW: La creación y estado requieren usuario (Token)
                        // Deben ir ANTES de los permitAll genéricos para que no se "cuelen" como públicos
                        .requestMatchers("/api/flow/create", "/api/flow/status").authenticated()

                        // 3. RESTAURACIÓN DE TUS RUTAS PÚBLICAS (Como lo tenías antes)
                        .requestMatchers(
                                "/auth/**",
                                "/api/auth/**",
                                "/api/clientes/**",
                                "/api/notificaciones/**",
                                "/api/prestamos/**",
                                "/api/operaciones/**",
                                "/api/pagos/**",
                                "/api/caja/**",
                                "/api/comprobantes/**",
                                "/api/public/**",
                                "/error"
                        ).permitAll()

                        // 4. Preflight requests (CORS) siempre permitidos
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 5. Todo lo demás cerrado por defecto
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();

        // --- RESTAURACIÓN DE TU CONFIGURACIÓN CORS ---
        // Esto soluciona los problemas de conexión con Vercel y Localhost
        cors.setAllowedOriginPatterns(List.of(
                "https://altoque-frontend.vercel.app", // Producción Frontend
                "http://altoque-frontend.vercel.app", // Producción Frontend (http fallback)
                "https://al-toque-d0b27cb5aec4.herokuapp.com", // Producción Backend (self)
                "http://localhost:8080",
                "http://localhost:8081",
                "http://localhost:5173",
                "http://localhost:3000"
                // Nota: He removido "*" para mayor seguridad en producción,
                // pero si tienes subdominios dinámicos podrías necesitarlo.
        ));

        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        cors.setAllowedHeaders(List.of("*")); // Permitir todos los headers
        cors.setAllowCredentials(true); // Permitir credenciales/cookies

        cors.setExposedHeaders(List.of("Authorization", "Content-Type", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}