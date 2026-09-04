package com.licoreria.pos.service;

import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Presentacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversionUnidadesTest {

    private final ConversionUnidades conversion = new ConversionUnidades();

    @Test
    void convierteCajaABotellas() {
        Presentacion caja = Presentacion.builder()
                .nombre("Caja")
                .factorAUnidadMinima(12)
                .activo(true)
                .build();
        assertEquals(24, conversion.aUnidadMinima(caja, 2));
    }

    @Test
    void rechazaCantidadCero() {
        Presentacion botella = Presentacion.builder()
                .nombre("Botella")
                .factorAUnidadMinima(1)
                .activo(true)
                .build();
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> conversion.aUnidadMinima(botella, 0));
        assertEquals("CANTIDAD_INVALIDA", error.getCodigo());
    }

    @Test
    void rechazaPresentacionInactiva() {
        Presentacion inactiva = Presentacion.builder()
                .nombre("Six pack")
                .factorAUnidadMinima(6)
                .activo(false)
                .build();
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> conversion.aUnidadMinima(inactiva, 1));
        assertEquals("PRESENTACION_INACTIVA", error.getCodigo());
    }
}
