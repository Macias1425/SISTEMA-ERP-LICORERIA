package com.licoreria.pos.security;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.model.Rol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @Test
    void generaTokenConUsuarioYRol() {
        PosProperties properties = new PosProperties();
        JwtService jwtService = new JwtService(properties);
        UsuarioPrincipal principal = new UsuarioPrincipal(
                7L, "admin", "hash", Rol.ADMIN, true, false);

        String token = jwtService.generar(principal);

        assertEquals("admin", jwtService.extraerUsername(token));
        assertTrue(jwtService.esValido(token, "admin"));
    }
}
