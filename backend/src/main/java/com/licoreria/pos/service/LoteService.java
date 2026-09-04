package com.licoreria.pos.service;

import com.licoreria.pos.dto.LoteDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.model.LoteInventario;
import com.licoreria.pos.model.NivelVencimiento;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.repository.LoteInventarioRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestiona los lotes de existencia: los crea en cada entrada y los consume en orden FEFO
 * en cada salida. La cantidad global del producto la sigue llevando el inventario;
 * aquí se responde "de qué lote salió" y "qué está por vencer".
 */
@Service
@RequiredArgsConstructor
public class LoteService {

    /** Umbral por defecto para marcar un lote como próximo a vencer. */
    public static final int DIAS_POR_VENCER = 30;

    private static final DateTimeFormatter SELLO_CODIGO = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int ESCALA_COSTO = 4;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final LoteInventarioRepository loteRepository;
    private final ProductoRepository productoRepository;
    private final AccesoService accesoService;
    private final Clock clock;

    // ---------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public PaginaDTO<LoteDTO> listarPorProducto(Long productoId, int pagina, int tamano) {
        accesoService.exigirPermiso(Permiso.INVENTARIO_VER);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + productoId));
        return PaginaDTO.de(loteRepository.historialPaginado(productoId, PaginacionUtil.pageable(pagina, tamano))
                .map(lote -> toDto(lote, producto)));
    }

    @Transactional(readOnly = true)
    public List<LoteDTO> listarPorProducto(Long productoId) {
        return listarPorProducto(productoId, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<LoteDTO> listarPorVencer(Integer dias, int pagina, int tamano) {
        accesoService.exigirPermiso(Permiso.INVENTARIO_VER);
        int ventana = dias == null || dias <= 0 ? DIAS_POR_VENCER : dias;
        LocalDate limite = LocalDate.now(clock).plusDays(ventana);
        return PaginaDTO.de(loteRepository.porVencerPaginado(limite, PaginacionUtil.pageable(pagina, tamano))
                .map(lote -> toDto(lote, productoRepository.findById(lote.getProductoId()).orElse(null))));
    }

    @Transactional(readOnly = true)
    public List<LoteDTO> listarPorVencer(Integer dias) {
        return listarPorVencer(dias, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<LoteDTO> sugerenciaFefo(Long productoId, int pagina, int tamano) {
        return PaginacionUtil.deLista(sugerenciaFefo(productoId), pagina, tamano);
    }

    /** Lotes en orden de salida sugerido, para que el POS sepa qué botella entregar. */
    @Transactional(readOnly = true)
    public List<LoteDTO> sugerenciaFefo(Long productoId) {
        accesoService.exigirPermiso(Permiso.INVENTARIO_VER);
        Producto producto = productoRepository.findById(productoId).orElse(null);
        return loteRepository.fefo(productoId).stream()
                .map(lote -> toDto(lote, producto))
                .toList();
    }

    // ---------------------------------------------------------------- movimientos

    /** Crea el lote de una entrada y devuelve su código para la trazabilidad del kardex. */
    @Transactional
    public String registrarEntrada(Producto producto, Long presentacionId, int cantidadUmm, EntradaLote datos) {
        if (cantidadUmm <= 0) {
            return null;
        }
        EntradaLote efectivos = datos == null ? EntradaLote.sinDatos() : datos;
        LocalDateTime ahora = LocalDateTime.now(clock);
        LoteInventario lote = LoteInventario.builder()
                .codigo(nuevoCodigo(producto, ahora))
                .productoId(producto.getId())
                .presentacionId(presentacionId)
                .compraId(efectivos.compraId())
                .proveedorNombre(efectivos.proveedorNombre())
                .cantidadInicialUmm(cantidadUmm)
                .cantidadDisponibleUmm(cantidadUmm)
                .costoUnitarioUmm(costoEfectivo(efectivos, producto))
                .fechaIngreso(ahora)
                .fechaVencimiento(efectivos.fechaVencimiento() != null
                        ? efectivos.fechaVencimiento()
                        : producto.getFechaVencimiento())
                .build();
        return loteRepository.save(lote).getCodigo();
    }

    /**
     * Descuenta en orden FEFO y devuelve la traza "L-0001x6, L-0002x3".
     * Si los lotes no alcanzan (histórico previo a la trazabilidad) descuenta lo que hay:
     * el saldo autoritativo es el del producto, no el de los lotes.
     */
    @Transactional
    public String consumirFefo(Long productoId, int cantidadUmm) {
        if (cantidadUmm <= 0) {
            return null;
        }
        int pendiente = cantidadUmm;
        List<String> traza = new ArrayList<>();
        for (LoteInventario lote : loteRepository.fefo(productoId)) {
            if (pendiente <= 0) {
                break;
            }
            int tomado = Math.min(pendiente, lote.getCantidadDisponibleUmm());
            lote.setCantidadDisponibleUmm(lote.getCantidadDisponibleUmm() - tomado);
            loteRepository.save(lote);
            traza.add(lote.getCodigo() + "x" + tomado);
            pendiente -= tomado;
        }
        if (pendiente > 0) {
            traza.add("sin-lote x" + pendiente);
        }
        return traza.isEmpty() ? null : String.join(", ", traza);
    }

    /**
     * Devuelve existencia por anulación: reingresa al lote vivo más reciente para no
     * inventar vencimientos. Si no hay lote vivo, crea uno nuevo con los datos del producto.
     */
    @Transactional
    public String devolver(Producto producto, Long presentacionId, int cantidadUmm) {
        if (cantidadUmm <= 0) {
            return null;
        }
        List<LoteInventario> vivos = loteRepository.disponiblesMasRecientes(producto.getId());
        if (vivos.isEmpty()) {
            return registrarEntrada(producto, presentacionId, cantidadUmm, EntradaLote.sinDatos());
        }
        LoteInventario destino = vivos.get(0);
        destino.setCantidadDisponibleUmm(destino.getCantidadDisponibleUmm() + cantidadUmm);
        loteRepository.save(destino);
        return destino.getCodigo() + "+" + cantidadUmm;
    }

    /** Costo promedio ponderado según los lotes vivos; sirve para valorar inventario. */
    @Transactional(readOnly = true)
    public BigDecimal costoPromedioLotes(Long productoId) {
        List<LoteInventario> vivos = loteRepository.fefo(productoId);
        int unidades = vivos.stream().mapToInt(LoteInventario::getCantidadDisponibleUmm).sum();
        if (unidades == 0) {
            return null;
        }
        BigDecimal valor = vivos.stream()
                .filter(lote -> lote.getCostoUnitarioUmm() != null)
                .map(lote -> lote.getCostoUnitarioUmm().multiply(BigDecimal.valueOf(lote.getCantidadDisponibleUmm())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return valor.divide(BigDecimal.valueOf(unidades), ESCALA_COSTO, REDONDEO);
    }

    // ---------------------------------------------------------------- apoyo

    private BigDecimal costoEfectivo(EntradaLote datos, Producto producto) {
        if (datos.costoUnitarioUmm() != null) {
            return datos.costoUnitarioUmm().setScale(ESCALA_COSTO, REDONDEO);
        }
        return producto.getPrecioCompra() == null
                ? null
                : producto.getPrecioCompra().setScale(ESCALA_COSTO, REDONDEO);
    }

    private String nuevoCodigo(Producto producto, LocalDateTime ahora) {
        String base = "L" + ahora.format(SELLO_CODIGO) + "-" + producto.getId();
        String candidato = base;
        int sufijo = 1;
        while (loteRepository.findFirstByCodigo(candidato).isPresent()) {
            candidato = base + "-" + sufijo++;
        }
        return candidato;
    }

    private LoteDTO toDto(LoteInventario lote, Producto producto) {
        LocalDate hoy = LocalDate.now(clock);
        Integer dias = lote.getFechaVencimiento() == null
                ? null
                : (int) ChronoUnit.DAYS.between(hoy, lote.getFechaVencimiento());
        return LoteDTO.builder()
                .id(lote.getId())
                .codigo(lote.getCodigo())
                .productoId(lote.getProductoId())
                .productoNombre(producto == null ? null : producto.getNombre())
                .productoCodigo(producto == null ? null : producto.getCodigo())
                .compraId(lote.getCompraId())
                .proveedorNombre(lote.getProveedorNombre())
                .cantidadInicialUmm(lote.getCantidadInicialUmm())
                .cantidadDisponibleUmm(lote.getCantidadDisponibleUmm())
                .costoUnitarioUmm(lote.getCostoUnitarioUmm())
                .valorInventario(valorDe(lote))
                .fechaIngreso(lote.getFechaIngreso())
                .fechaVencimiento(lote.getFechaVencimiento())
                .diasParaVencer(dias)
                .nivelVencimiento(nivel(dias))
                .agotado(lote.getCantidadDisponibleUmm() != null && lote.getCantidadDisponibleUmm() <= 0)
                .build();
    }

    private BigDecimal valorDe(LoteInventario lote) {
        if (lote.getCostoUnitarioUmm() == null || lote.getCantidadDisponibleUmm() == null) {
            return null;
        }
        return lote.getCostoUnitarioUmm()
                .multiply(BigDecimal.valueOf(lote.getCantidadDisponibleUmm()))
                .setScale(2, REDONDEO);
    }

    private NivelVencimiento nivel(Integer dias) {
        if (dias == null) {
            return NivelVencimiento.SIN_FECHA;
        }
        if (dias < 0) {
            return NivelVencimiento.VENCIDO;
        }
        return dias <= DIAS_POR_VENCER ? NivelVencimiento.POR_VENCER : NivelVencimiento.VIGENTE;
    }

    /** Resumen textual para auditoría y kardex. */
    public String resumenCodigos(List<LoteDTO> lotes) {
        return lotes.stream().map(LoteDTO::getCodigo).collect(Collectors.joining(", "));
    }
}
