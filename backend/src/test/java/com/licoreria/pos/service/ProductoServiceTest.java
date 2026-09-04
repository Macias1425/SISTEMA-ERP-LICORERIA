package com.licoreria.pos.service;

import com.licoreria.pos.dto.ProductoDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.ProductoRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private AutorizacionService autorizacionService;
    @Mock
    private AccesoService accesoService;
    @Mock
    private CategoriaService categoriaService;

    private ProductoService productoService;
    private PoliticaPrecioService politicaPrecioService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 8, 23, 20, 0).atZone(ZoneId.of("America/Managua")).toInstant(),
                ZoneId.of("America/Managua")
        );
        politicaPrecioService = new PoliticaPrecioService(auditoriaService);
        productoService = new ProductoService(
                productoRepository,
                inventarioService,
                auditoriaService,
                autorizacionService,
                accesoService,
                categoriaService,
                politicaPrecioService,
                clock
        );
    }

    @Test
    void rechazaPrecioVentaMenorQueCompra() {
        Usuario admin = Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build();
        when(accesoService.exigirPermiso(Permiso.PRODUCTOS_GESTIONAR)).thenReturn(admin);
        when(productoRepository.existsByCodigoIgnoreCase("RON-001")).thenReturn(false);

        ProductoDTO dto = ProductoDTO.builder()
                .codigo("RON-001")
                .nombre("Ron Añejo")
                .precioCompra(new BigDecimal("200.00"))
                .precioVenta(new BigDecimal("150.00"))
                .build();

        assertThrows(ReglaNegocioException.class, () -> productoService.crear(dto));
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    void rechazaDesactivarProductoConStock() {
        Usuario admin = Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build();
        Producto existente = Producto.builder()
                .id(3L)
                .codigo("VOD-001")
                .nombre("Vodka")
                .precioCompra(new BigDecimal("100"))
                .precioVenta(new BigDecimal("150"))
                .stockActual(12)
                .activo(true)
                .build();

        when(accesoService.exigirPermiso(Permiso.PRODUCTOS_GESTIONAR)).thenReturn(admin);
        when(productoRepository.findById(3L)).thenReturn(Optional.of(existente));
        when(productoRepository.existsByCodigoIgnoreCaseAndIdNot("VOD-001", 3L)).thenReturn(false);

        ProductoDTO dto = ProductoDTO.builder()
                .codigo("VOD-001")
                .nombre("Vodka")
                .precioCompra(new BigDecimal("100"))
                .precioVenta(new BigDecimal("150"))
                .activo(false)
                .build();

        assertThrows(ReglaNegocioException.class, () -> productoService.actualizar(3L, dto));
    }

    @Test
    void marcaProductoSinStockComoEliminable() {
        Producto producto = Producto.builder()
                .id(4L)
                .codigo("GIN-001")
                .nombre("Ginebra")
                .stockActual(0)
                .precioCompra(BigDecimal.TEN)
                .precioVenta(BigDecimal.valueOf(20))
                .build();
        when(productoRepository.findById(4L)).thenReturn(Optional.of(producto));
        when(inventarioService.calcularNivelAlerta(producto)).thenReturn(com.licoreria.pos.model.NivelAlerta.CRITICO);

        ProductoDTO dto = productoService.obtenerPorId(4L);

        assertEquals(true, dto.getEliminable());
    }
}
