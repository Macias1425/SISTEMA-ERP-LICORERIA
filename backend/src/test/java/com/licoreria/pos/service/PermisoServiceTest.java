package com.licoreria.pos.service;

import com.licoreria.pos.dto.ActualizarPermisosUsuarioDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.model.UsuarioPermiso;
import com.licoreria.pos.repository.UsuarioPermisoRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermisoServiceTest {

    @Mock
    private UsuarioPermisoRepository usuarioPermisoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AutorizacionService autorizacionService;
    @Mock
    private AuditoriaService auditoriaService;

    private PermisoService permisoService;

    @BeforeEach
    void setUp() {
        permisoService = new PermisoService(
                usuarioPermisoRepository,
                usuarioRepository,
                autorizacionService,
                auditoriaService
        );
    }

    @Test
    void adminTieneTodosLosPermisos() {
        Usuario admin = Usuario.builder().id(1L).rol(Rol.ADMIN).build();
        Set<Permiso> efectivos = permisoService.permisosEfectivos(admin);
        assertEquals(Permiso.values().length, efectivos.size());
    }

    @Test
    void cajeroTieneConjuntoEsperadoPorRol() {
        Set<Permiso> esperados = Set.of(
                Permiso.VENTAS_CREAR,
                Permiso.CAJA_OPERAR
        );
        assertEquals(esperados, permisoService.permisosBasePorRol(Rol.CAJERO));
    }

    @Test
    void cajeroTienePermisosOperativos() {
        Usuario cajero = Usuario.builder().id(2L).rol(Rol.CAJERO).build();
        when(usuarioPermisoRepository.findByUsuarioIdOrderByPermisoAsc(2L)).thenReturn(List.of());

        Set<Permiso> efectivos = permisoService.permisosEfectivos(cajero);

        assertTrue(efectivos.contains(Permiso.VENTAS_CREAR));
        assertTrue(efectivos.contains(Permiso.CAJA_OPERAR));
        assertTrue(!efectivos.contains(Permiso.USUARIOS_GESTIONAR));
    }

    @Test
    void permisosAdicionalesSeFusionanConRol() {
        Usuario cajero = Usuario.builder().id(2L).rol(Rol.CAJERO).build();
        when(usuarioPermisoRepository.findByUsuarioIdOrderByPermisoAsc(2L))
                .thenReturn(List.of(UsuarioPermiso.builder().permiso(Permiso.CONTROL_VENTAS_VER).build()));

        Set<Permiso> efectivos = permisoService.permisosEfectivos(cajero);

        assertTrue(efectivos.contains(Permiso.CONTROL_VENTAS_VER));
        assertTrue(efectivos.contains(Permiso.VENTAS_CREAR));
    }

    @Test
    void rechazaPermisoRedundanteConRol() {
        Usuario cajero = Usuario.builder().id(2L).username("cajero1").rol(Rol.CAJERO).build();
        when(autorizacionService.operadorActual()).thenReturn(Usuario.builder().id(9L).rol(Rol.ADMIN).build());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(cajero));

        assertThrows(ReglaNegocioException.class, () -> permisoService.actualizarAdicionales(
                2L,
                ActualizarPermisosUsuarioDTO.builder()
                        .permisosAdicionales(List.of(Permiso.VENTAS_CREAR))
                        .build()
        ));
    }

    @Test
    void actualizaPermisosAdicionales() {
        Usuario admin = Usuario.builder().id(9L).rol(Rol.ADMIN).build();
        Usuario cajero = Usuario.builder().id(2L).username("cajero1").nombreCompleto("Cajero").rol(Rol.CAJERO).build();
        when(autorizacionService.operadorActual()).thenReturn(admin);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(cajero));
        when(usuarioPermisoRepository.findByUsuarioIdOrderByPermisoAsc(9L)).thenReturn(List.of());
        when(usuarioPermisoRepository.findByUsuarioIdOrderByPermisoAsc(2L))
                .thenReturn(List.of())
                .thenReturn(List.of(UsuarioPermiso.builder().usuarioId(2L).permiso(Permiso.CONTROL_VENTAS_VER).build()));

        var resultado = permisoService.actualizarAdicionales(
                2L,
                ActualizarPermisosUsuarioDTO.builder()
                        .permisosAdicionales(List.of(Permiso.CONTROL_VENTAS_VER))
                        .build()
        );

        verify(usuarioPermisoRepository).deleteByUsuarioId(2L);
        verify(usuarioPermisoRepository).save(any(UsuarioPermiso.class));
        assertEquals(1, resultado.getPermisosAdicionales().size());
        assertEquals(Permiso.CONTROL_VENTAS_VER, resultado.getPermisosAdicionales().get(0));
    }
}
