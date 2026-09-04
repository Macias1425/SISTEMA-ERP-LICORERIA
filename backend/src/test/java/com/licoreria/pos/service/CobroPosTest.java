package com.licoreria.pos.service;

import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.FormaPago;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CobroPosTest {

    @Test
    void efectivoCalculaVuelto() {
        BigDecimal vuelto = CobroPos.vuelto(FormaPago.EFECTIVO, new BigDecimal("300.00"), new BigDecimal("287.50"));
        assertEquals(new BigDecimal("12.50"), vuelto);
        assertEquals(new BigDecimal("300.00"),
                CobroPos.montoRegistrado(FormaPago.EFECTIVO, new BigDecimal("300.00"), new BigDecimal("287.50")));
    }

    @Test
    void efectivoInsuficienteSeRechaza() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> CobroPos.vuelto(FormaPago.EFECTIVO, new BigDecimal("100.00"), new BigDecimal("287.50")));
        assertEquals("PAGO_INSUFICIENTE", error.getCodigo());
    }

    @Test
    void efectivoSinMontoSeRechaza() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> CobroPos.vuelto(FormaPago.EFECTIVO, null, new BigDecimal("50.00")));
        assertEquals("PAGO_INSUFICIENTE", error.getCodigo());
    }

    @Test
    void tarjetaNoDaVueltoYRegistraElTotal() {
        BigDecimal total = new BigDecimal("287.50");
        assertEquals(new BigDecimal("0.00"), CobroPos.vuelto(FormaPago.TARJETA, new BigDecimal("500.00"), total));
        assertEquals(new BigDecimal("287.50"), CobroPos.montoRegistrado(FormaPago.TARJETA, new BigDecimal("500.00"), total));
    }
}
