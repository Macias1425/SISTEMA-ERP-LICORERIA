package com.licoreria.pos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licoreria.pos.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   CambioPasswordObligatorioFilter cambioPasswordFilter,
                                                   HorarioAccesoFilter horarioAccesoFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        // La autorización por módulo se declara con permisos en cada controlador
                        // (@PreAuthorize("@acceso.tiene(...)")). Filtrar por rol aquí anulaba
                        // los permisos adicionales concedidos a un usuario.
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, exception) ->
                                escribir(response, objectMapper, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Debe iniciar sesión"))
                        .accessDeniedHandler((request, response, exception) ->
                                escribir(response, objectMapper, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                                        "No tiene permiso para esta operación"))
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(cambioPasswordFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(horarioAccesoFilter, CambioPasswordObligatorioFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Evita que Spring Boot registre el filtro JWT dos veces (cadena de seguridad + servlet). */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CambioPasswordObligatorioFilter> cambioPasswordFilterRegistration(
            CambioPasswordObligatorioFilter filter) {
        FilterRegistrationBean<CambioPasswordObligatorioFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<HorarioAccesoFilter> horarioAccesoFilterRegistration(
            HorarioAccesoFilter filter) {
        FilterRegistrationBean<HorarioAccesoFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void escribir(HttpServletResponse response, ObjectMapper mapper, HttpStatus status,
                          String codigo, String mensaje) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .codigo(codigo)
                .mensaje(mensaje)
                .build());
    }
}
