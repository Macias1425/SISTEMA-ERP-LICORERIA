package com.licoreria.pos.service;

import com.licoreria.pos.dto.AutorizacionDTO;
import com.licoreria.pos.dto.MermaRequestDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.exception.StockInsuficienteException;
import com.licoreria.pos.exception.StockInsuficienteException;
import com.licoreria.pos.model.EstadoMerma;
import com.licoreria.pos.model.Merma;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TipoMerma;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.MermaRepository;
import com.licoreria.pos.repository.PresentacionRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MermaServiceTest {

    @Mock
    private MermaRepository mermaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private PresentacionRepository presentacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private AutorizacionService autorizacionService;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private PermisoService permisoService;

    private MermaService mermaService;

    @BeforeEach
    void setUp() {
        mermaService = new MermaService(
                mermaRepository,
                productoRepository,
                presentacionRepository,
                usuarioRepository,
                inventarioService,
                new ConversionUnidades(),
                autorizacionService,
                auditoriaService,
                permisoService
        );
    }

    @Test
    void solicitarExigeStockDisponible() {
        Usuario almacenista = Usuario.builder().id(3L).rol(Rol.ALMACENISTA).activo(true).build();
        Producto producto = Producto.builder().id(1L).nombre("Ron").activo(true).stockActual(5).build();
        Presentacion botella = Presentacion.builder().id(4L).producto(producto).factorAUnidadMinima(1).build();

        when(autorizacionService.operadorActual()).thenReturn(almacenista);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(inventarioService.obtenerPresentacion(1L, 4L)).thenReturn(botella);
        org.mockito.Mockito.doThrow(new StockInsuficienteException("Ron", 5, 10))
                .when(inventarioService).exigirStockDisponible(1L, 10);

        MermaRequestDTO dto = MermaRequestDTO.builder()
                .productoId(1L)
                .presentacionId(4L)
                .cantidad(10)
                .tipo(TipoMerma.ROTURA)
                .motivo("Faltante detectado en conteo")
                .build();

        assertThrows(StockInsuficienteException.class, () -> mermaService.solicitar(dto));
        verify(mermaRepository, never()).save(any(Merma.class));
    }

    @Test
    void aprobarDescuentaStock() {
        Usuario admin = Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).username("admin").build();
        Merma merma = Merma.builder()
                .id(7L)
                .productoId(1L)
                .presentacionId(4L)
                .cantidadPresentacion(2)
                .cantidadUmm(2)
                .tipo(TipoMerma.DANADA)
                .motivo("Producto vencido en anaquel")
                .estado(EstadoMerma.PENDIENTE)
                .solicitadoPor(3L)
                .build();

        when(mermaRepository.findById(7L)).thenReturn(Optional.of(merma));
        when(autorizacionService.operadorActual()).thenReturn(admin);
        when(autorizacionService.exigirCredencialAdmin(any(AutorizacionDTO.class), any()))
                .thenReturn(admin);
        when(mermaRepository.save(any(Merma.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(Producto.builder().id(1L).nombre("Ron").build()));

        var respuesta = mermaService.aprobar(7L, AutorizacionDTO.builder()
                .username("admin")
                .password("admin123")
                .build());

        assertEquals(EstadoMerma.APROBADA, respuesta.getEstado());
        verify(inventarioService).exigirStockDisponible(1L, 2);
        verify(inventarioService).descontar(1L, 4L, 2, TipoMovimiento.MERMA, merma.getMotivo(), 1L, null, null, 7L);
    }

    @Test
    void adminVeMermaPendienteComoResoluble() {
        Usuario admin = Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build();
        Merma merma = Merma.builder()
                .id(8L)
                .productoId(1L)
                .presentacionId(4L)
                .cantidadPresentacion(1)
                .cantidadUmm(1)
                .tipo(TipoMerma.MUESTRA)
                .motivo("Botella rota en traslado")
                .estado(EstadoMerma.PENDIENTE)
                .solicitadoPor(3L)
                .build();

        when(autorizacionService.operadorActual()).thenReturn(admin);
        when(mermaRepository.findById(8L)).thenReturn(Optional.of(merma));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(Producto.builder().id(1L).nombre("Ron").build()));

        var respuesta = mermaService.obtener(8L);

        assertTrue(respuesta.getAprobable());
        assertTrue(respuesta.getRechazable());
    }

    @Test
    void almacenistaNoPuedeResolverMerma() {
        Usuario almacenista = Usuario.builder().id(3L).rol(Rol.ALMACENISTA).activo(true).build();
        Merma merma = Merma.builder()
                .id(9L)
                .productoId(1L)
                .presentacionId(4L)
                .cantidadPresentacion(1)
                .cantidadUmm(1)
                .tipo(TipoMerma.ROTURA)
                .motivo("Ajuste por inventario físico")
                .estado(EstadoMerma.PENDIENTE)
                .solicitadoPor(3L)
                .build();

        when(autorizacionService.operadorActual()).thenReturn(almacenista);
        when(mermaRepository.findById(9L)).thenReturn(Optional.of(merma));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(Producto.builder().id(1L).nombre("Ron").build()));

        var respuesta = mermaService.obtener(9L);

        assertFalse(respuesta.getAprobable());
        assertFalse(respuesta.getRechazable());
        assertEquals("Solo un administrador puede aprobar o rechazar mermas", respuesta.getMotivoNoResolucion());
    }

    @Test
    void rechazarNoDescuentaStock() {
        Usuario admin = Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build();
        Merma merma = Merma.builder()
                .id(10L)
                .productoId(1L)
                .presentacionId(4L)
                .cantidadPresentacion(1)
                .cantidadUmm(1)
                .tipo(TipoMerma.ROTURA)
                .motivo("Sospecha de faltante no confirmada")
                .estado(EstadoMerma.PENDIENTE)
                .solicitadoPor(3L)
                .build();

        when(mermaRepository.findById(10L)).thenReturn(Optional.of(merma));
        when(autorizacionService.operadorActual()).thenReturn(admin);
        when(autorizacionService.exigirCredencialAdmin(any(AutorizacionDTO.class), any()))
                .thenReturn(admin);
        when(mermaRepository.save(any(Merma.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(Producto.builder().id(1L).nombre("Ron").build()));

        var respuesta = mermaService.rechazar(10L, AutorizacionDTO.builder()
                .username("admin")
                .password("admin123")
                .build());

        assertEquals(EstadoMerma.RECHAZADA, respuesta.getEstado());
        verify(inventarioService, never()).descontar(anyLong(), anyLong(), anyInt(), any(), any(), anyLong(), any(), any(), any());
    }
}
