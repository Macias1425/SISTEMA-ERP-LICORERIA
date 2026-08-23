package com.licoreria.pos.security;

import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return new UsuarioPrincipal(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.getRol(),
                Boolean.TRUE.equals(usuario.getActivo()),
                Boolean.TRUE.equals(usuario.getDebeCambiarPassword())
        );
    }
}
