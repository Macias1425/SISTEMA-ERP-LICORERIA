package com.licoreria.pos.service;

import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.FormaPago;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class CobroPos {

    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private CobroPos() {
    }

    static BigDecimal vuelto(FormaPago formaPago, BigDecimal montoRecibido, BigDecimal total) {
        BigDecimal cobrado = total == null ? BigDecimal.ZERO : total.setScale(2, REDONDEO);
        if (formaPago != FormaPago.EFECTIVO) {
            return BigDecimal.ZERO.setScale(2, REDONDEO);
        }
        if (montoRecibido == null) {
            throw new ReglaNegocioException(
                    "PAGO_INSUFICIENTE",
                    "En efectivo debe indicar el monto recibido. Total a cobrar: L " + cobrado.toPlainString()
            );
        }
        BigDecimal recibido = montoRecibido.setScale(2, REDONDEO);
        if (recibido.compareTo(cobrado) < 0) {
            throw new ReglaNegocioException(
                    "PAGO_INSUFICIENTE",
                    "El efectivo recibido (L " + recibido.toPlainString()
                            + ") no cubre el total (L " + cobrado.toPlainString() + ")"
            );
        }
        return recibido.subtract(cobrado).setScale(2, REDONDEO);
    }

    static BigDecimal montoRegistrado(FormaPago formaPago, BigDecimal montoRecibido, BigDecimal total) {
        BigDecimal cobrado = total == null ? BigDecimal.ZERO : total.setScale(2, REDONDEO);
        if (formaPago != FormaPago.EFECTIVO) {
            return cobrado;
        }
        return montoRecibido.setScale(2, REDONDEO);
    }
}
