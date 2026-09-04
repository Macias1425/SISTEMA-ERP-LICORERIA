package com.licoreria.pos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licoreria.pos.dto.ErrorResponse;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.service.HorarioAccesoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class HorarioAccesoFilter extends OncePerRequestFilter {

    private final HorarioAccesoService horarioAccesoService;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = ruta(request);
        return terminaEn(path, "/api/auth/login")
                || terminaEn(path, "/api/auth/me")
                || terminaEn(path, "/api/auth/cambiar-password");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal principal) {
            Usuario usuario = usuarioRepository.findById(principal.getId()).orElse(null);
            if (usuario != null && !horarioAccesoService.accesoPermitidoAhora(usuario)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.FORBIDDEN.value())
                        .codigo("FUERA_DE_HORARIO_ACCESO")
                        .mensaje("Su cuenta no puede operar fuera del horario de acceso configurado")
                        .build());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String ruta(HttpServletRequest request) {
        String servlet = request.getServletPath();
        if (servlet != null && !servlet.isBlank() && !"/".equals(servlet)) {
            return servlet;
        }
        return request.getRequestURI();
    }

    private boolean terminaEn(String path, String sufijo) {
        return path != null && path.endsWith(sufijo);
    }
}
