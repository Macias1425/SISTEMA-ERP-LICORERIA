package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.TipoVerificacionEdad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MayoriaEdadServiceTest {

    private MayoriaEdadService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 8, 19, 12, 0).atZone(ZoneId.of("America/Tegucigalpa")).toInstant(),
                ZoneId.of("America/Tegucigalpa")
        );
        service = new MayoriaEdadService(new PosProperties(), clock);
    }

    @Test
    void noExigeVerificacionSiNoHayAlcohol() {
        assertNull(service.validar(false, null, false));
    }

    @Test
    void aceptaFechaDeNacimientoMayorDeEdad() {
        assertEquals(
                TipoVerificacionEdad.FECHA_NACIMIENTO,
                service.validar(true, LocalDate.of(2000, 1, 15), false)
        );
    }

    @Test
    void bloqueaMenorDeEdad() {
        assertThrows(ReglaNegocioException.class,
                () -> service.validar(true, LocalDate.of(2012, 5, 1), true));
    }

    @Test
    void aceptaConfirmacionDelCajero() {
        assertEquals(
                TipoVerificacionEdad.CONFIRMACION_CAJERO,
                service.validar(true, null, true)
        );
    }

    @Test
    void exigeVerificacionSiHayAlcohol() {
        assertThrows(ReglaNegocioException.class, () -> service.validar(true, null, false));
    }
}
