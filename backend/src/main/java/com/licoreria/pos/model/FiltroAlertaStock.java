package com.licoreria.pos.model;

/**
 * Filtro de existencias para el catálogo. Es distinto de {@link NivelAlerta} porque la
 * operación pide "lo que hay que reponer" en una sola vista: ALERTA agrupa crítico y mínimo.
 */
public enum FiltroAlertaStock {
    CRITICO,
    MINIMO,
    /** Crítico o mínimo: todo lo que exige reposición. */
    ALERTA,
    OK
}
