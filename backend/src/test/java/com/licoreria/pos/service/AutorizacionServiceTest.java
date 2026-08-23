package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.dto.AutorizacionDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.security.LoginAttemptService;
import com.licoreria.pos.security.UsuarioActualService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioActualService usuarioActualService;

    private AutorizacionService autorizacionService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        PosProperties properties = new PosProperties();
        autorizacionService = new AutorizacionService(
                usuarioRepository,
                usuarioActualService,
                passwordEncoder,
                new LoginAttemptService(properties, Clock.system(ZoneOffset.UTC))
        );
    }

    @Test
    void cajeroNoAbreModuloDeAdmin() {
        when(usuarioActualService.usuario()).thenReturn(Usuario.builder()
                .id(2L).username("cajero").rol(Rol.CAJERO).activo(true).build());
        assertThrows(AccessDeniedException.class, () -> autorizacionService.exigirRol(Rol.ADMIN));
    }

    @Test
    void credencialAdminValida() {
        Usuario admin = Usuario.builder()
                .id(1L)
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        Usuario autorizado = autorizacionService.exigirCredencialAdmin(
                new AutorizacionDTO("admin", "admin123"),
                "Se requiere administrador"
        );
        assertEquals(1L, autorizado.getId());
    }

    @Test
    void credencialAdminFalsaNoRevelaUsuario() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.empty());
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> autorizacionService.exigirCredencialAdmin(
                        new AutorizacionDTO("admin", "otra"),
                        "Se requiere administrador"));
        assertEquals("AUTORIZACION_REQUERIDA", ex.getCodigo());
    }
}
