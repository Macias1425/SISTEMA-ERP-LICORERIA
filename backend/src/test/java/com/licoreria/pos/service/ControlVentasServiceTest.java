package com.licoreria.pos.service;

import com.licoreria.pos.dto.ControlVentasReglasDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.ConfiguracionPos;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.repository.AuditoriaRepository;
import com.licoreria.pos.repository.ConfiguracionPosRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Lazy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlVentasServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-23T12:00:00Z"), ZoneOffset.UTC);
    private static final Usuario OPERADOR = Usuario.builder().id(1L).rol(Rol.ADMIN).build();

    @Mock
    private ConfiguracionPosRepository configuracionRepository;
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private AuditoriaRepository auditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AutorizacionService autorizacionService;
    @Mock
    private PermisoService permisoService;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private @Lazy VentaService ventaService;

    private ControlVentasService controlVentasService;

    @BeforeEach
    void setUp() {
        controlVentasService = new ControlVentasService(
                configuracionRepository,
                ventaRepository,
                auditoriaRepository,
                usuarioRepository,
                autorizacionService,
                permisoService,
                auditoriaService,
                ventaService,
                CLOCK
        );
    }

    @Test
    void validarLimiteTurnoBloqueaCuandoSeAlcanzaMaximo() {
        when(configuracionRepository.findById(1L)).thenReturn(Optional.of(ConfiguracionPos.builder()
                .id(1L)
                .montoAlertaVenta(new BigDecimal("5000"))
                .montoSupervisorRequerido(new BigDecimal("15000"))
                .maxVentasPorTurno(3)
                .alertarOverridePrecio(true)
                .build()));
        when(ventaRepository.countByTurnoCajaIdAndEstado(10L, EstadoVenta.COMPLETADA)).thenReturn(3L);

        assertThrows(ReglaNegocioException.class, () -> controlVentasService.validarLimiteTurno(10L));
    }

    @Test
    void resumenCuentaVentasAltoMonto() {
        when(autorizacionService.operadorActual()).thenReturn(OPERADOR);
        when(configuracionRepository.findById(1L)).thenReturn(Optional.of(ConfiguracionPos.builder()
                .id(1L)
                .montoAlertaVenta(new BigDecimal("1000"))
                .montoSupervisorRequerido(new BigDecimal("5000"))
                .maxVentasPorTurno(0)
                .alertarOverridePrecio(true)
                .build()));
        when(ventaRepository.findByFechaBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        venta(1L, new BigDecimal("500"), null, 2L),
                        venta(2L, new BigDecimal("1200"), 9L, 2L)
                ));
        when(auditoriaRepository.findByAccionAndFechaHoraBetweenOrderByFechaHoraDesc(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        var resumen = controlVentasService.resumen(null, null);

        assertEquals(2L, resumen.getTotalVentas());
        assertEquals(1L, resumen.getVentasAltoMonto());
        assertEquals(1L, resumen.getOverridesPrecio());
        assertEquals(1, resumen.getPorCajero().size());
        assertEquals(new BigDecimal("850.00"), resumen.getTicketPromedio());
    }

    @Test
    void guardarReglasExigeConsistenciaMontos() {
        when(autorizacionService.operadorActual()).thenReturn(OPERADOR);

        assertThrows(ReglaNegocioException.class, () -> controlVentasService.guardarReglas(
                ControlVentasReglasDTO.builder()
                        .montoAlertaVenta(new BigDecimal("20000"))
                        .montoSupervisorRequerido(new BigDecimal("10000"))
                        .maxVentasPorTurno(0)
                        .alertarOverridePrecio(true)
                        .build()
        ));
    }

    private Venta venta(Long id, BigDecimal total, Long autorizadoPrecioPor, Long usuarioId) {
        return Venta.builder()
                .id(id)
                .numero("V-00" + id)
                .total(total)
                .usuarioId(usuarioId)
                .estado(EstadoVenta.COMPLETADA)
                .fecha(LocalDateTime.now(CLOCK))
                .autorizadoPrecioPor(autorizadoPrecioPor)
                .build();
    }
}
