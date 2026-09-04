package com.licoreria.pos.service;

import com.licoreria.pos.dto.CompraRequestDTO;
import com.licoreria.pos.dto.DetalleCompraDTO;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Compra;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.PoliticaPrecio;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Proveedor;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.CompraRepository;
import com.licoreria.pos.repository.PrecioProveedorRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.UsuarioRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock
    private CompraRepository compraRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private PrecioProveedorRepository precioProveedorRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private ProveedorService proveedorService;
    @Mock
    private AutorizacionService autorizacionService;
    @Mock
    private AccesoService accesoService;
    @Mock
    private AuditoriaService auditoriaService;

    private PoliticaPrecioService politicaPrecioService;

    private CompraService compraService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 8, 23, 19, 0).atZone(ZoneId.of("America/Managua")).toInstant(),
                ZoneId.of("America/Managua")
        );
        politicaPrecioService = new PoliticaPrecioService(auditoriaService);
        compraService = new CompraService(
                compraRepository,
                productoRepository,
                precioProveedorRepository,
                usuarioRepository,
                inventarioService,
                proveedorService,
                new ConversionUnidades(),
                autorizacionService,
                accesoService,
                auditoriaService,
                politicaPrecioService,
                clock
        );
    }

    @Test
    void recibirCompraIngresaStockYActualizaCosto() {
        Producto producto = Producto.builder()
                .id(1L)
                .nombre("Ron Añejo")
                .precioCompra(new BigDecimal("180.00"))
                .activo(true)
                .build();
        Presentacion botella = Presentacion.builder()
                .id(4L)
                .producto(producto)
                .nombre("Botella")
                .factorAUnidadMinima(1)
                .activo(true)
                .build();
        Usuario almacenista = Usuario.builder().id(3L).rol(Rol.ALMACENISTA).activo(true).build();
        Proveedor proveedor = Proveedor.builder().id(5L).nombre("Distribuidora del Norte").activo(true).build();

        when(accesoService.exigirPermiso(Permiso.COMPRAS_GESTIONAR)).thenReturn(almacenista);
        when(proveedorService.exigirActivo(5L)).thenReturn(proveedor);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(inventarioService.obtenerPresentacion(1L, 4L)).thenReturn(botella);
        when(precioProveedorRepository.findByProveedorIdAndProductoIdAndPresentacionId(5L, 1L, 4L))
                .thenReturn(Optional.empty());
        when(precioProveedorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> {
            Compra compra = invocation.getArgument(0);
            compra.setId(22L);
            return compra;
        });

        CompraRequestDTO request = CompraRequestDTO.builder()
                .proveedorId(5L)
                .documentoProveedor("FAC-900")
                .actualizarCostos(true)
                .detalles(List.of(DetalleCompraDTO.builder()
                        .productoId(1L)
                        .presentacionId(4L)
                        .cantidad(6)
                        .costoUnitario(new BigDecimal("175.00"))
                        .build()))
                .build();

        var recibida = compraService.recibir(request);

        assertEquals("Distribuidora del Norte", recibida.getProveedorNombre());
        assertEquals(5L, recibida.getProveedorId());
        assertEquals(new BigDecimal("1050.00"), recibida.getTotal());
        assertEquals(new BigDecimal("175.00"), producto.getPrecioCompra());
        verify(inventarioService).ingresar(eq(1L), eq(4L), eq(6), eq(TipoMovimiento.COMPRA),
                anyString(), eq(3L), eq(22L), eq(null), eq(null), any(EntradaLote.class));
        verify(auditoriaService).registrar(eq(almacenista), eq(AccionAuditoria.COMPRA), eq("Compra"),
                eq(22L), any(), anyString(), anyString());
    }

    @Test
    void recibirCompraCalculaCostoPonderado() {
        Producto producto = Producto.builder()
                .id(1L)
                .nombre("Ron Añejo")
                .precioCompra(new BigDecimal("180.00"))
                .stockActual(100)
                .activo(true)
                .build();
        Presentacion botella = Presentacion.builder()
                .id(4L)
                .producto(producto)
                .nombre("Botella")
                .factorAUnidadMinima(1)
                .activo(true)
                .build();
        Usuario almacenista = Usuario.builder().id(3L).rol(Rol.ALMACENISTA).activo(true).build();
        Proveedor proveedor = Proveedor.builder().id(5L).nombre("Distribuidora del Norte").activo(true).build();

        when(accesoService.exigirPermiso(Permiso.COMPRAS_GESTIONAR)).thenReturn(almacenista);
        when(proveedorService.exigirActivo(5L)).thenReturn(proveedor);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(inventarioService.obtenerPresentacion(1L, 4L)).thenReturn(botella);
        when(precioProveedorRepository.findByProveedorIdAndProductoIdAndPresentacionId(5L, 1L, 4L))
                .thenReturn(Optional.empty());
        when(precioProveedorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            int cantidad = invocation.getArgument(2);
            producto.setStockActual(producto.getStockActual() + cantidad);
            return null;
        }).when(inventarioService).ingresar(anyLong(), anyLong(), anyInt(), any(), anyString(), anyLong(),
                any(), any(), any(), any());
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> {
            Compra compra = invocation.getArgument(0);
            compra.setId(23L);
            return compra;
        });

        CompraRequestDTO request = CompraRequestDTO.builder()
                .proveedorId(5L)
                .documentoProveedor("FAC-901")
                .actualizarCostos(true)
                .detalles(List.of(DetalleCompraDTO.builder()
                        .productoId(1L)
                        .presentacionId(4L)
                        .cantidad(50)
                        .costoUnitario(new BigDecimal("200.00"))
                        .build()))
                .build();

        compraService.recibir(request);

        assertEquals(new BigDecimal("186.67"), producto.getPrecioCompra());
    }

    @Test
    void recibirCompraAplicaPoliticaMarkupAutomatico() {
        Producto producto = Producto.builder()
                .id(6L)
                .nombre("Agua purificada")
                .precioCompra(new BigDecimal("8.00"))
                .precioVenta(new BigDecimal("10.00"))
                .stockActual(60)
                .politicaPrecio(PoliticaPrecio.AUTOMATICO_MARKUP)
                .margenObjetivoPct(new BigDecimal("20"))
                .activo(true)
                .build();
        Presentacion caja = Presentacion.builder()
                .id(9L)
                .producto(producto)
                .nombre("CAJA")
                .factorAUnidadMinima(12)
                .activo(true)
                .build();
        Usuario almacenista = Usuario.builder().id(3L).rol(Rol.ALMACENISTA).activo(true).build();
        Proveedor proveedor = Proveedor.builder().id(5L).nombre("Distribuidora del Norte").activo(true).build();

        when(accesoService.exigirPermiso(Permiso.COMPRAS_GESTIONAR)).thenReturn(almacenista);
        when(proveedorService.exigirActivo(5L)).thenReturn(proveedor);
        when(productoRepository.findById(6L)).thenReturn(Optional.of(producto));
        when(inventarioService.obtenerPresentacion(6L, 9L)).thenReturn(caja);
        when(precioProveedorRepository.findByProveedorIdAndProductoIdAndPresentacionId(5L, 6L, 9L))
                .thenReturn(Optional.empty());
        doAnswer(invocation -> {
            int cantidad = invocation.getArgument(2);
            producto.setStockActual(producto.getStockActual() + cantidad);
            return null;
        }).when(inventarioService).ingresar(anyLong(), anyLong(), anyInt(), any(), anyString(), anyLong(),
                any(), any(), any(), any());
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(precioProveedorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> {
            Compra compra = invocation.getArgument(0);
            compra.setId(24L);
            return compra;
        });

        CompraRequestDTO request = CompraRequestDTO.builder()
                .proveedorId(5L)
                .documentoProveedor("FAC-902")
                .actualizarCostos(true)
                .detalles(List.of(DetalleCompraDTO.builder()
                        .productoId(6L)
                        .presentacionId(9L)
                        .cantidad(10)
                        .costoUnitario(new BigDecimal("120.00"))
                        .build()))
                .build();

        compraService.recibir(request);

        assertEquals(new BigDecimal("10.00"), producto.getPrecioCompra());
        assertEquals(new BigDecimal("12.50"), producto.getPrecioVenta());
        verify(auditoriaService).registrar(eq(almacenista), eq(AccionAuditoria.CAMBIO_PRECIO), eq("Producto"),
                eq(6L), any(), any(), contains("Markup automático por compra"));
        verify(precioProveedorRepository).save(org.mockito.ArgumentMatchers.argThat(precio ->
                precio.getPrecioUnitario().compareTo(new BigDecimal("120.00")) == 0));
    }
}
