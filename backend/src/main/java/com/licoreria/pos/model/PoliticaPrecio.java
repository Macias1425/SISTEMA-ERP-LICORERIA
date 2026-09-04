package com.licoreria.pos.model;

/**
 * Define qué ocurre con el precio de venta cuando el costo cambia por una compra recibida.
 */
public enum PoliticaPrecio {
    /** El costo se actualiza; el precio de venta solo cambia si alguien lo edita manualmente. */
    MANUAL,
    /** Se registra un precio sugerido en historial; no se aplica solo. */
    SUGERIDO,
    /** Recalcula precio de venta con markup sobre el nuevo costo y lo registra en historial. */
    AUTOMATICO_MARKUP
}
