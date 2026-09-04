package com.licoreria.pos.service;

import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Auditoria;
import com.licoreria.pos.model.NivelRiesgoAuditoria;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.AuditoriaRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-23T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AuditoriaRepository auditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private AuditoriaService auditoriaService;

    @BeforeEach
    void setUp() {
        auditoriaService = new AuditoriaService(auditoriaRepository, usuarioRepository, CLOCK);
    }

    @Test
    void resumenCalculaKpis() {
        when(auditoriaRepository.count()).thenReturn(75L);
        when(auditoriaRepository.countByFechaHoraGreaterThanEqual(any())).thenReturn(12L);
        when(auditoriaRepository.countByAccionIn(anyList())).thenReturn(30L, 8L);
        when(auditoriaRepository.findAllByOrderByFechaHoraDesc()).thenReturn(List.of(
                evento(1L, AccionAuditoria.CONFIGURACION),
                evento(2L, AccionAuditoria.VENTA),
                evento(3L, AccionAuditoria.ANULACION_FACTURA)
        ));

        var resumen = auditoriaService.resumen();

        assertEquals(75L, resumen.getEventosTotales());
        assertEquals(12L, resumen.getEventosHoy());
        assertEquals(30L, resumen.getVentasAuditadas());
        assertEquals(8L, resumen.getEventosCaja());
        assertEquals(2L, resumen.getRiesgoAlto());
    }

    @Test
    void buscarEnriqueceDtoYFiltraPorCategoriaCaja() {
        when(auditoriaRepository.buscar(eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(List.of(-1L))))
                .thenReturn(List.of(
                        evento(1L, AccionAuditoria.APERTURA_CAJA),
                        evento(2L, AccionAuditoria.VENTA)
                ));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(
                Usuario.builder().id(5L).nombreCompleto("José Macías").build()
        ));

        var resultados = auditoriaService.buscar(null, null, null, null, null, null, null, null, "caja");

        assertEquals(1, resultados.size());
        assertEquals("Caja abierta", resultados.get(0).getEventoEtiqueta());
        assertEquals("APERTURA_CAJA", resultados.get(0).getCodigoEvento());
        assertEquals(NivelRiesgoAuditoria.MEDIO, resultados.get(0).getNivelRiesgo());
        assertEquals("Caja", resultados.get(0).getModulo());
        assertEquals("José Macías", resultados.get(0).getUsuarioNombre());
    }

    @Test
    void buscarFiltraPorNombreUsuario() {
        when(usuarioRepository.buscar("maria", null, null)).thenReturn(List.of(
                Usuario.builder().id(5L).nombreCompleto("María López").build()
        ));
        when(auditoriaRepository.buscar(eq("maria"), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(List.of(5L))))
                .thenReturn(List.of(evento(1L, AccionAuditoria.VENTA)));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(
                Usuario.builder().id(5L).nombreCompleto("María López").build()
        ));

        var resultados = auditoriaService.buscar("maria", null, null, null, null, null, null, null, null);

        assertEquals(1, resultados.size());
        assertEquals("María López", resultados.get(0).getUsuarioNombre());
    }

    @Test
    void registrarPersisteEvento() {
        when(auditoriaRepository.save(any(Auditoria.class))).thenAnswer(invocation -> {
            Auditoria log = invocation.getArgument(0);
            log.setId(99L);
            return log;
        });

        auditoriaService.registrar(10L, "ADMIN", AccionAuditoria.CONFIGURACION,
                "Configuracion", 1L, null, "{\"cambio\":true}", "Parámetros actualizados");

        verify(auditoriaRepository).save(any(Auditoria.class));
    }

    @Test
    void nivelRiesgoAltoParaAnulacion() {
        assertEquals(NivelRiesgoAuditoria.ALTO, AuditoriaService.nivelRiesgo(AccionAuditoria.ANULACION_FACTURA));
        assertEquals(NivelRiesgoAuditoria.BAJO, AuditoriaService.nivelRiesgo(AccionAuditoria.VENTA));
    }

    private Auditoria evento(Long id, AccionAuditoria accion) {
        return Auditoria.builder()
                .id(id)
                .usuarioId(5L)
                .rol("ADMIN")
                .accion(accion)
                .entidad("TurnoCaja")
                .entidadId(id)
                .fechaHora(LocalDateTime.now(CLOCK))
                .build();
    }
}
