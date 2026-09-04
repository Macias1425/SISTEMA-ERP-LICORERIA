package com.licoreria.pos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Aritmética de precios compartida por la cotización del POS y el registro de la venta,
 * para que el cajero vea en pantalla exactamente el total que cobrará el servidor.
 */
public final class TarifaVenta {

    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final int ESCALA_DINERO = 2;
    private static final int ESCALA_UMM = 4;

    private TarifaVenta() {
    }

    /** Precio de la presentación (caja, six-pack) a partir del precio por unidad mínima. */
    public static BigDecimal precioPresentacion(BigDecimal precioUmm, int factorAUnidadMinima) {
        return precioUmm
                .multiply(BigDecimal.valueOf(factorAUnidadMinima))
                .setScale(ESCALA_DINERO, REDONDEO);
    }

    /** Precio por unidad mínima implícito en un precio de presentación (usado en overrides). */
    public static BigDecimal precioUmmDesdePresentacion(BigDecimal precioPresentacion, int factorAUnidadMinima) {
        return precioPresentacion.divide(BigDecimal.valueOf(factorAUnidadMinima), ESCALA_UMM, REDONDEO);
    }

    public static BigDecimal subtotalLinea(BigDecimal precioPresentacion, int cantidadPresentacion) {
        return precioPresentacion
                .multiply(BigDecimal.valueOf(cantidadPresentacion))
                .setScale(ESCALA_DINERO, REDONDEO);
    }

    public static BigDecimal impuesto(BigDecimal subtotal, BigDecimal tasaIva) {
        return subtotal.multiply(tasaIva).setScale(ESCALA_DINERO, REDONDEO);
    }

    public static BigDecimal dinero(BigDecimal valor) {
        return valor == null
                ? BigDecimal.ZERO.setScale(ESCALA_DINERO, REDONDEO)
                : valor.setScale(ESCALA_DINERO, REDONDEO);
    }
}
