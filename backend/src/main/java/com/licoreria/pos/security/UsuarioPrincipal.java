package com.licoreria.pos.security;

import com.licoreria.pos.model.Rol;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class UsuarioPrincipal extends User {

    private final Long id;
    private final Rol rol;
    private final boolean debeCambiarPassword;

    public UsuarioPrincipal(Long id, String username, String password, Rol rol, boolean activo,
                            boolean debeCambiarPassword) {
        super(
                username,
                password,
                activo,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))
        );
        this.id = id;
        this.rol = rol;
        this.debeCambiarPassword = debeCambiarPassword;
    }
}
