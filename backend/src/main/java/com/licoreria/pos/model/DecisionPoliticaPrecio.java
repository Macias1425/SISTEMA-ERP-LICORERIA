package com.licoreria.pos.model;

/** Decisión emitida al evaluar la política de precio tras una recepción. */
public enum DecisionPoliticaPrecio {
    APLICADA_AUTO,
    SOLO_SUGERIDO,
    APLICADA_MANUAL,
    SIN_CAMBIO
}
