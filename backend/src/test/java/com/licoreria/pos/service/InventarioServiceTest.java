package com.licoreria.pos.service;

import com.licoreria.pos.dto.MovimientoInventarioDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.exception.StockInsuficienteException;
import com.licoreria.pos.model.MovimientoInventario;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.repository.MovimientoInventarioRepository;
import com.licoreria.pos.repository.PresentacionRepository;
import com.licoreria.pos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private PresentacionRepository presentacionRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;

    private InventarioService inventarioService;

    @BeforeEach
    void setUp() {
        inventarioService = new InventarioService(
                productoRepository,
                presentacionRepository,
                movimientoInventarioRepository,
                new ConversionUnidades(),
                org.mockito.Mockito.mock(AuditoriaService.class),
                org.mockito.Mockito.mock(AutorizacionService.class)
        );
    }

    @Test
    void descuentaCajaEnUnidadMinima() {
        Producto producto = productoConStock(24);
        Presentacion caja = cajaDe(producto, 12);

        when(presentacionRepository.findByIdAndProductoId(2L, 1L)).thenReturn(Optional.of(caja));
        when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventario movimiento = inventarioService.descontar(
                1L, 2L, 1, TipoMovimiento.SALIDA, "Venta", 10L);

        assertEquals(12, movimiento.getCantidadUmm());
        assertEquals(12, producto.getStockActual());
        verify(productoRepository).save(producto);
    }

    @Test
    void rechazaStockNegativo() {
        Producto producto = productoConStock(10);
        Presentacion caja = cajaDe(producto, 12);

        when(presentacionRepository.findByIdAndProductoId(2L, 1L)).thenReturn(Optional.of(caja));
        when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));

        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.descontar(1L, 2L, 1, TipoMovimiento.SALIDA, "Venta", 10L));
        assertEquals(10, producto.getStockActual());
    }

    @Test
    void salidaDirectaDebeIrPorMermas() {
        MovimientoInventarioDTO dto = MovimientoInventarioDTO.builder()
                .productoId(1L)
                .presentacionId(2L)
                .tipo(TipoMovimiento.SALIDA)
                .cantidad(1)
                .build();

        assertThrows(ReglaNegocioException.class, () -> inventarioService.registrarMovimiento(dto));
    }

    private Producto productoConStock(int stock) {
        return Producto.builder()
                .id(1L)
                .codigo("RON-001")
                .nombre("Ron Añejo")
                .precioCompra(BigDecimal.TEN)
                .precioVenta(BigDecimal.valueOf(20))
                .stockActual(stock)
                .stockMinimo(6)
                .stockCritico(2)
                .build();
    }

    private Presentacion cajaDe(Producto producto, int factor) {
        return Presentacion.builder()
                .id(2L)
                .producto(producto)
                .nombre("Caja")
                .factorAUnidadMinima(factor)
                .activo(true)
                .build();
    }
}
