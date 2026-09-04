package com.licoreria.pos.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinanzasServiceTest {

    @Test
    void variacionPctCalculaCrecimiento() {
        assertEquals(new BigDecimal("25.00"), FinanzasService.variacionPct(new BigDecimal("125"), new BigDecimal("100")));
        assertEquals(new BigDecimal("0"), FinanzasService.variacionPct(BigDecimal.ZERO, BigDecimal.ZERO));
        assertEquals(new BigDecimal("100"), FinanzasService.variacionPct(new BigDecimal("50"), BigDecimal.ZERO));
    }

    @Test
    void porcentajeMargen() {
        assertEquals(new BigDecimal("40.00"), FinanzasService.porcentaje(new BigDecimal("40"), new BigDecimal("100")));
        assertEquals(BigDecimal.ZERO, FinanzasService.porcentaje(new BigDecimal("10"), BigDecimal.ZERO));
    }
}
