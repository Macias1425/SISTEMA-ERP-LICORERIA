package com.licoreria.pos.service;

import com.licoreria.pos.dto.FinanzasComparativaDTO;
import com.licoreria.pos.dto.FinanzasFlujoCajaDTO;
import com.licoreria.pos.dto.FinanzasMargenProductoDTO;
import com.licoreria.pos.dto.FinanzasPeriodoDTO;
import com.licoreria.pos.dto.FinanzasSerieDiariaDTO;
import com.licoreria.pos.dto.ReportePeriodoDTO;
import com.licoreria.pos.dto.TurnoCajaDTO;
import com.licoreria.pos.model.Compra;
import com.licoreria.pos.model.DetalleVenta;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.repository.CompraRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanzasService {

    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final ReporteService reporteService;
    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public FinanzasPeriodoDTO periodo(LocalDate desde, LocalDate hasta, String formaPago) {
        ReportePeriodoDTO base = reporteService.periodo(desde, hasta, null, formaPago, null);
        RangoFechas rango = new RangoFechas(base.getDesde(), base.getHasta());

        List<Venta> ventas = ventasCompletadas(rango);
        if (formaPago != null && !formaPago.isBlank()) {
            String esperado = formaPago.trim().toUpperCase();
            ventas = ventas.stream()
                    .filter(v -> v.getFormaPago() != null && v.getFormaPago().name().equals(esperado))
                    .toList();
        }

        List<Compra> compras = compraRepository.findByFechaBetween(rango.inicio(), rango.fin());
        MargenCalculado margen = calcularMargen(ventas);
        FinanzasFlujoCajaDTO flujo = construirFlujo(base, margen);
        FinanzasComparativaDTO comparativa = construirComparativa(rango, formaPago, margen.margenBruto());
        List<FinanzasSerieDiariaDTO> serie = construirSerieDiaria(rango, ventas, compras);
        List<FinanzasMargenProductoDTO> productos = margen.productos().stream()
                .sorted(Comparator
                        .comparing(FinanzasMargenProductoDTO::getMargen, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FinanzasMargenProductoDTO::getIngresos, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(15)
                .toList();

        return FinanzasPeriodoDTO.builder()
                .desde(base.getDesde())
                .hasta(base.getHasta())
                .ventasCantidad(base.getVentasCantidad())
                .ventasSubtotal(base.getVentasSubtotal())
                .ventasImpuesto(base.getVentasImpuesto())
                .ventasTotal(base.getVentasTotal())
                .ventasEfectivoTotal(base.getVentasEfectivoTotal())
                .ventasTarjetaTotal(base.getVentasTarjetaTotal())
                .ticketPromedio(base.getTicketPromedio())
                .comprasCantidad(base.getComprasCantidad())
                .comprasTotal(base.getComprasTotal())
                .costoVentas(margen.costoVentas())
                .margenBruto(margen.margenBruto())
                .margenBrutoPct(margen.margenBrutoPct())
                .resultado(base.getResultado())
                .facturasEmitidas(base.getFacturasEmitidas())
                .facturasAnuladas(base.getFacturasAnuladas())
                .facturasTotalEmitido(base.getFacturasTotalEmitido())
                .turnosCantidad(base.getTurnosCantidad())
                .flujo(flujo)
                .comparativa(comparativa)
                .serieDiaria(serie)
                .margenProductos(productos)
                .build();
    }

    private FinanzasComparativaDTO construirComparativa(RangoFechas rango, String formaPago, BigDecimal margenActual) {
        long dias = ChronoUnit.DAYS.between(rango.desde(), rango.hasta()) + 1;
        LocalDate anteriorFin = rango.desde().minusDays(1);
        LocalDate anteriorInicio = anteriorFin.minusDays(dias - 1);

        ReportePeriodoDTO anterior = reporteService.periodo(anteriorInicio, anteriorFin, null, formaPago, null);
        List<Venta> ventasAnteriores = ventasCompletadas(new RangoFechas(anteriorInicio, anteriorFin));
        if (formaPago != null && !formaPago.isBlank()) {
            String esperado = formaPago.trim().toUpperCase();
            ventasAnteriores = ventasAnteriores.stream()
                    .filter(v -> v.getFormaPago() != null && v.getFormaPago().name().equals(esperado))
                    .toList();
        }
        BigDecimal margenAnterior = calcularMargen(ventasAnteriores).margenBruto();

        ReportePeriodoDTO actual = reporteService.periodo(rango.desde(), rango.hasta(), null, formaPago, null);

        return FinanzasComparativaDTO.builder()
                .periodoAnteriorDesde(anteriorInicio)
                .periodoAnteriorHasta(anteriorFin)
                .ventasActual(actual.getVentasTotal())
                .ventasAnterior(anterior.getVentasTotal())
                .ventasVariacionPct(variacionPct(actual.getVentasTotal(), anterior.getVentasTotal()))
                .comprasActual(actual.getComprasTotal())
                .comprasAnterior(anterior.getComprasTotal())
                .comprasVariacionPct(variacionPct(actual.getComprasTotal(), anterior.getComprasTotal()))
                .resultadoActual(actual.getResultado())
                .resultadoAnterior(anterior.getResultado())
                .resultadoVariacionPct(variacionPct(actual.getResultado(), anterior.getResultado()))
                .margenActual(margenActual)
                .margenAnterior(margenAnterior)
                .margenVariacionPct(variacionPct(margenActual, margenAnterior))
                .build();
    }

    private FinanzasFlujoCajaDTO construirFlujo(ReportePeriodoDTO base, MargenCalculado margen) {
        BigDecimal faltante = BigDecimal.ZERO;
        BigDecimal sobrante = BigDecimal.ZERO;
        int cuadrados = 0;
        int conDiferencia = 0;

        for (TurnoCajaDTO turno : base.getTurnos()) {
            BigDecimal dif = turno.getDiferencia() == null ? BigDecimal.ZERO : turno.getDiferencia();
            if ("FALTANTE".equalsIgnoreCase(turno.getResultadoArqueo())) {
                faltante = faltante.add(dif.abs());
                conDiferencia++;
            } else if ("SOBRANTE".equalsIgnoreCase(turno.getResultadoArqueo())) {
                sobrante = sobrante.add(dif.abs());
                conDiferencia++;
            } else if ("CUADRADO".equalsIgnoreCase(turno.getResultadoArqueo())) {
                cuadrados++;
            }
        }

        return FinanzasFlujoCajaDTO.builder()
                .entradasVentas(base.getVentasTotal())
                .entradasEfectivo(base.getVentasEfectivoTotal())
                .entradasTarjeta(base.getVentasTarjetaTotal())
                .salidasCompras(base.getComprasTotal())
                .flujoNeto(base.getResultado())
                .ivaCobrado(base.getVentasImpuesto())
                .faltanteCaja(faltante)
                .sobranteCaja(sobrante)
                .turnosCuadrados(cuadrados)
                .turnosConDiferencia(conDiferencia)
                .build();
    }

    private List<FinanzasSerieDiariaDTO> construirSerieDiaria(RangoFechas rango, List<Venta> ventas, List<Compra> compras) {
        Map<LocalDate, FinanzasSerieDiariaDTO> porDia = new HashMap<>();
        LocalDate cursor = rango.desde();
        while (!cursor.isAfter(rango.hasta())) {
            porDia.put(cursor, FinanzasSerieDiariaDTO.builder().fecha(cursor).build());
            cursor = cursor.plusDays(1);
        }

        for (Venta venta : ventas) {
            if (venta.getFecha() == null) continue;
            LocalDate dia = venta.getFecha().toLocalDate();
            FinanzasSerieDiariaDTO punto = porDia.get(dia);
            if (punto == null) continue;
            punto.setVentas(punto.getVentas().add(nvl(venta.getTotal())));
            punto.setMargenBruto(punto.getMargenBruto().add(calcularMargen(List.of(venta)).margenBruto()));
        }

        for (Compra compra : compras) {
            if (compra.getFecha() == null) continue;
            LocalDate dia = compra.getFecha().toLocalDate();
            FinanzasSerieDiariaDTO punto = porDia.get(dia);
            if (punto == null) continue;
            punto.setCompras(punto.getCompras().add(nvl(compra.getTotal())));
        }

        return porDia.values().stream()
                .peek(p -> p.setResultado(p.getVentas().subtract(p.getCompras())))
                .sorted(Comparator.comparing(FinanzasSerieDiariaDTO::getFecha))
                .toList();
    }

    private MargenCalculado calcularMargen(List<Venta> ventas) {
        Set<Long> productoIds = new HashSet<>();
        for (Venta venta : ventas) {
            if (venta.getDetalles() == null) continue;
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getProductoId() != null) {
                    productoIds.add(detalle.getProductoId());
                }
            }
        }

        Map<Long, Producto> catalogo = productoIds.isEmpty()
                ? Map.of()
                : productoRepository.findAllById(productoIds).stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        Map<Long, FinanzasMargenProductoDTO> porProducto = new HashMap<>();
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal costo = BigDecimal.ZERO;

        for (Venta venta : ventas) {
            if (venta.getDetalles() == null) continue;
            for (DetalleVenta detalle : venta.getDetalles()) {
                Long productoId = detalle.getProductoId();
                Producto producto = catalogo.get(productoId);
                int umm = detalle.getCantidadUmm() == null ? 0 : detalle.getCantidadUmm();
                BigDecimal lineaIngreso = nvl(detalle.getSubtotal());
                BigDecimal precioCompra = producto == null || producto.getPrecioCompra() == null
                        ? BigDecimal.ZERO
                        : producto.getPrecioCompra();
                BigDecimal lineaCosto = precioCompra.multiply(BigDecimal.valueOf(umm));

                ingresos = ingresos.add(lineaIngreso);
                costo = costo.add(lineaCosto);

                FinanzasMargenProductoDTO item = porProducto.computeIfAbsent(productoId, id ->
                        FinanzasMargenProductoDTO.builder()
                                .productoId(id)
                                .codigo(producto == null ? String.valueOf(id) : producto.getCodigo())
                                .nombre(producto == null ? "Producto " + id : producto.getNombre())
                                .build());
                item.setCantidadUmm(item.getCantidadUmm() + umm);
                item.setIngresos(item.getIngresos().add(lineaIngreso));
                item.setCosto(item.getCosto().add(lineaCosto));
            }
        }

        List<FinanzasMargenProductoDTO> productos = porProducto.values().stream()
                .peek(item -> {
                    item.setMargen(item.getIngresos().subtract(item.getCosto()));
                    item.setMargenPct(porcentaje(item.getMargen(), item.getIngresos()));
                })
                .toList();

        BigDecimal margenBruto = ingresos.subtract(costo);
        return new MargenCalculado(costo, margenBruto, porcentaje(margenBruto, ingresos), productos);
    }

    private List<Venta> ventasCompletadas(RangoFechas rango) {
        return ventaRepository.findByFechaBetween(rango.inicio(), rango.fin()).stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .toList();
    }

    static BigDecimal variacionPct(BigDecimal actual, BigDecimal anterior) {
        BigDecimal a = nvl(actual);
        BigDecimal b = nvl(anterior);
        if (b.compareTo(BigDecimal.ZERO) == 0) {
            return a.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        return a.subtract(b)
                .multiply(BigDecimal.valueOf(100))
                .divide(b.abs(), 2, REDONDEO);
    }

    static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return nvl(parte).multiply(BigDecimal.valueOf(100)).divide(total, 2, REDONDEO);
    }

    static BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private record RangoFechas(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio() {
            return desde.atStartOfDay();
        }

        LocalDateTime fin() {
            return hasta.atTime(LocalTime.MAX);
        }
    }

    private record MargenCalculado(
            BigDecimal costoVentas,
            BigDecimal margenBruto,
            BigDecimal margenBrutoPct,
            List<FinanzasMargenProductoDTO> productos
    ) {
    }
}
