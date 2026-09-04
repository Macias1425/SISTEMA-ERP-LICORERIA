package com.licoreria.pos.service;

import com.licoreria.pos.dto.ResetPasswordDTO;
import com.licoreria.pos.dto.UsuarioCreateDTO;
import com.licoreria.pos.dto.UsuarioUpdateDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.EstadoTurnoCaja;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TurnoCaja;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.TurnoCajaRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.security.PoliticaPassword;
import com.licoreria.pos.security.UsuarioActualService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TurnoCajaRepository turnoCajaRepository;
    @Mock
    private UsuarioActualService usuarioActualService;
    @Mock
    private PermisoService permisoService;
    @Mock
    private HorarioAccesoService horarioAccesoService;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(
                usuarioRepository,
                turnoCajaRepository,
                new BCryptPasswordEncoder(),
                new PoliticaPassword(),
                usuarioActualService,
                permisoService,
                horarioAccesoService,
                Clock.system(ZoneOffset.UTC)
        );
    }

    @Test
    void crearUsuarioExigeCambioDePassword() {
        when(usuarioRepository.existsByUsernameIgnoreCase("caja01")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(10L);
            return usuario;
        });
        when(usuarioActualService.usuario()).thenReturn(Usuario.builder().id(99L).rol(Rol.ADMIN).build());
        when(turnoCajaRepository.findByUsuarioIdAndEstado(10L, EstadoTurnoCaja.ABIERTO)).thenReturn(Optional.empty());

        var creado = usuarioService.crear(UsuarioCreateDTO.builder()
                .username("Caja01")
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

        UsuarioUpdateDTO dto = UsuarioUpdateDTO.builder()
                .nombreCompleto("Admin")
                .rol(Rol.ADMIN)
                .activo(false)
                .build();
        assertThrows(ReglaNegocioException.class, () -> usuarioService.actualizar(1L, dto));
    }

    @Test
    void noDesactivaUsuarioConTurnoAbierto() {
        Usuario cajero = Usuario.builder()
                .id(3L)
                .username("cajero")
                .nombreCompleto("Cajero")
                .rol(Rol.CAJERO)
                .activo(true)
                .build();
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(cajero));
        when(usuarioActualService.usuario()).thenReturn(Usuario.builder().id(1L).rol(Rol.ADMIN).build());
        when(turnoCajaRepository.findByUsuarioIdAndEstado(3L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.of(TurnoCaja.builder().id(8L).build()));

        UsuarioUpdateDTO dto = UsuarioUpdateDTO.builder()
                .nombreCompleto("Cajero")
                .rol(Rol.CAJERO)
                .activo(false)
                .build();
        assertThrows(ReglaNegocioException.class, () -> usuarioService.actualizar(3L, dto));
    }

    @Test
    void noReseteaPasswordDeUsuarioBloqueado() {
        Usuario bloqueado = Usuario.builder()
                .id(4L)
                .username("inactivo")
                .nombreCompleto("Inactivo")
                .rol(Rol.CAJERO)
                .activo(false)
                .build();
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(bloqueado));

        assertThrows(ReglaNegocioException.class, () -> usuarioService.resetearPassword(
                4L,
                new ResetPasswordDTO("Clave1234")
        ));
    }

    @Test
    void listaConFiltros() {
        Usuario usuario = Usuario.builder()
                .id(5L)
                .username("almacen")
                .nombreCompleto("Almacén")
                .rol(Rol.ALMACENISTA)
                .activo(true)
                .build();
        when(usuarioRepository.buscarPaginado(
                eq("alma"), eq(Rol.ALMACENISTA), eq(true), isNull(), eq(true), anyList(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(usuario)));
        when(usuarioActualService.usuario()).thenReturn(Usuario.builder().id(1L).rol(Rol.ADMIN).build());
        when(turnoCajaRepository.findByUsuarioIdAndEstado(5L, EstadoTurnoCaja.ABIERTO)).thenReturn(Optional.empty());

        var lista = usuarioService.listar("alma", Rol.ALMACENISTA, true);

        assertEquals(1, lista.size());
        assertEquals("almacen", lista.get(0).getUsername());
        assertFalse(lista.get(0).getEsSesionActual());
    }

    @Test
    void marcaFlagsDeSesionActual() {
        Usuario admin = Usuario.builder()
                .id(1L)
                .username("admin")
                .nombreCompleto("Admin")
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioActualService.usuario()).thenReturn(admin);
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(1L);
        when(turnoCajaRepository.findByUsuarioIdAndEstado(1L, EstadoTurnoCaja.ABIERTO)).thenReturn(Optional.empty());

        var dto = usuarioService.obtener(1L);

        assertTrue(dto.getEsSesionActual());
        assertFalse(dto.getPuedeDesactivar());
        assertFalse(dto.getPuedeCambiarRol());
    }
}
