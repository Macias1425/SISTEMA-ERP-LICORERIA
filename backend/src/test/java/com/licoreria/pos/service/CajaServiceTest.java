package com.licoreria.pos.service;

import com.licoreria.pos.dto.CierreCajaDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.EstadoTurnoCaja;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.FormaPago;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TurnoCaja;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.repository.TurnoCajaRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CajaServiceTest {

    @Mock
    private TurnoCajaRepository turnoCajaRepository;
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AutorizacionService autorizacionService;
    @Mock
    private AccesoService accesoService;
    @Mock
    private AuditoriaService auditoriaService;

    private CajaService cajaService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 8, 18, 21, 0).atZone(ZoneId.of("America/Managua")).toInstant(),
                ZoneId.of("America/Managua")
        );
        cajaService = new CajaService(
                turnoCajaRepository, ventaRepository, usuarioRepository, autorizacionService, accesoService,
                auditoriaService, clock);
    }

    @Test
    void noCierraSinConteoFisico() {
        CierreCajaDTO dto = CierreCajaDTO.builder().montoFisico(null).build();
        assertThrows(ReglaNegocioException.class, () -> cajaService.cerrar(1L, dto));
    }

    @Test
    void calculaFaltanteEnArqueo() {
        TurnoCaja turno = TurnoCaja.builder()
                .id(1L)
                .usuarioId(2L)
                .montoInicial(new BigDecimal("100.00"))
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();
        Usuario cajero = Usuario.builder().id(2L).rol(Rol.CAJERO).activo(true).build();
        Venta venta = Venta.builder()
                .total(new BigDecimal("50.00"))
                .formaPago(FormaPago.EFECTIVO)
                .estado(EstadoVenta.COMPLETADA)
                .build();

        when(turnoCajaRepository.findById(1L)).thenReturn(Optional.of(turno));
        when(accesoService.exigirPermiso(Permiso.CAJA_OPERAR)).thenReturn(cajero);
        when(ventaRepository.findByTurnoCajaIdAndEstado(1L, EstadoVenta.COMPLETADA)).thenReturn(List.of(venta));
        when(turnoCajaRepository.save(any(TurnoCaja.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var cerrado = cajaService.cerrar(1L, CierreCajaDTO.builder()
                .montoFisico(new BigDecimal("140.00"))
                .build());

        assertEquals(new BigDecimal("150.00"), cerrado.getMontoEsperado());
        assertEquals(new BigDecimal("-10.00"), cerrado.getDiferencia());
        assertEquals("FALTANTE", cerrado.getResultadoArqueo());
    }

    @Test
    void estadoSinTurnoNoEsError() {
        Usuario cajero = Usuario.builder().id(2L).rol(Rol.CAJERO).activo(true).build();
        when(autorizacionService.operadorActual()).thenReturn(cajero);
        when(turnoCajaRepository.findByUsuarioIdAndEstado(2L, EstadoTurnoCaja.ABIERTO)).thenReturn(Optional.empty());

        var estado = cajaService.estado();
        assertEquals(false, estado.isAbierta());
    }
}
