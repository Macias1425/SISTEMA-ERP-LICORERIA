package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.dto.CotizacionDTO;
import com.licoreria.pos.dto.CotizacionLineaDTO;
import com.licoreria.pos.dto.CotizacionRequestDTO;
import com.licoreria.pos.dto.DetalleVentaDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cotiza el carrito con las mismas reglas de precio que aplica {@link VentaService} al cobrar.
 * Es de solo lectura: no mueve stock ni exige credenciales de supervisor, únicamente informa.
 */
@Service
@RequiredArgsConstructor
public class CotizacionVentaService {

    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final ConversionUnidades conversionUnidades;
    private final ListaPrecioService listaPrecioService;
    private final HorarioVentaService horarioVentaService;
    private final ControlVentasService controlVentasService;
    private final AccesoService accesoService;
    private final PosProperties posProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CotizacionDTO cotizar(CotizacionRequestDTO request) {
        accesoService.exigirPermiso(Permiso.VENTAS_CREAR);

        LocalDate hoy = LocalDate.now(clock);
        TipoCliente tipoSolicitado = request.getTipoCliente() == null ? TipoCliente.DETAL : request.getTipoCliente();
        List<LineaCotizada> preparadas = prepararLineas(request.getDetalles());

        int ummTicket = preparadas.stream().mapToInt(LineaCotizada::cantidadUmm).sum();
        TipoCliente tipoAplicado = listaPrecioService.resolverTipoAplicado(tipoSolicitado, ummTicket);
        Map<Long, Integer> demandaPorProducto = demandaPorProducto(preparadas);

        List<String> avisos = new ArrayList<>();
        if (tipoSolicitado != TipoCliente.DETAL && tipoAplicado == TipoCliente.DETAL) {
            avisos.add("Volumen insuficiente para tarifa mayorista; se aplicará precio detal.");
        }

        List<CotizacionLineaDTO> lineas = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean hayAlcohol = false;
        boolean cobrable = true;

        for (LineaCotizada linea : preparadas) {
            ListaPrecioService.PrecioResuelto precio = listaPrecioService.resolverPrecio(
                    linea.producto(), tipoAplicado, linea.cantidadUmm());

            BigDecimal precioUmm = TarifaVenta.dinero(precio.getPrecioUmm());
            int factor = linea.presentacion().getFactorAUnidadMinima();
            BigDecimal precioPresentacion = TarifaVenta.precioPresentacion(precioUmm, factor);
            BigDecimal subtotalLinea = TarifaVenta.subtotalLinea(precioPresentacion, linea.cantidad());
            subtotal = subtotal.add(subtotalLinea);

            boolean vencido = estaVencido(linea.producto(), hoy);
            int disponible = stockDisponible(linea.producto());
            boolean stockSuficiente = disponible >= demandaPorProducto.getOrDefault(linea.producto().getId(), 0);
            boolean requiereAutorizacion = requiereAutorizacionPrecio(linea.precioSolicitado(), precioPresentacion);
            hayAlcohol = hayAlcohol || Boolean.TRUE.equals(linea.producto().getEsAlcoholico());
            cobrable = cobrable && !vencido && stockSuficiente;

            lineas.add(CotizacionLineaDTO.builder()
                    .productoId(linea.producto().getId())
                    .productoNombre(linea.producto().getNombre())
                    .presentacionId(linea.presentacion().getId())
                    .presentacionNombre(linea.presentacion().getNombre())
                    .factorAUnidadMinima(factor)
                    .cantidad(linea.cantidad())
                    .cantidadUmm(linea.cantidadUmm())
                    .precioUnitarioUmm(precioUmm)
                    .precioUnitario(precioPresentacion)
                    .subtotal(subtotalLinea)
                    .tipoAplicadoLinea(precio.getTipoAplicadoLinea())
                    .precioSolicitado(linea.precioSolicitado())
                    .requiereAutorizacion(requiereAutorizacion)
                    .stockDisponibleUmm(disponible)
                    .stockSuficiente(stockSuficiente)
                    .vencido(vencido)
                    .esAlcoholico(linea.producto().getEsAlcoholico())
                    .aviso(avisoLinea(vencido, stockSuficiente, requiereAutorizacion))
                    .build());
        }

        BigDecimal tasa = posProperties.getImpuesto().getTasaIva();
        BigDecimal impuesto = TarifaVenta.impuesto(subtotal, tasa);
        BigDecimal total = TarifaVenta.dinero(subtotal.add(impuesto));

        boolean licorPermitido = !hayAlcohol || horarioVentaService.estadoActual().isVentaLicorPermitidaAhora();
        if (!licorPermitido) {
            avisos.add("Fuera del horario autorizado para venta de licor.");
            cobrable = false;
        }

        BigDecimal montoSupervisor = controlVentasService.reglasOperativas().getMontoSupervisorRequerido();
        boolean requiereSupervisor = montoSupervisor != null && total.compareTo(montoSupervisor) >= 0;
        if (requiereSupervisor) {
            avisos.add("Venta de alto monto: requerirá autorización de administrador.");
        }
        if (lineas.stream().anyMatch(linea -> Boolean.TRUE.equals(linea.getRequiereAutorizacion()))) {
            avisos.add("Hay precios distintos al catálogo: requerirán autorización de administrador.");
        }

        return CotizacionDTO.builder()
                .tipoClienteSolicitado(tipoSolicitado)
                .tipoClienteAplicado(tipoAplicado)
                .cantidadUmmTicket(ummTicket)
                .volumenMinimoMayorista(posProperties.getMayorista().getVolumenMinimoUmm())
                .subtotal(TarifaVenta.dinero(subtotal))
                .tasaIva(tasa)
                .impuesto(impuesto)
                .total(total)
                .hayAlcohol(hayAlcohol)
                .ventaLicorPermitidaAhora(licorPermitido)
                .requiereAutorizacionSupervisor(requiereSupervisor)
                .montoSupervisorRequerido(montoSupervisor)
                .cobrable(cobrable)
                .lineas(lineas)
                .avisos(avisos)
                .build();
    }

    private List<LineaCotizada> prepararLineas(List<DetalleVentaDTO> detalles) {
        List<LineaCotizada> lineas = new ArrayList<>();
        for (DetalleVentaDTO dto : detalles) {
            Producto producto = productoRepository.findById(dto.getProductoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + dto.getProductoId()));
            Presentacion presentacion = inventarioService.obtenerPresentacion(producto.getId(), dto.getPresentacionId());
            int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, dto.getCantidad());
            lineas.add(new LineaCotizada(producto, presentacion, dto.getCantidad(), cantidadUmm, dto.getPrecioUnitario()));
        }
        return lineas;
    }

    private Map<Long, Integer> demandaPorProducto(List<LineaCotizada> lineas) {
        Map<Long, Integer> demanda = new LinkedHashMap<>();
        for (LineaCotizada linea : lineas) {
            demanda.merge(linea.producto().getId(), linea.cantidadUmm(), Integer::sum);
        }
        return demanda;
    }

    private boolean estaVencido(Producto producto, LocalDate hoy) {
        return producto.getFechaVencimiento() != null && producto.getFechaVencimiento().isBefore(hoy);
    }

    private int stockDisponible(Producto producto) {
        return producto.getStockActual() == null ? 0 : producto.getStockActual();
    }

    private boolean requiereAutorizacionPrecio(BigDecimal solicitado, BigDecimal catalogo) {
        return solicitado != null && solicitado.compareTo(catalogo) != 0;
    }

    private String avisoLinea(boolean vencido, boolean stockSuficiente, boolean requiereAutorizacion) {
        if (vencido) {
            return "Producto vencido: no se puede vender";
        }
        if (!stockSuficiente) {
            return "Stock insuficiente para la cantidad solicitada";
        }
        if (requiereAutorizacion) {
            return "Precio distinto al catálogo: requiere administrador";
        }
        return null;
    }

    private record LineaCotizada(
            Producto producto,
            Presentacion presentacion,
            int cantidad,
            int cantidadUmm,
            BigDecimal precioSolicitado
    ) {
    }
}
