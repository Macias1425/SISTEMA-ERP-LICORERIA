package com.licoreria.pos.service;

import com.licoreria.pos.dto.AnulacionFacturaDTO;
import com.licoreria.pos.dto.AutorizacionDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.DetalleVenta;
import com.licoreria.pos.model.EstadoFactura;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.Factura;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.repository.ClienteRepository;
import com.licoreria.pos.repository.FacturaRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturaServiceAnulacionTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private AutorizacionService autorizacionService;
    @Mock
    private AuditoriaService auditoriaService;

    private FacturaService facturaService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 8, 18, 21, 0).atZone(ZoneId.of("America/Tegucigalpa")).toInstant(),
                ZoneId.of("America/Tegucigalpa")
        );
        facturaService = new FacturaService(
                facturaRepository, ventaRepository, clienteRepository,
                inventarioService, autorizacionService, auditoriaService, clock
        );
    }

    @Test
    void cajeroNoPuedeAnularSinAdmin() {
        Factura factura = Factura.builder().id(1L).estado(EstadoFactura.EMITIDA).ventaId(9L).build();
        when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));
        when(autorizacionService.exigirRol(Rol.CAJERO, Rol.ADMIN)).thenReturn(
                Usuario.builder().id(2L).rol(Rol.CAJERO).activo(true).build());
        when(autorizacionService.exigirCredencialAdmin(any(), anyString()))
                .thenThrow(new ReglaNegocioException("AUTORIZACION_REQUERIDA", "Se requiere administrador"));

        AnulacionFacturaDTO dto = AnulacionFacturaDTO.builder()
                .autorizacion(AutorizacionDTO.builder().username("cajero").password("cajero123").build())
                .motivo("Error de captura")
                .build();

        assertThrows(ReglaNegocioException.class, () -> facturaService.anular(1L, dto));
        verify(inventarioService, never()).ingresar(anyLong(), anyLong(), anyInt(), any(), anyString(), anyLong());
    }

    @Test
    void adminAnulaYDevuelveStock() {
        Factura factura = Factura.builder()
                .id(1L)
                .numero("F-1")
                .estado(EstadoFactura.EMITIDA)
                .ventaId(9L)
                .total(BigDecimal.TEN)
                .build();
        DetalleVenta detalle = DetalleVenta.builder().productoId(5L).presentacionId(8L).cantidad(2).build();
        Venta venta = Venta.builder().id(9L).estado(EstadoVenta.COMPLETADA).detalles(List.of(detalle)).build();
        detalle.setVenta(venta);
        Usuario cajero = Usuario.builder().id(2L).rol(Rol.CAJERO).activo(true).build();
        Usuario admin = Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build();

        when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));
        when(autorizacionService.exigirRol(Rol.CAJERO, Rol.ADMIN)).thenReturn(cajero);
        when(autorizacionService.exigirCredencialAdmin(any(), anyString())).thenReturn(admin);
        when(ventaRepository.findById(9L)).thenReturn(Optional.of(venta));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnulacionFacturaDTO dto = AnulacionFacturaDTO.builder()
                .autorizacion(AutorizacionDTO.builder().username("admin").password("admin123").build())
                .motivo("Cliente desistio de la compra")
                .build();

        var anulada = facturaService.anular(1L, dto);

        assertEquals(EstadoFactura.ANULADA, anulada.getEstado());
        verify(inventarioService).ingresar(5L, 8L, 2, TipoMovimiento.ANULACION,
                "Anulación factura F-1: Cliente desistio de la compra", 1L);
        verify(auditoriaService).registrar(eq(admin), any(), eq("Factura"), eq(1L), any(), any(), any());
    }
}
