package com.licoreria.pos.security;

import com.licoreria.pos.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaPasswordTest {

    private final PoliticaPassword politica = new PoliticaPassword();

    @Test
    void aceptaPasswordConLetrasYNumeros() {
        assertDoesNotThrow(() -> politica.validar("Clave1234", "cajero"));
    }

    @Test
    void rechazaPasswordCorta() {
        assertThrows(ReglaNegocioException.class, () -> politica.validar("Ab1", "cajero"));
    }

    @Test
    void rechazaPasswordIgualAlUsuario() {
        assertThrows(ReglaNegocioException.class, () -> politica.validar("cajero12", "cajero12"));
    }
}
