package com.licoreria.pos.service;

import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Presentacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversionUnidadesTest {

    private final ConversionUnidades conversion = new ConversionUnidades();

    @Test
    void cajaDeDocePorDosUnidadesSonVeinticuatroBotellas() {
        Presentacion caja = Presentacion.builder()
                .nombre("Caja")
                .factorAUnidadMinima(12)
                .activo(true)
                .build();

        assertEquals(24, conversion.aUnidadMinima(caja, 2));
    }

    @Test
    void sixPackEsSeisBotellas() {
        Presentacion sixPack = Presentacion.builder()
                .nombre("Six-pack")
                .factorAUnidadMinima(6)
                .activo(true)
                .build();

        assertEquals(6, conversion.aUnidadMinima(sixPack, 1));
    }

    @Test
    void rechazaCantidadCero() {
        Presentacion botella = Presentacion.builder()
                .nombre("Botella")
                .factorAUnidadMinima(1)
                .activo(true)
                .build();

        assertThrows(ReglaNegocioException.class, () -> conversion.aUnidadMinima(botella, 0));
    }
}
