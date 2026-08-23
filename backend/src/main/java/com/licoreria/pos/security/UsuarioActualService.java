package com.licoreria.pos.security;

import com.licoreria.pos.exception.AutenticacionException;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioActualService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new AutenticacionException("NO_AUTENTICADO", "Debe iniciar sesión");
        }
        return principal;
    }

    public Usuario usuario() {
        UsuarioPrincipal principal = principal();
        return usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new AutenticacionException("NO_AUTENTICADO", "Debe iniciar sesión"));
    }

    public Long id() {
        return principal().getId();
    }
}
