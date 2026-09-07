package com.licoreria.pos.service;

import com.licoreria.pos.dto.AlertaVencimientoDTO;
import com.licoreria.pos.dto.EstadoNormativaDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.ReporteCompraItemDTO;
import com.licoreria.pos.dto.ReporteFacturaItemDTO;
import com.licoreria.pos.dto.ReportePeriodoDTO;
import com.licoreria.pos.dto.ReporteProductoVendidoDTO;
import com.licoreria.pos.dto.ReporteVentaItemDTO;
import com.licoreria.pos.dto.ResumenOperativoDTO;
import com.licoreria.pos.dto.TurnoCajaDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Compra;
import com.licoreria.pos.model.DetalleVenta;
import com.licoreria.pos.model.EstadoFactura;
import com.licoreria.pos.model.EstadoTurnoCaja;
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
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final VentaRepository ventaRepository;
    private final FacturaRepository facturaRepository;
    private final CompraRepository compraRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final InventarioService inventarioService;
    private final HorarioVentaService horarioVentaService;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaService cajaService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ResumenOperativoDTO resumenDelDia() {
        LocalDate fecha = LocalDate.now(clock);
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        List<Venta> ventas = ventaRepository.findByFechaBetween(inicio, fin).stream()
                .filter(venta -> venta.getEstado() == EstadoVenta.COMPLETADA)
                .toList();
        List<Factura> facturas = facturaRepository.findByFechaEmisionBetween(inicio, fin);
        List<Compra> compras = compraRepository.findByFechaBetween(inicio, fin);

        BigDecimal ventasSubtotal = sumar(ventas.stream().map(Venta::getSubtotal).toList());
        BigDecimal ventasImpuesto = sumar(ventas.stream().map(Venta::getImpuesto).toList());
        BigDecimal ventasTotal = sumar(ventas.stream().map(Venta::getTotal).toList());
        BigDecimal comprasTotal = sumar(compras.stream().map(Compra::getTotal).toList());
        BigDecimal emitidasTotal = sumar(facturas.stream()
                .filter(factura -> factura.getEstado() == EstadoFactura.EMITIDA)
                .map(Factura::getTotal)
                .toList());

        EstadoNormativaDTO normativa = horarioVentaService.estadoActual();

        return ResumenOperativoDTO.builder()
                .fecha(fecha)
                .ventasCantidad(ventas.size())
                .ventasSubtotal(ventasSubtotal)
                .ventasImpuesto(ventasImpuesto)
                .ventasTotal(ventasTotal)
                .comprasCantidad(compras.size())
                .comprasTotal(comprasTotal)
                .resultadoDia(ventasTotal.subtract(comprasTotal))
                .facturasEmitidas((int) facturas.stream().filter(f -> f.getEstado() == EstadoFactura.EMITIDA).count())
                .facturasAnuladas((int) facturas.stream().filter(f -> f.getEstado() == EstadoFactura.ANULADA).count())
                .facturasTotalEmitido(emitidasTotal)
                .turnosAbiertos((int) turnoCajaRepository.countByEstado(EstadoTurnoCaja.ABIERTO))
                .ventaLicorPermitidaAhora(normativa.isVentaLicorPermitidaAhora())
                .mensajeNormativa(normativa.getMensaje())
                .alertasStock(inventarioService.listarAlertas())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportePeriodoDTO periodo(LocalDate desde, LocalDate hasta) {
        return periodo(desde, hasta, null, null, null, null, 0, PaginacionUtil.TAMANO_MAX);
    }

    @Transactional(readOnly = true)
    public ReportePeriodoDTO periodo(
            LocalDate desde,
            LocalDate hasta,
            String busqueda,
            String formaPago,
            String estadoFactura
    ) {
        return periodo(desde, hasta, busqueda, formaPago, estadoFactura, null, 0, PaginacionUtil.TAMANO_MAX);
    }

    @Transactional(readOnly = true)
    public ReportePeriodoDTO periodo(
            LocalDate desde,
            LocalDate hasta,
            String busqueda,
            String formaPago,
            String estadoFactura,
            String tabla,
            int pagina,
            int tamano
    ) {
        RangoPeriodo rango = validarRango(desde, hasta);
        List<Venta> ventas = ventaRepository.findByFechaBetween(rango.inicio(), rango.fin()).stream()
                .filter(venta -> venta.getEstado() == EstadoVenta.COMPLETADA)
                .sorted(Comparator.comparing(Venta::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<Factura> facturas = facturaRepository.findByFechaEmisionBetween(rango.inicio(), rango.fin()).stream()
                .sorted(Comparator.comparing(Factura::getFechaEmision, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<Compra> compras = compraRepository.findByFechaBetween(rango.inicio(), rango.fin()).stream()
                .sorted(Comparator.comparing(Compra::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<TurnoCajaDTO> turnos = cajaService.turnosEnPeriodo(rango.inicio(), rango.fin()).stream()
                .filter(turno -> coincideBusqueda(busqueda, turno.getUsuarioNombre()))
                .toList();

        Map<Long, String> nombresUsuario = cargarNombresUsuarios(ventas, facturas, turnos);
        Map<Long, Factura> facturasPorVenta = facturasPorVenta(facturas);

        List<Venta> ventasFiltradas = ventas.stream()
                .filter(venta -> coincideVenta(venta, facturasPorVenta.get(venta.getId()), busqueda, formaPago, nombresUsuario))
                .toList();
        List<Factura> facturasFiltradas = facturas.stream()
                .filter(factura -> coincideFactura(factura, busqueda, estadoFactura, nombresUsuario))
                .toList();
        List<Compra> comprasFiltradas = compras.stream()
                .filter(compra -> coincideBusqueda(busqueda, compra.getNumero(), compra.getProveedorNombre()))
                .toList();

        BigDecimal ventasSubtotal = sumar(ventasFiltradas.stream().map(Venta::getSubtotal).toList());
        BigDecimal ventasImpuesto = sumar(ventasFiltradas.stream().map(Venta::getImpuesto).toList());
        BigDecimal ventasTotal = sumar(ventasFiltradas.stream().map(Venta::getTotal).toList());
        BigDecimal comprasTotal = sumar(comprasFiltradas.stream().map(Compra::getTotal).toList());
        BigDecimal emitidasTotal = sumar(facturasFiltradas.stream()
                .filter(factura -> factura.getEstado() == EstadoFactura.EMITIDA)
                .map(Factura::getTotal)
                .toList());
        BigDecimal ventasEfectivo = sumarVentasPorForma(ventasFiltradas, FormaPago.EFECTIVO);
        BigDecimal ventasTarjeta = sumarVentasPorForma(ventasFiltradas, FormaPago.TARJETA)
                .add(sumarVentasPorForma(ventasFiltradas, FormaPago.STRIPE));
        BigDecimal ticketPromedio = ventasFiltradas.isEmpty()
                ? BigDecimal.ZERO
                : ventasTotal.divide(BigDecimal.valueOf(ventasFiltradas.size()), 2, REDONDEO);

        List<ReporteProductoVendidoDTO> productos = agregarProductosVendidos(ventasFiltradas);
        if (busqueda != null && !busqueda.isBlank()) {
            productos = productos.stream()
                    .filter(item -> coincideBusqueda(busqueda, item.getCodigo(), item.getNombre()))
                    .toList();
        }

        List<ReporteVentaItemDTO> ventasItems = ventasFiltradas.stream()
                .map(venta -> toVentaItem(venta, facturasPorVenta.get(venta.getId()), nombresUsuario))
                .toList();
        List<ReporteCompraItemDTO> comprasItems = comprasFiltradas.stream().map(this::toCompraItem).toList();
        List<ReporteFacturaItemDTO> facturasItems = facturasFiltradas.stream()
                .map(factura -> toFacturaItem(factura, nombresUsuario))
                .toList();

        PaginaDTO<?> paginaTabla = paginaDeTabla(tabla, pagina, tamano, ventasItems, comprasItems, facturasItems, productos, turnos);
        boolean recortar = debePaginarTabla(tabla);
        String tablaNorm = normalizarTabla(tabla);

        return ReportePeriodoDTO.builder()
                .desde(rango.desde())
                .hasta(rango.hasta())
                .ventasCantidad(ventasFiltradas.size())
                .ventasSubtotal(ventasSubtotal)
                .ventasImpuesto(ventasImpuesto)
                .ventasTotal(ventasTotal)
                .ventasEfectivoTotal(ventasEfectivo)
                .ventasTarjetaTotal(ventasTarjeta)
                .ticketPromedio(ticketPromedio)
                .comprasCantidad(comprasFiltradas.size())
                .comprasTotal(comprasTotal)
                .resultado(ventasTotal.subtract(comprasTotal))
                .facturasEmitidas((int) facturasFiltradas.stream().filter(f -> f.getEstado() == EstadoFactura.EMITIDA).count())
                .facturasAnuladas((int) facturasFiltradas.stream().filter(f -> f.getEstado() == EstadoFactura.ANULADA).count())
                .facturasTotalEmitido(emitidasTotal)
                .turnosCantidad(turnos.size())
                .ventas(recortar && !"ventas".equals(tablaNorm) ? List.of() : recortar
                        ? paginaDe(ventasItems, pagina, tamano).getContenido()
                        : ventasItems)
                .compras(recortar && !"compras".equals(tablaNorm) ? List.of() : recortar
                        ? paginaDe(comprasItems, pagina, tamano).getContenido()
                        : comprasItems)
                .facturas(recortar && !"facturas".equals(tablaNorm) ? List.of() : recortar
                        ? paginaDe(facturasItems, pagina, tamano).getContenido()
                        : facturasItems)
                .productos(recortar && !"productos".equals(tablaNorm) ? List.of() : recortar
                        ? paginaDe(productos, pagina, tamano).getContenido()
                        : productos)
                .turnos(recortar && !"caja".equals(tablaNorm) ? List.of() : recortar
                        ? paginaDe(turnos, pagina, tamano).getContenido()
                        : turnos)
                .pagina(paginaTabla.getPagina())
                .tamano(paginaTabla.getTamano())
                .totalElementos(paginaTabla.getTotalElementos())
                .totalPaginas(paginaTabla.getTotalPaginas())
                .build();
    }

    private static boolean debePaginarTabla(String tabla) {
        String clave = normalizarTabla(tabla);
        return clave != null && !"finanzas".equals(clave);
    }

    private static String normalizarTabla(String tabla) {
        if (tabla == null || tabla.isBlank()) {
            return null;
        }
        return tabla.trim().toLowerCase(Locale.ROOT);
    }

    private static <T> PaginaDTO<T> paginaDe(List<T> items, int pagina, int tamano) {
        return PaginacionUtil.deLista(items, pagina, tamano);
    }

    private static PaginaDTO<?> paginaDeTabla(
            String tabla,
            int pagina,
            int tamano,
            List<ReporteVentaItemDTO> ventas,
            List<ReporteCompraItemDTO> compras,
            List<ReporteFacturaItemDTO> facturas,
            List<ReporteProductoVendidoDTO> productos,
            List<TurnoCajaDTO> turnos
    ) {
        String clave = normalizarTabla(tabla);
        if ("ventas".equals(clave)) {
            return paginaDe(ventas, pagina, tamano);
        }
        if ("compras".equals(clave)) {
            return paginaDe(compras, pagina, tamano);
        }
        if ("facturas".equals(clave)) {
            return paginaDe(facturas, pagina, tamano);
        }
        if ("productos".equals(clave)) {
            return paginaDe(productos, pagina, tamano);
        }
        if ("caja".equals(clave)) {
            return paginaDe(turnos, pagina, tamano);
        }
        return PaginaDTO.builder()
                .contenido(List.of())
                .pagina(0)
                .tamano(tamano <= 0 ? PaginacionUtil.TAMANO_DEFAULT : tamano)
                .totalElementos(0L)
                .totalPaginas(1)
                .primera(true)
                .ultima(true)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AlertaVencimientoDTO> vencimientos(int diasAlerta) {
        if (diasAlerta < 1 || diasAlerta > 365) {
            throw new ReglaNegocioException("DIAS_ALERTA_INVALIDOS", "Los días de alerta deben estar entre 1 y 365");
        }
        LocalDate hoy = LocalDate.now(clock);
        return productoRepository.findByActivoTrue().stream()
                .filter(producto -> (producto.getStockActual() != null && producto.getStockActual() > 0)
                        || producto.getFechaVencimiento() != null)
                .map(producto -> toVencimiento(producto, hoy, diasAlerta))
                .sorted(Comparator
                        .comparingInt((AlertaVencimientoDTO item) -> orden(item.getEstado()))
                        .thenComparing(item -> item.getFechaVencimiento() == null
                                ? LocalDate.MAX
                                : item.getFechaVencimiento())
                        .thenComparing(AlertaVencimientoDTO::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<AlertaVencimientoDTO> vencimientos(
            int diasAlerta, String busqueda, String estado, int pagina, int tamano) {
        String termino = busqueda == null || busqueda.isBlank() ? null : busqueda.trim().toLowerCase();
        String filtroEstado = estado == null || estado.isBlank() || "todos".equalsIgnoreCase(estado)
                ? null
                : estado.trim().toUpperCase();
        List<AlertaVencimientoDTO> filtrados = vencimientos(diasAlerta).stream()
                .filter(item -> {
                    if ("ATENCION".equals(filtroEstado)) {
                        return item.getEstado() == NivelVencimiento.VENCIDO
                                || item.getEstado() == NivelVencimiento.POR_VENCER;
                    }
                    if (filtroEstado == null) {
                        return true;
                    }
                    return item.getEstado() != null && filtroEstado.equals(item.getEstado().name());
                })
                .filter(item -> termino == null
                        || (item.getCodigo() != null && item.getCodigo().toLowerCase().contains(termino))
                        || (item.getNombre() != null && item.getNombre().toLowerCase().contains(termino)))
                .toList();
        return PaginacionUtil.deLista(filtrados, pagina, tamano);
    }

    static NivelVencimiento clasificar(LocalDate vencimiento, LocalDate hoy, int diasAlerta) {
        if (vencimiento == null) {
            return NivelVencimiento.SIN_FECHA;
        }
        if (vencimiento.isBefore(hoy)) {
            return NivelVencimiento.VENCIDO;
        }
        long dias = ChronoUnit.DAYS.between(hoy, vencimiento);
        if (dias <= diasAlerta) {
            return NivelVencimiento.POR_VENCER;
        }
        return NivelVencimiento.VIGENTE;
    }

    private RangoPeriodo validarRango(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate inicioFecha = desde == null ? hoy : desde;
        LocalDate finFecha = hasta == null ? hoy : hasta;
        if (finFecha.isBefore(inicioFecha)) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "La fecha final no puede ser anterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(inicioFecha, finFecha) > 366) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "El rango no puede superar 366 días");
        }
        return new RangoPeriodo(
                inicioFecha,
                finFecha,
                inicioFecha.atStartOfDay(),
                finFecha.atTime(LocalTime.MAX)
        );
    }

    private Map<Long, Factura> facturasPorVenta(List<Factura> facturas) {
        Map<Long, Factura> porVenta = new HashMap<>();
        for (Factura factura : facturas) {
            if (factura.getVentaId() != null) {
                porVenta.putIfAbsent(factura.getVentaId(), factura);
            }
        }
        return porVenta;
    }

    private Map<Long, String> cargarNombresUsuarios(List<Venta> ventas, List<Factura> facturas, List<TurnoCajaDTO> turnos) {
        Set<Long> ids = new HashSet<>();
        ventas.forEach(venta -> {
            if (venta.getUsuarioId() != null) {
                ids.add(venta.getUsuarioId());
            }
        });
        facturas.forEach(factura -> {
            if (factura.getVentaId() != null) {
                ventaRepository.findById(factura.getVentaId())
                        .map(Venta::getUsuarioId)
                        .ifPresent(ids::add);
            }
        });
        turnos.forEach(turno -> {
            if (turno.getUsuarioId() != null) {
                ids.add(turno.getUsuarioId());
            }
        });
        if (ids.isEmpty()) {
            return Map.of();
        }
        return usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombreCompleto));
    }

    private boolean coincideVenta(
            Venta venta,
            Factura factura,
            String busqueda,
            String formaPago,
            Map<Long, String> nombresUsuario
    ) {
        if (formaPago != null && !formaPago.isBlank()) {
            String esperado = formaPago.trim().toUpperCase(Locale.ROOT);
            String actual = venta.getFormaPago() == null ? "" : venta.getFormaPago().name();
            if (!actual.equals(esperado)) {
                return false;
            }
        }
        String cliente = factura == null ? "Consumidor final" : factura.getClienteNombre();
        String cajero = venta.getUsuarioId() == null ? "" : nombresUsuario.getOrDefault(venta.getUsuarioId(), "");
        return coincideBusqueda(busqueda, venta.getNumero(), cliente, cajero);
    }

    private boolean coincideFactura(Factura factura, String busqueda, String estadoFactura, Map<Long, String> nombresUsuario) {
        if (estadoFactura != null && !estadoFactura.isBlank()) {
            String esperado = estadoFactura.trim().toUpperCase(Locale.ROOT);
            String actual = factura.getEstado() == null ? "" : factura.getEstado().name();
            if (!actual.equals(esperado)) {
                return false;
            }
        }
        String cajero = "";
        if (factura.getVentaId() != null) {
            cajero = ventaRepository.findById(factura.getVentaId())
                    .map(Venta::getUsuarioId)
                    .map(nombresUsuario::get)
                    .orElse("");
        }
        return coincideBusqueda(busqueda, factura.getNumero(), factura.getClienteNombre(), cajero);
    }

    private boolean coincideBusqueda(String busqueda, String... campos) {
        if (busqueda == null || busqueda.isBlank()) {
            return true;
        }
        String q = busqueda.trim().toLowerCase(Locale.ROOT);
        for (String campo : campos) {
            if (campo != null && campo.toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal sumarVentasPorForma(List<Venta> ventas, FormaPago formaPago) {
        return sumar(ventas.stream()
                .filter(venta -> venta.getFormaPago() == formaPago)
                .map(Venta::getTotal)
                .toList());
    }

    private AlertaVencimientoDTO toVencimiento(Producto producto, LocalDate hoy, int diasAlerta) {
        LocalDate vencimiento = producto.getFechaVencimiento();
        return AlertaVencimientoDTO.builder()
                .productoId(producto.getId())
                .codigo(producto.getCodigo())
                .nombre(producto.getNombre())
                .stockActual(producto.getStockActual() == null ? 0 : producto.getStockActual())
                .unidadMinima(producto.getUnidadMinima())
                .fechaVencimiento(vencimiento)
                .diasRestantes(vencimiento == null ? null : ChronoUnit.DAYS.between(hoy, vencimiento))
                .estado(clasificar(vencimiento, hoy, diasAlerta))
                .build();
    }

    private ReporteVentaItemDTO toVentaItem(Venta venta, Factura factura, Map<Long, String> nombresUsuario) {
        String cliente = factura == null
                ? facturaRepository.findByVentaId(venta.getId()).map(Factura::getClienteNombre).orElse("Consumidor final")
                : factura.getClienteNombre();
        return ReporteVentaItemDTO.builder()
                .ventaId(venta.getId())
                .numero(venta.getNumero())
                .fecha(venta.getFecha())
                .clienteNombre(cliente)
                .usuarioId(venta.getUsuarioId())
                .cajeroNombre(venta.getUsuarioId() == null ? null : nombresUsuario.get(venta.getUsuarioId()))
                .formaPago(venta.getFormaPago() == null ? null : venta.getFormaPago().name())
                .tipoCliente(venta.getTipoClienteAplicado() == null ? null : venta.getTipoClienteAplicado().name())
                .subtotal(venta.getSubtotal())
                .impuesto(venta.getImpuesto())
                .total(venta.getTotal())
                .estado(venta.getEstado() == null ? null : venta.getEstado().name())
                .build();
    }

    private ReporteCompraItemDTO toCompraItem(Compra compra) {
        return ReporteCompraItemDTO.builder()
                .compraId(compra.getId())
                .proveedorId(compra.getProveedorId())
                .numero(compra.getNumero())
                .fecha(compra.getFecha())
                .proveedorNombre(compra.getProveedorNombre())
                .total(compra.getTotal())
                .build();
    }

    private ReporteFacturaItemDTO toFacturaItem(Factura factura, Map<Long, String> nombresUsuario) {
        String cajero = null;
        if (factura.getVentaId() != null) {
            cajero = ventaRepository.findById(factura.getVentaId())
                    .map(Venta::getUsuarioId)
                    .map(nombresUsuario::get)
                    .orElse(null);
        }
        return ReporteFacturaItemDTO.builder()
                .facturaId(factura.getId())
                .ventaId(factura.getVentaId())
                .numero(factura.getNumero())
                .fechaEmision(factura.getFechaEmision())
                .clienteNombre(factura.getClienteNombre())
                .cajeroNombre(cajero)
                .total(factura.getTotal())
                .estado(factura.getEstado() == null ? null : factura.getEstado().name())
                .build();
    }

    private List<ReporteProductoVendidoDTO> agregarProductosVendidos(List<Venta> ventas) {
        Map<Long, ReporteProductoVendidoDTO> porProducto = new HashMap<>();
        Set<Long> productoIds = new HashSet<>();
        for (Venta venta : ventas) {
            if (venta.getDetalles() == null) {
                continue;
            }
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getProductoId() != null) {
                    productoIds.add(detalle.getProductoId());
                }
            }
        }
        Map<Long, Producto> catalogo = productoIds.isEmpty()
                ? Map.of()
                : productoRepository.findAllById(productoIds).stream()
                .collect(Collectors.toMap(Producto::getId, producto -> producto));

        for (Venta venta : ventas) {
            if (venta.getDetalles() == null) {
                continue;
            }
            Set<Long> vistos = new HashSet<>();
            for (DetalleVenta detalle : venta.getDetalles()) {
                Long productoId = detalle.getProductoId();
                Producto producto = catalogo.get(productoId);
                ReporteProductoVendidoDTO actual = porProducto.computeIfAbsent(productoId, id -> ReporteProductoVendidoDTO.builder()
                        .productoId(id)
                        .codigo(producto == null ? String.valueOf(id) : producto.getCodigo())
                        .nombre(producto == null ? "Producto " + id : producto.getNombre())
                        .cantidadUmm(0)
                        .tickets(0)
                        .total(BigDecimal.ZERO)
                        .build());
                actual.setCantidadUmm(actual.getCantidadUmm() + (detalle.getCantidadUmm() == null ? 0 : detalle.getCantidadUmm()));
                actual.setTotal(actual.getTotal().add(detalle.getSubtotal() == null ? BigDecimal.ZERO : detalle.getSubtotal()));
                if (vistos.add(productoId)) {
                    actual.setTickets(actual.getTickets() + 1);
                }
            }
        }
        return porProducto.values().stream()
                .sorted(Comparator
                        .comparing(ReporteProductoVendidoDTO::getTotal, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ReporteProductoVendidoDTO::getCantidadUmm, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private int orden(NivelVencimiento estado) {
        return switch (estado) {
            case VENCIDO -> 0;
            case POR_VENCER -> 1;
            case SIN_FECHA -> 2;
            case VIGENTE -> 3;
        };
    }

    private BigDecimal sumar(List<BigDecimal> valores) {
        return valores.stream()
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record RangoPeriodo(LocalDate desde, LocalDate hasta, LocalDateTime inicio, LocalDateTime fin) {
    }
}
