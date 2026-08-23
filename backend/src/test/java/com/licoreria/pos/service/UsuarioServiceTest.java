package com.licoreria.pos.service;

import com.licoreria.pos.dto.UsuarioCreateDTO;
import com.licoreria.pos.dto.UsuarioUpdateDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.security.PoliticaPassword;
import com.licoreria.pos.security.UsuarioActualService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioActualService usuarioActualService;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(
                usuarioRepository,
                new BCryptPasswordEncoder(),
                new PoliticaPassword(),
                usuarioActualService,
                Clock.system(ZoneOffset.UTC)
        );
    }

    @Test
    void crearUsuarioExigeCambioDePassword() {
        when(usuarioRepository.existsByUsername("caja01")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(10L);
            return usuario;
        });

        var creado = usuarioService.crear(UsuarioCreateDTO.builder()
                .username("caja01")
                .password("Clave1234")
                .nombreCompleto("Caja Uno")
                .rol(Rol.CAJERO)
                .build());

        assertEquals("caja01", creado.getUsername());
        assertTrue(creado.getDebeCambiarPassword());
    }

    @Test
    void noDesactivaAlUltimoAdmin() {
        Usuario admin = Usuario.builder()
                .id(1L)
                .username("admin")
                .nombreCompleto("Admin")
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioActualService.usuario()).thenReturn(Usuario.builder().id(2L).rol(Rol.ADMIN).build());
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(1L);

        UsuarioUpdateDTO dto = new UsuarioUpdateDTO("Admin", Rol.ADMIN, false);
        assertThrows(ReglaNegocioException.class, () -> usuarioService.actualizar(1L, dto));
    }
}
