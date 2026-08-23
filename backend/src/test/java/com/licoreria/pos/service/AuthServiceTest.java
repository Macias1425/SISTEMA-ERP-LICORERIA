package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.dto.LoginRequestDTO;
import com.licoreria.pos.exception.AutenticacionException;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.security.CustomUserDetailsService;
import com.licoreria.pos.security.JwtService;
import com.licoreria.pos.security.LoginAttemptService;
import com.licoreria.pos.security.PoliticaPassword;
import com.licoreria.pos.security.UsuarioActualService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioActualService usuarioActualService;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        PosProperties properties = new PosProperties();
        Clock clock = Clock.system(ZoneOffset.UTC);
        passwordEncoder = new BCryptPasswordEncoder();
        CustomUserDetailsService userDetailsService = new CustomUserDetailsService(usuarioRepository);
        authService = new AuthService(
                usuarioRepository,
                passwordEncoder,
                new JwtService(properties),
                userDetailsService,
                new LoginAttemptService(properties, clock),
                new PoliticaPassword(),
                usuarioActualService,
                clock
        );
    }

    @Test
    void loginExitosoDevuelveToken() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .username("admin")
                .password(passwordEncoder.encode("admin1234"))
                .nombreCompleto("Administrador")
                .rol(Rol.ADMIN)
                .activo(true)
                .debeCambiarPassword(false)
                .build();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var respuesta = authService.login(new LoginRequestDTO("admin", "admin1234"));

        assertEquals("admin", respuesta.getUsername());
        assertEquals(Rol.ADMIN, respuesta.getRol());
        assertEquals("Bearer", respuesta.getTipoToken());
        assertNotNull(respuesta.getToken());
    }

    @Test
    void loginFalloNoRevelaSiElUsuarioExiste() {
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());
        AutenticacionException ex = assertThrows(AutenticacionException.class,
                () -> authService.login(new LoginRequestDTO("nadie", "secreto1")));
        assertEquals("CREDENCIALES_INVALIDAS", ex.getCodigo());
    }

    @Test
    void bloqueaTrasCincoFallos() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.empty());
        LoginRequestDTO dto = new LoginRequestDTO("admin", "clave-mala");
        for (int i = 0; i < 5; i++) {
            AutenticacionException fallo = assertThrows(AutenticacionException.class, () -> authService.login(dto));
            assertEquals("CREDENCIALES_INVALIDAS", fallo.getCodigo());
        }
        AutenticacionException bloqueo = assertThrows(AutenticacionException.class, () -> authService.login(dto));
        assertEquals("CUENTA_BLOQUEADA", bloqueo.getCodigo());
    }
}
