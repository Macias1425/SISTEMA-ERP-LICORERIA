package com.licoreria.pos.service;

import com.licoreria.pos.dto.AlertaStockDTO;
import com.licoreria.pos.dto.MovimientoInventarioDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.exception.StockInsuficienteException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.MovimientoInventario;
import com.licoreria.pos.model.NivelAlerta;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.MovimientoInventarioRepository;
import com.licoreria.pos.repository.PresentacionRepository;
import com.licoreria.pos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final PresentacionRepository presentacionRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ConversionUnidades conversionUnidades;
    private final AuditoriaService auditoriaService;
    private final AutorizacionService autorizacionService;

    @Transactional(readOnly = true)
    public List<MovimientoInventario> listarMovimientos(Long productoId) {
        asegurarProducto(productoId);
        return movimientoInventarioRepository.findByProductoIdOrderByFechaDesc(productoId);
    }

    @Transactional(readOnly = true)
    public List<AlertaStockDTO> listarAlertas() {
        return productoRepository.findByActivoTrue().stream()
                .map(this::toAlerta)
                .filter(alerta -> alerta.getNivelAlerta() != NivelAlerta.OK)
                .toList();
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
    public MovimientoInventario registrarMovimiento(MovimientoInventarioDTO dto) {
        Usuario operador = autorizacionService.exigirRol(Rol.ALMACENISTA, Rol.ADMIN);
        if (dto.getTipo() == TipoMovimiento.SALIDA
                || dto.getTipo() == TipoMovimiento.MERMA
                || dto.getTipo() == TipoMovimiento.VENTA
                || dto.getTipo() == TipoMovimiento.ANULACION) {
            throw new ReglaNegocioException(
                    "USAR_MODULO_CORRECTO",
                    "Las salidas por venta van por /api/ventas y las que no son venta por /api/inventario/mermas"
            );
        }

        Presentacion presentacion = obtenerPresentacion(dto.getProductoId(), dto.getPresentacionId());
        int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, dto.getCantidad());
        boolean incrementa = dto.getTipo() == TipoMovimiento.ENTRADA
                || Boolean.TRUE.equals(dto.getIncremento());

        if (dto.getTipo() == TipoMovimiento.AJUSTE && (dto.getMotivo() == null || dto.getMotivo().isBlank())) {
            throw new ReglaNegocioException("MOTIVO_OBLIGATORIO", "El ajuste de inventario requiere justificación");
        }

        MovimientoInventario movimiento = aplicarMovimiento(
                dto.getProductoId(),
                presentacion,
                dto.getCantidad(),
                cantidadUmm,
                dto.getTipo(),
                incrementa,
                dto.getMotivo(),
                operador.getId()
        );
        AccionAuditoria accion = dto.getTipo() == TipoMovimiento.ENTRADA
                ? AccionAuditoria.ENTRADA_STOCK
                : AccionAuditoria.AJUSTE_STOCK;
        auditoriaService.registrar(operador, accion, "Producto", dto.getProductoId(),
                null, "umm=" + movimiento.getCantidadUmm() + ", stock=" + movimiento.getStockResultante(),
                dto.getMotivo());
        return movimiento;
    }

    /** Punto único de salida de stock (ventas y mermas aprobadas). RN-INV-02. */
    @Transactional
    public MovimientoInventario descontar(Long productoId, Long presentacionId, int cantidadPresentacion,
                                          TipoMovimiento tipo, String motivo, Long usuarioId) {
        Presentacion presentacion = obtenerPresentacion(productoId, presentacionId);
        int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, cantidadPresentacion);
        return aplicarMovimiento(productoId, presentacion, cantidadPresentacion, cantidadUmm, tipo, false, motivo, usuarioId);
    }

    /** Devolución de stock por anulación de factura. */
    @Transactional
    public MovimientoInventario ingresar(Long productoId, Long presentacionId, int cantidadPresentacion,
                                         TipoMovimiento tipo, String motivo, Long usuarioId) {
        Presentacion presentacion = obtenerPresentacion(productoId, presentacionId);
        int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, cantidadPresentacion);
        return aplicarMovimiento(productoId, presentacion, cantidadPresentacion, cantidadUmm, tipo, true, motivo, usuarioId);
    }

    private MovimientoInventario aplicarMovimiento(Long productoId, Presentacion presentacion,
                                                   int cantidadPresentacion, int cantidadUmm,
                                                   TipoMovimiento tipo, boolean incrementa,
                                                   String motivo, Long usuarioId) {
        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + productoId));

        int stockActual = producto.getStockActual() == null ? 0 : producto.getStockActual();
        int nuevoStock = incrementa ? stockActual + cantidadUmm : stockActual - cantidadUmm;

        if (nuevoStock < 0) {
            throw new StockInsuficienteException(producto.getNombre(), stockActual, cantidadUmm);
        }

        producto.setStockActual(nuevoStock);
        productoRepository.save(producto);

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
                .build();

        return movimientoInventarioRepository.save(movimiento);
    }

    public Presentacion obtenerPresentacion(Long productoId, Long presentacionId) {
        return presentacionRepository.findByIdAndProductoId(presentacionId, productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Presentación " + presentacionId + " no pertenece al producto " + productoId));
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
