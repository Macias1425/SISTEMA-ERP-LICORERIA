package com.licoreria.pos.service;

import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.DecisionPoliticaPrecio;
import com.licoreria.pos.model.PoliticaPrecio;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reglas de precio de venta según la política del producto cuando cambia el costo por compra.
 */
@Service
@RequiredArgsConstructor
public class PoliticaPrecioService {

    public static final BigDecimal MARGEN_DEFECTO = new BigDecimal("20");
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final RoundingMode REDONDEO_VENTA = RoundingMode.CEILING;

    private final AuditoriaService auditoriaService;

    /**
     * Tras actualizar {@code precioCompra} por una compra, aplica la política del producto.
     */
    public ResultadoPoliticaPrecio aplicarTrasCompra(Producto producto, BigDecimal costoAnterior, BigDecimal costoNuevo,
                                                     Usuario operador, String referenciaCompra) {
        return aplicarTrasCompra(producto, costoAnterior, costoNuevo, operador, referenciaCompra, null);
    }

    public ResultadoPoliticaPrecio aplicarTrasCompra(Producto producto, BigDecimal costoAnterior, BigDecimal costoNuevo,
                                                     Usuario operador, String referenciaCompra,
                                                     BigDecimal precioVentaExplicito) {
        PoliticaPrecio politica = producto.getPoliticaPrecio() == null
                ? PoliticaPrecio.MANUAL
                : producto.getPoliticaPrecio();
        BigDecimal margen = margenEfectivo(producto);
        BigDecimal ventaAnterior = producto.getPrecioVenta();
        BigDecimal sugerido = precioVentaDesdeMargen(costoNuevo, margen);

        if (politica == PoliticaPrecio.MANUAL) {
            if (precioVentaExplicito != null) {
                return aplicarManualExplicito(producto, costoAnterior, costoNuevo, operador, referenciaCompra,
                        precioVentaExplicito);
            }
            return sinCambio(politica, ventaAnterior, sugerido,
                    "Política manual: se mantiene el precio de venta vigente.");
        }
        if (politica != PoliticaPrecio.AUTOMATICO_MARKUP && !costoCambio(costoAnterior, costoNuevo)) {
            return sinCambio(politica, ventaAnterior, sugerido,
                    "El costo no cambió; no se evalúa la política de precio.");
        }

        return switch (politica) {
            case MANUAL -> sinCambio(politica, ventaAnterior, sugerido,
                    "Política manual: se mantiene el precio de venta vigente.");
            case SUGERIDO -> registrarSugerido(producto, costoAnterior, costoNuevo, margen, operador, referenciaCompra,
                    ventaAnterior, sugerido);
            case AUTOMATICO_MARKUP -> aplicarMarkup(producto, costoAnterior, costoNuevo, margen, operador,
                    referenciaCompra, ventaAnterior, sugerido);
        };
    }

    /**
     * Al guardar catálogo con política automática, sincroniza venta con costo + margen.
     */
    public BigDecimal aplicarEnCatalogo(Producto producto) {
        if (producto == null || producto.getPoliticaPrecio() != PoliticaPrecio.AUTOMATICO_MARKUP) {
            return null;
        }
        BigDecimal venta = precioVentaDesdeMargen(producto.getPrecioCompra(), margenEfectivo(producto));
        if (venta == null) {
            return null;
        }
        producto.setPrecioVenta(venta);
        return venta;
    }

    public BigDecimal precioVentaDesdeMargen(BigDecimal costo, BigDecimal margenPorcentaje) {
        if (costo == null || costo.signum() <= 0) {
            return null;
        }
        BigDecimal margen = normalizarMargen(margenPorcentaje);
        BigDecimal divisor = CIEN.subtract(margen);
        if (divisor.signum() <= 0) {
            return null;
        }
        return costo.multiply(CIEN).divide(divisor, 2, REDONDEO_VENTA);
    }

    public BigDecimal margenEfectivo(Producto producto) {
        if (producto == null || producto.getMargenObjetivoPct() == null) {
            return MARGEN_DEFECTO;
        }
        return normalizarMargen(producto.getMargenObjetivoPct());
    }

    public BigDecimal calcularMarkupPct(BigDecimal costo, BigDecimal venta) {
        if (costo == null || venta == null || costo.signum() <= 0) {
            return null;
        }
        return venta.subtract(costo)
                .multiply(CIEN)
                .divide(costo, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularMargenPct(BigDecimal costo, BigDecimal venta) {
        if (costo == null || venta == null || venta.signum() <= 0) {
            return null;
        }
        return venta.subtract(costo)
                .multiply(CIEN)
                .divide(venta, 4, RoundingMode.HALF_UP);
    }

    public void validarMargen(BigDecimal margen) {
        if (margen == null) {
            return;
        }
        if (margen.signum() < 0 || margen.compareTo(CIEN) >= 0) {
            throw new ReglaNegocioException(
                    "MARGEN_INVALIDO",
                    "El margen objetivo debe estar entre 0 y 99.99 %"
            );
        }
    }

    private ResultadoPoliticaPrecio aplicarManualExplicito(Producto producto, BigDecimal costoAnterior,
                                                           BigDecimal costoNuevo, Usuario operador,
                                                           String referenciaCompra, BigDecimal precioVentaExplicito) {
        BigDecimal nuevoVenta = precioVentaExplicito.setScale(2, RoundingMode.HALF_UP);
        if (nuevoVenta.compareTo(costoNuevo) < 0) {
            throw new ReglaNegocioException("PRECIO_VENTA_MENOR_COSTO",
                    "El precio de venta no puede ser menor al costo de compra");
        }
        BigDecimal ventaAnterior = producto.getPrecioVenta();
        if (ventaAnterior != null && ventaAnterior.compareTo(nuevoVenta) == 0) {
            return sinCambio(PoliticaPrecio.MANUAL, ventaAnterior,
                    precioVentaDesdeMargen(costoNuevo, margenEfectivo(producto)),
                    "Precio manual explícito coincide con el vigente.");
        }
        producto.setPrecioVenta(nuevoVenta);
        auditoriaService.registrar(operador, AccionAuditoria.CAMBIO_PRECIO, "Producto", producto.getId(),
                "compra=" + costoAnterior.toPlainString() + ", venta=" + (ventaAnterior == null ? "0" : ventaAnterior.toPlainString()),
                "compra=" + costoNuevo.toPlainString() + ", venta=" + nuevoVenta.toPlainString(),
                "Precio manual por recepción " + referenciaCompra);
        return ResultadoPoliticaPrecio.builder()
                .decision(DecisionPoliticaPrecio.APLICADA_MANUAL)
                .ventaAnterior(ventaAnterior)
                .ventaNueva(nuevoVenta)
                .ventaSugerida(nuevoVenta)
                .mensaje("Política manual: se aplicó venta C$" + nuevoVenta.toPlainString()
                        + " enviada en la recepción.")
                .ventaAplicada(true)
                .politica(PoliticaPrecio.MANUAL)
                .build();
    }

    private ResultadoPoliticaPrecio registrarSugerido(Producto producto, BigDecimal costoAnterior, BigDecimal costoNuevo,
                                                      BigDecimal margen, Usuario operador, String referenciaCompra,
                                                      BigDecimal ventaAnterior, BigDecimal sugerido) {
        if (sugerido == null) {
            return sinCambio(PoliticaPrecio.SUGERIDO, ventaAnterior, null,
                    "No se pudo calcular precio sugerido.");
        }
        auditoriaService.registrar(operador, AccionAuditoria.CAMBIO_PRECIO, "Producto", producto.getId(),
                "compra=" + costoAnterior.toPlainString() + ", venta=" + (ventaAnterior == null ? "0" : ventaAnterior.toPlainString()),
                "compra=" + costoNuevo.toPlainString() + ", sugerido=" + sugerido.toPlainString(),
                "SUGERIDO: proveedor subió costo en " + referenciaCompra
                        + ". CPP C$" + costoNuevo.toPlainString()
                        + ". Para margen " + margen.stripTrailingZeros().toPlainString()
                        + "%, vender a C$" + sugerido.toPlainString()
                        + " (venta actual C$" + (ventaAnterior == null ? "0" : ventaAnterior.toPlainString()) + ")");
        String mensajeUsuario = costoNuevo.compareTo(costoAnterior == null ? BigDecimal.ZERO : costoAnterior) > 0
                ? "El proveedor subió el costo. CPP C$" + costoNuevo.toPlainString()
                + ". Para no perder margen (" + margen.stripTrailingZeros().toPlainString()
                + "%), venda a C$" + sugerido.toPlainString()
                + ". El POS sigue en C$" + (ventaAnterior == null ? "0" : ventaAnterior.toPlainString()) + "."
                : "CPP C$" + costoNuevo.toPlainString() + ". Precio sugerido C$" + sugerido.toPlainString()
                + " para margen " + margen.stripTrailingZeros().toPlainString() + "%.";
        return ResultadoPoliticaPrecio.builder()
                .decision(DecisionPoliticaPrecio.SOLO_SUGERIDO)
                .ventaAnterior(ventaAnterior)
                .ventaNueva(ventaAnterior)
                .ventaSugerida(sugerido)
                .mensaje(mensajeUsuario)
                .ventaAplicada(false)
                .politica(PoliticaPrecio.SUGERIDO)
                .build();
    }

    private ResultadoPoliticaPrecio aplicarMarkup(Producto producto, BigDecimal costoAnterior, BigDecimal costoNuevo,
                                                  BigDecimal margen, Usuario operador, String referenciaCompra,
                                                  BigDecimal ventaAnterior, BigDecimal sugerido) {
        if (sugerido == null) {
            return sinCambio(PoliticaPrecio.AUTOMATICO_MARKUP, ventaAnterior, null,
                    "No se pudo calcular venta automática.");
        }
        if (ventaAnterior != null && ventaAnterior.compareTo(sugerido) == 0) {
            return ResultadoPoliticaPrecio.builder()
                    .decision(DecisionPoliticaPrecio.APLICADA_AUTO)
                    .ventaAnterior(ventaAnterior)
                    .ventaNueva(sugerido)
                    .ventaSugerida(sugerido)
                    .mensaje("Política automático: la venta C$" + sugerido.toPlainString() + " ya coincide con el markup.")
                    .ventaAplicada(false)
                    .politica(PoliticaPrecio.AUTOMATICO_MARKUP)
                    .build();
        }
        producto.setPrecioVenta(sugerido);
        auditoriaService.registrar(operador, AccionAuditoria.CAMBIO_PRECIO, "Producto", producto.getId(),
                "compra=" + costoAnterior.toPlainString() + ", venta=" + (ventaAnterior == null ? "0" : ventaAnterior.toPlainString()),
                "compra=" + costoNuevo.toPlainString() + ", venta=" + sugerido.toPlainString(),
                "Markup automático por compra " + referenciaCompra
                        + " (margen " + margen.stripTrailingZeros().toPlainString() + "%)");
        return ResultadoPoliticaPrecio.builder()
                .decision(DecisionPoliticaPrecio.APLICADA_AUTO)
                .ventaAnterior(ventaAnterior)
                .ventaNueva(sugerido)
                .ventaSugerida(sugerido)
                .mensaje(null)
                .ventaAplicada(true)
                .politica(PoliticaPrecio.AUTOMATICO_MARKUP)
                .build();
    }

    private ResultadoPoliticaPrecio sinCambio(PoliticaPrecio politica, BigDecimal ventaAnterior,
                                              BigDecimal sugerido, String mensaje) {
        return ResultadoPoliticaPrecio.builder()
                .decision(DecisionPoliticaPrecio.SIN_CAMBIO)
                .ventaAnterior(ventaAnterior)
                .ventaNueva(ventaAnterior)
                .ventaSugerida(sugerido)
                .mensaje(mensaje)
                .ventaAplicada(false)
                .politica(politica)
                .build();
    }

    private boolean costoCambio(BigDecimal costoAnterior, BigDecimal costoNuevo) {
        if (costoNuevo == null) {
            return false;
        }
        BigDecimal nuevo = costoNuevo.setScale(2, RoundingMode.HALF_UP);
        if (costoAnterior == null) {
            return nuevo.signum() > 0;
        }
        return costoAnterior.setScale(2, RoundingMode.HALF_UP).compareTo(nuevo) != 0;
    }

    private BigDecimal normalizarMargen(BigDecimal margen) {
        if (margen == null || margen.signum() <= 0 || margen.compareTo(CIEN) >= 0) {
            return MARGEN_DEFECTO;
        }
        return margen.setScale(2, RoundingMode.HALF_UP);
    }
}
