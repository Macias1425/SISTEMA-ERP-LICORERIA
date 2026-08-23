package com.licoreria.pos.service;

import com.licoreria.pos.dto.AutorizacionDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.security.LoginAttemptService;
import com.licoreria.pos.security.UsuarioActualService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AutorizacionService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioActualService usuarioActualService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public Usuario operadorActual() {
        Usuario usuario = usuarioActualService.usuario();
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ReglaNegocioException("USUARIO_INACTIVO", "El usuario no está activo");
        }
        return usuario;
    }

    public Usuario exigirRol(Rol... permitidos) {
        Usuario usuario = operadorActual();
        boolean autorizado = Arrays.stream(permitidos).anyMatch(rol -> usuario.getRol() == rol);
        if (!autorizado) {
            throw new AccessDeniedException("No tiene permiso para esta operación");
        }
        return usuario;
    }

    /**
     * Doble control: el operador autenticado no basta; hay que teclear usuario y clave de un ADMIN activo.
     * El mensaje de fallo es genérico para no revelar si el usuario existe.
     */
    public Usuario exigirCredencialAdmin(AutorizacionDTO autorizacion, String mensaje) {
        if (autorizacion == null
                || autorizacion.getUsername() == null || autorizacion.getUsername().isBlank()
                || autorizacion.getPassword() == null || autorizacion.getPassword().isBlank()) {
            throw new ReglaNegocioException("AUTORIZACION_REQUERIDA", mensaje);
        }

        String username = autorizacion.getUsername().trim();
        if (loginAttemptService.estaBloqueado(username)) {
            throw new ReglaNegocioException("CUENTA_BLOQUEADA",
                    "Demasiados intentos fallidos. Espere 15 minutos o contacte al administrador");
        }

        Usuario supervisor = usuarioRepository.findByUsername(username).orElse(null);
        if (supervisor == null
                || !Boolean.TRUE.equals(supervisor.getActivo())
                || supervisor.getRol() != Rol.ADMIN
                || !passwordEncoder.matches(autorizacion.getPassword(), supervisor.getPassword())) {
            loginAttemptService.registrarFallo(username);
            throw new ReglaNegocioException("AUTORIZACION_REQUERIDA", mensaje);
        }

        loginAttemptService.registrarExito(username);
        return supervisor;
    }
}
