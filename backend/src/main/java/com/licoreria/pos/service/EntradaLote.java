package com.licoreria.pos.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos con los que nace un lote en una entrada de inventario.
 * Cuando no se conocen (ajuste manual, devolución) el {@link LoteService} usa los del producto.
 */
public record EntradaLote(
        BigDecimal costoUnitarioUmm,
        LocalDate fechaVencimiento,
        Long compraId,
        String proveedorNombre
) {

    public static EntradaLote sinDatos() {
        return new EntradaLote(null, null, null, null);
    }

    public static EntradaLote deCompra(BigDecimal costoUnitarioUmm, LocalDate fechaVencimiento,
                                       Long compraId, String proveedorNombre) {
        return new EntradaLote(costoUnitarioUmm, fechaVencimiento, compraId, proveedorNombre);
    }
}
