package com.licoreria.pos.service;

import com.licoreria.pos.dto.EstadoNormativaDTO;
import com.licoreria.pos.dto.TurnoCajaDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Compra;
import com.licoreria.pos.model.DetalleVenta;
import com.licoreria.pos.model.EstadoFactura;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.Factura;
import com.licoreria.pos.model.FormaPago;
import com.licoreria.pos.model.NivelVencimiento;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.repository.CompraRepository;
import com.licoreria.pos.repository.FacturaRepository;
import com.licoreria.pos.repository.ProductoRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private CompraRepository compraRepository;
    @Mock
    private TurnoCajaRepository turnoCajaRepository;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private HorarioVentaService horarioVentaService;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CajaService cajaService;

    private ReporteService reporteService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 8, 23, 19, 0).atZone(ZoneId.of("America/Managua")).toInstant(),
                ZoneId.of("America/Managua")
        );
        reporteService = new ReporteService(
                ventaRepository, facturaRepository, compraRepository, turnoCajaRepository,
                inventarioService, horarioVentaService, productoRepository, usuarioRepository,
                cajaService, clock
        );
    }

    @Test
    void calculaResultadoDelDiaComoVentasMenosCompras() {
        when(ventaRepository.findByFechaBetween(any(), any())).thenReturn(List.of(
                Venta.builder().estado(EstadoVenta.COMPLETADA)
                        .subtotal(new BigDecimal("250.00"))
                        .impuesto(new BigDecimal("37.50"))
                        .total(new BigDecimal("287.50"))
                        .build(),
                Venta.builder().estado(EstadoVenta.ANULADA)
                        .total(new BigDecimal("100.00"))
                        .build()
        ));
        when(facturaRepository.findByFechaEmisionBetween(any(), any())).thenReturn(List.of(
                Factura.builder().estado(EstadoFactura.EMITIDA).total(new BigDecimal("287.50")).build()
        ));
        when(compraRepository.findByFechaBetween(any(), any())).thenReturn(List.of(
                Compra.builder().total(new BigDecimal("1080.00")).build()
        ));
        when(turnoCajaRepository.countByEstado(any())).thenReturn(1L);
        when(inventarioService.listarAlertas()).thenReturn(List.of());
        when(horarioVentaService.estadoActual()).thenReturn(EstadoNormativaDTO.builder()
                .ventaLicorPermitidaAhora(true)
                .mensaje("ok")
                .build());

        var resumen = reporteService.resumenDelDia();

        assertEquals(1, resumen.getVentasCantidad());
        assertEquals(new BigDecimal("287.50"), resumen.getVentasTotal());
        assertEquals(new BigDecimal("1080.00"), resumen.getComprasTotal());
        assertEquals(new BigDecimal("-792.50"), resumen.getResultadoDia());
        assertEquals(1, resumen.getFacturasEmitidas());
    }

    @Test
    void clasificaVencidosPorVencerYSinFecha() {
        LocalDate hoy = LocalDate.of(2026, 8, 23);
        assertEquals(NivelVencimiento.VENCIDO, ReporteService.clasificar(hoy.minusDays(1), hoy, 30));
        assertEquals(NivelVencimiento.POR_VENCER, ReporteService.clasificar(hoy.plusDays(15), hoy, 30));
        assertEquals(NivelVencimiento.VIGENTE, ReporteService.clasificar(hoy.plusDays(60), hoy, 30));
        assertEquals(NivelVencimiento.SIN_FECHA, ReporteService.clasificar(null, hoy, 30));
    }

    @Test
    void listaVencimientosOrdenandoVencidosPrimero() {
        when(productoRepository.findByActivoTrue()).thenReturn(List.of(
                Producto.builder().id(1L).codigo("CER-001").nombre("Cerveza").stockActual(8)
                        .fechaVencimiento(LocalDate.of(2026, 8, 10)).unidadMinima("BOTELLA").activo(true).build(),
                Producto.builder().id(2L).codigo("RON-001").nombre("Ron").stockActual(18)
                        .fechaVencimiento(LocalDate.of(2026, 9, 5)).unidadMinima("BOTELLA").activo(true).build(),
                Producto.builder().id(3L).codigo("VIN-001").nombre("Vino").stockActual(4)
                        .unidadMinima("BOTELLA").activo(true).build()
        ));

        var lista = reporteService.vencimientos(30);

        assertEquals(3, lista.size());
        assertEquals("CER-001", lista.get(0).getCodigo());
        assertEquals(NivelVencimiento.VENCIDO, lista.get(0).getEstado());
        assertEquals(-13, lista.get(0).getDiasRestantes());
        assertEquals("RON-001", lista.get(1).getCodigo());
        assertEquals(NivelVencimiento.POR_VENCER, lista.get(1).getEstado());
        assertEquals("VIN-001", lista.get(2).getCodigo());
        assertEquals(NivelVencimiento.SIN_FECHA, lista.get(2).getEstado());
    }

    @Test
    void armaReporteDePeriodoConProductosVendidos() {
        when(ventaRepository.findByFechaBetween(any(), any())).thenReturn(List.of(
                Venta.builder()
                        .id(1L)
                        .usuarioId(2L)
                        .numero("V-1")
                        .fecha(LocalDateTime.of(2026, 8, 23, 18, 29))
                        .estado(EstadoVenta.COMPLETADA)
                        .formaPago(FormaPago.EFECTIVO)
                        .subtotal(new BigDecimal("250.00"))
                        .impuesto(new BigDecimal("37.50"))
                        .total(new BigDecimal("287.50"))
                        .detalles(List.of(DetalleVenta.builder()
                                .productoId(1L)
                                .cantidadUmm(1)
                                .subtotal(new BigDecimal("250.00"))
                                .build()))
                        .build()
        ));
        when(facturaRepository.findByFechaEmisionBetween(any(), any())).thenReturn(List.of(
                Factura.builder().id(10L).ventaId(1L).numero("F-1").estado(EstadoFactura.EMITIDA)
                        .clienteNombre("Consumidor final").total(new BigDecimal("287.50")).build()
        ));
        when(compraRepository.findByFechaBetween(any(), any())).thenReturn(List.of(
                Compra.builder().id(5L).numero("C-1").proveedorNombre("Norte").total(new BigDecimal("1080.00")).build()
        ));
        when(productoRepository.findAllById(any())).thenReturn(List.of(
                Producto.builder().id(1L).codigo("RON-001").nombre("Ron Añejo 750ml").build()
        ));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(
                Usuario.builder().id(2L).nombreCompleto("Cajero Demo").build()
        ));
        when(cajaService.turnosEnPeriodo(any(), any())).thenReturn(List.of(
                TurnoCajaDTO.builder().id(7L).usuarioNombre("Cajero Demo").totalVentas(new BigDecimal("287.50")).build()
        ));

        var reporte = reporteService.periodo(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 23));

        assertEquals(1, reporte.getVentasCantidad());
        assertEquals(new BigDecimal("287.50"), reporte.getVentasTotal());
        assertEquals(new BigDecimal("287.50"), reporte.getVentasEfectivoTotal());
        assertEquals(new BigDecimal("1080.00"), reporte.getComprasTotal());
        assertEquals(new BigDecimal("-792.50"), reporte.getResultado());
        assertEquals(1, reporte.getProductos().size());
        assertEquals("RON-001", reporte.getProductos().get(0).getCodigo());
        assertEquals(1, reporte.getTurnosCantidad());
        assertEquals("Cajero Demo", reporte.getVentas().get(0).getCajeroNombre());
    }

    @Test
    void rechazaRangoInvalido() {
        assertThrows(ReglaNegocioException.class, () ->
                reporteService.periodo(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 23)));
    }

    @Test
    void filtraVentasPorFormaPago() {
        when(ventaRepository.findByFechaBetween(any(), any())).thenReturn(List.of(
                Venta.builder().id(1L).numero("V-1").estado(EstadoVenta.COMPLETADA)
                        .formaPago(FormaPago.EFECTIVO).total(new BigDecimal("100.00")).build(),
                Venta.builder().id(2L).numero("V-2").estado(EstadoVenta.COMPLETADA)
                        .formaPago(FormaPago.TARJETA).total(new BigDecimal("200.00")).build()
        ));
        when(facturaRepository.findByFechaEmisionBetween(any(), any())).thenReturn(List.of());
        when(compraRepository.findByFechaBetween(any(), any())).thenReturn(List.of());
        when(cajaService.turnosEnPeriodo(any(), any())).thenReturn(List.of());

        var reporte = reporteService.periodo(
                LocalDate.of(2026, 8, 23),
                LocalDate.of(2026, 8, 23),
                null,
                "TARJETA",
                null
        );

        assertEquals(1, reporte.getVentasCantidad());
        assertEquals("V-2", reporte.getVentas().get(0).getNumero());
        assertEquals(new BigDecimal("200.00"), reporte.getVentasTarjetaTotal());
    }
}
