package com.licoreria.pos.service;

import com.licoreria.pos.dto.AlertaStockDTO;
import com.licoreria.pos.dto.MovimientoInventarioDTO;
import com.licoreria.pos.dto.MovimientoInventarioResponseDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.exception.StockInsuficienteException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.MovimientoInventario;
import com.licoreria.pos.model.NivelAlerta;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.MovimientoInventarioRepository;
import com.licoreria.pos.repository.PresentacionRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final PresentacionRepository presentacionRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConversionUnidades conversionUnidades;
    private final AuditoriaService auditoriaService;
    private final AutorizacionService autorizacionService;
    private final PermisoService permisoService;
    private final LoteService loteService;

    @Transactional(readOnly = true)
    public PaginaDTO<MovimientoInventarioResponseDTO> listarMovimientos(
            Long productoId,
            TipoMovimiento tipo,
            LocalDate desde,
            LocalDate hasta,
            int pagina,
            int tamano
    ) {
        permisoService.exigirPermiso(autorizacionService.operadorActual(), Permiso.INVENTARIO_VER);
        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime fin = hasta == null ? null : hasta.atTime(LocalTime.MAX);
        Page<MovimientoInventario> page = movimientoInventarioRepository.buscarPaginado(
                productoId, tipo, inicio, fin, PaginacionUtil.pageable(pagina, tamano)
        );
        return PaginaDTO.de(page.map(this::toMovimientoDto));
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponseDTO> listarMovimientos(
            Long productoId,
            TipoMovimiento tipo,
            LocalDate desde,
            LocalDate hasta
    ) {
        return listarMovimientos(productoId, tipo, desde, hasta, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<AlertaStockDTO> listarAlertas(int pagina, int tamano) {
        permisoService.exigirPermiso(autorizacionService.operadorActual(), Permiso.INVENTARIO_VER);
        List<AlertaStockDTO> alertas = productoRepository.findByActivoTrue().stream()
                .map(this::toAlerta)
                .filter(alerta -> alerta.getNivelAlerta() != NivelAlerta.OK)
                .toList();
        return PaginacionUtil.deLista(alertas, pagina, tamano);
    }

    @Transactional(readOnly = true)
    public List<AlertaStockDTO> listarAlertas() {
        return listarAlertas(0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    public NivelAlerta calcularNivelAlerta(Producto producto) {
        int stock = producto.getStockActual() == null ? 0 : producto.getStockActual();
        int critico = producto.getStockCritico() == null ? 0 : producto.getStockCritico();
        int minimo = producto.getStockMinimo() == null ? 0 : producto.getStockMinimo();

        if (stock <= 0 || (critico > 0 && stock <= critico)) {
            return NivelAlerta.CRITICO;
        }
        if (minimo > 0 && stock <= minimo) {
            return NivelAlerta.MINIMO;
        }
        return NivelAlerta.OK;
    }

    /**
     * Entradas y ajustes. Las salidas que no son venta deben ir por mermas (RN-INV-04).
     * Las ventas llamarán {@link #descontar}.
     */
    @Transactional
    public MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioDTO dto) {
        Usuario operador = autorizacionService.operadorActual();
        permisoService.exigirPermiso(operador, Permiso.INVENTARIO_AJUSTAR);
        if (dto.getTipo() == TipoMovimiento.SALIDA
                || dto.getTipo() == TipoMovimiento.MERMA
                || dto.getTipo() == TipoMovimiento.VENTA
                || dto.getTipo() == TipoMovimiento.ANULACION
                || dto.getTipo() == TipoMovimiento.COMPRA) {
            throw new ReglaNegocioException(
                    "USAR_MODULO_CORRECTO",
                    "Las compras van por /api/compras, las ventas por /api/ventas y las mermas por /api/inventario/mermas"
            );
        }

        Producto producto = asegurarProducto(dto.getProductoId());
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ReglaNegocioException("PRODUCTO_INACTIVO", "El producto no está activo: " + producto.getNombre());
        }

        Presentacion presentacion = obtenerPresentacion(dto.getProductoId(), dto.getPresentacionId());
        int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, dto.getCantidad());
        boolean incrementa = dto.getTipo() == TipoMovimiento.ENTRADA
                || Boolean.TRUE.equals(dto.getIncremento());

        if (dto.getTipo() == TipoMovimiento.AJUSTE && (dto.getMotivo() == null || dto.getMotivo().isBlank())) {
            throw new ReglaNegocioException("MOTIVO_OBLIGATORIO", "El ajuste de inventario requiere justificación");
        }
        if (dto.getTipo() == TipoMovimiento.AJUSTE && !incrementa) {
            exigirStockDisponible(dto.getProductoId(), cantidadUmm);
        }

        MovimientoInventario movimiento = aplicarMovimiento(
                dto.getProductoId(),
                presentacion,
                dto.getCantidad(),
                cantidadUmm,
                dto.getTipo(),
                incrementa,
                dto.getMotivo(),
                operador.getId(),
                null,
                null,
                null
        );
        AccionAuditoria accion = dto.getTipo() == TipoMovimiento.ENTRADA
                ? AccionAuditoria.ENTRADA_STOCK
                : AccionAuditoria.AJUSTE_STOCK;
        auditoriaService.registrar(operador, accion, "Producto", dto.getProductoId(),
                null, "umm=" + movimiento.getCantidadUmm() + ", stock=" + movimiento.getStockResultante(),
                dto.getMotivo());
        return toMovimientoDto(movimiento);
    }

    /**
     * Bloquea el producto y exige existencia suficiente en UMM antes de grabar la venta.
     * RN-POS-06: la demanda de todas las líneas del mismo SKU se suma.
     */
    @Transactional
    public void exigirStockDisponible(Long productoId, int cantidadUmm) {
        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + productoId));
        int stockActual = producto.getStockActual() == null ? 0 : producto.getStockActual();
        if (stockActual < cantidadUmm) {
            throw new StockInsuficienteException(producto.getNombre(), stockActual, cantidadUmm);
        }
    }

    /** Punto único de salida de stock (ventas y mermas aprobadas). RN-INV-02. */
    @Transactional
    public MovimientoInventario descontar(Long productoId, Long presentacionId, int cantidadPresentacion,
                                          TipoMovimiento tipo, String motivo, Long usuarioId) {
        return descontar(productoId, presentacionId, cantidadPresentacion, tipo, motivo, usuarioId, null, null, null);
    }

    @Transactional
    public MovimientoInventario descontar(Long productoId, Long presentacionId, int cantidadPresentacion,
                                          TipoMovimiento tipo, String motivo, Long usuarioId,
                                          Long compraId, Long ventaId, Long mermaId) {
        Presentacion presentacion = obtenerPresentacion(productoId, presentacionId);
        int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, cantidadPresentacion);
        return aplicarMovimiento(productoId, presentacion, cantidadPresentacion, cantidadUmm, tipo, false, motivo,
                usuarioId, compraId, ventaId, mermaId);
    }

    /** Devolución de stock por anulación de factura o ingreso por compra. */
    @Transactional
    public MovimientoInventario ingresar(Long productoId, Long presentacionId, int cantidadPresentacion,
                                         TipoMovimiento tipo, String motivo, Long usuarioId) {
        return ingresar(productoId, presentacionId, cantidadPresentacion, tipo, motivo, usuarioId, null, null, null);
    }

    @Transactional
    public MovimientoInventario ingresar(Long productoId, Long presentacionId, int cantidadPresentacion,
                                         TipoMovimiento tipo, String motivo, Long usuarioId,
                                         Long compraId, Long ventaId, Long mermaId) {
        return ingresar(productoId, presentacionId, cantidadPresentacion, tipo, motivo, usuarioId,
                compraId, ventaId, mermaId, null);
    }

    /** Entrada con datos de lote conocidos (costo y vencimiento de la línea de compra). */
    @Transactional
    public MovimientoInventario ingresar(Long productoId, Long presentacionId, int cantidadPresentacion,
                                         TipoMovimiento tipo, String motivo, Long usuarioId,
                                         Long compraId, Long ventaId, Long mermaId, EntradaLote datosLote) {
        Presentacion presentacion = obtenerPresentacion(productoId, presentacionId);
        int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, cantidadPresentacion);
        return aplicarMovimiento(productoId, presentacion, cantidadPresentacion, cantidadUmm, tipo, true, motivo,
                usuarioId, compraId, ventaId, mermaId, datosLote);
    }

    private MovimientoInventario aplicarMovimiento(Long productoId, Presentacion presentacion,
                                                   int cantidadPresentacion, int cantidadUmm,
                                                   TipoMovimiento tipo, boolean incrementa,
                                                   String motivo, Long usuarioId,
                                                   Long compraId, Long ventaId, Long mermaId) {
        return aplicarMovimiento(productoId, presentacion, cantidadPresentacion, cantidadUmm, tipo, incrementa,
                motivo, usuarioId, compraId, ventaId, mermaId, null);
    }

    private MovimientoInventario aplicarMovimiento(Long productoId, Presentacion presentacion,
                                                   int cantidadPresentacion, int cantidadUmm,
                                                   TipoMovimiento tipo, boolean incrementa,
                                                   String motivo, Long usuarioId,
                                                   Long compraId, Long ventaId, Long mermaId,
                                                   EntradaLote datosLote) {
        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + productoId));

        int stockActual = producto.getStockActual() == null ? 0 : producto.getStockActual();
        int nuevoStock = incrementa ? stockActual + cantidadUmm : stockActual - cantidadUmm;

        if (nuevoStock < 0) {
            throw new StockInsuficienteException(producto.getNombre(), stockActual, cantidadUmm);
        }

        producto.setStockActual(nuevoStock);
        productoRepository.save(producto);

        String trazaLotes = sincronizarLotes(producto, presentacion, cantidadUmm, tipo, incrementa, datosLote);

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .productoId(productoId)
                .presentacionId(presentacion.getId())
                .tipo(tipo)
                .cantidadPresentacion(cantidadPresentacion)
                .cantidadUmm(cantidadUmm)
                .stockResultante(nuevoStock)
                .motivo(motivo)
                .fecha(LocalDateTime.now())
                .usuarioId(usuarioId)
                .compraId(compraId)
                .ventaId(ventaId)
                .mermaId(mermaId)
                .incremento(incrementa)
                .detalleLotes(trazaLotes)
                .build();

        return movimientoInventarioRepository.save(movimiento);
    }

    /**
     * Mantiene los lotes alineados con el movimiento: las entradas crean lote,
     * las devoluciones por anulación reingresan al lote vivo y las salidas consumen FEFO.
     */
    private String sincronizarLotes(Producto producto, Presentacion presentacion, int cantidadUmm,
                                    TipoMovimiento tipo, boolean incrementa, EntradaLote datosLote) {
        if (!incrementa) {
            return loteService.consumirFefo(producto.getId(), cantidadUmm);
        }
        if (tipo == TipoMovimiento.ANULACION) {
            return loteService.devolver(producto, presentacion.getId(), cantidadUmm);
        }
        return loteService.registrarEntrada(producto, presentacion.getId(), cantidadUmm, datosLote);
    }

    public Presentacion obtenerPresentacion(Long productoId, Long presentacionId) {
        return presentacionRepository.findByIdAndProductoId(presentacionId, productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Presentación " + presentacionId + " no pertenece al producto " + productoId));
    }

    private MovimientoInventarioResponseDTO toMovimientoDto(MovimientoInventario movimiento) {
        Producto producto = productoRepository.findById(movimiento.getProductoId()).orElse(null);
        String presentacionNombre = null;
        if (movimiento.getPresentacionId() != null && movimiento.getProductoId() != null) {
            try {
                presentacionNombre = obtenerPresentacion(movimiento.getProductoId(), movimiento.getPresentacionId())
                        .getNombre();
            } catch (RuntimeException ignored) {
                presentacionNombre = null;
            }
        }
        return MovimientoInventarioResponseDTO.builder()
                .id(movimiento.getId())
                .productoId(movimiento.getProductoId())
                .productoNombre(producto == null ? null : producto.getNombre())
                .presentacionId(movimiento.getPresentacionId())
                .presentacionNombre(presentacionNombre)
                .tipo(movimiento.getTipo())
                .cantidadPresentacion(movimiento.getCantidadPresentacion())
                .cantidadUmm(movimiento.getCantidadUmm())
                .stockResultante(movimiento.getStockResultante())
                .motivo(movimiento.getMotivo())
                .fecha(movimiento.getFecha())
                .usuarioId(movimiento.getUsuarioId())
                .usuarioNombre(nombreUsuario(movimiento.getUsuarioId()))
                .compraId(movimiento.getCompraId())
                .ventaId(movimiento.getVentaId())
                .mermaId(movimiento.getMermaId())
                .direccion(direccionMovimiento(movimiento))
                .detalleLotes(movimiento.getDetalleLotes())
                .build();
    }

    private String direccionMovimiento(MovimientoInventario movimiento) {
        TipoMovimiento tipo = movimiento.getTipo();
        if (tipo == TipoMovimiento.AJUSTE) {
            return Boolean.TRUE.equals(movimiento.getIncremento()) ? "ENTRADA" : "SALIDA";
        }
        return switch (tipo) {
            case ENTRADA, COMPRA, ANULACION -> "ENTRADA";
            case VENTA, MERMA, SALIDA -> "SALIDA";
            case AJUSTE -> "AJUSTE";
        };
    }

    private String nombreUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return usuarioRepository.findById(usuarioId)
                .map(Usuario::getNombreCompleto)
                .orElse(null);
    }

    private Producto asegurarProducto(Long productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + productoId));
    }

    private AlertaStockDTO toAlerta(Producto producto) {
        return AlertaStockDTO.builder()
                .productoId(producto.getId())
                .codigo(producto.getCodigo())
                .nombre(producto.getNombre())
                .stockActual(producto.getStockActual())
                .stockMinimo(producto.getStockMinimo())
                .stockCritico(producto.getStockCritico())
                .unidadMinima(producto.getUnidadMinima())
                .nivelAlerta(calcularNivelAlerta(producto))
                .build();
    }
}
