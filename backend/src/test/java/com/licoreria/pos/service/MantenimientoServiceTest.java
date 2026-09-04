package com.licoreria.pos.service;

import com.licoreria.pos.dto.AlertaMantenimientoDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MantenimientoServiceTest {

    @Test
    void parsearJdbcUrlExtraeHostPuertoYBase() {
        var conexion = MantenimientoService.parsearJdbcUrl(
                "jdbc:mysql://localhost:3306/pos_licoreria?useSSL=false");
        assertEquals("localhost", conexion.host());
        assertEquals("3306", conexion.port());
        assertEquals("pos_licoreria", conexion.database());
    }

    @Test
    void parsearJdbcUrlSinPuerto() {
        var conexion = MantenimientoService.parsearJdbcUrl("jdbc:mysql://127.0.0.1/pos_licoreria");
        assertEquals("127.0.0.1", conexion.host());
        assertEquals(null, conexion.port());
        assertEquals("pos_licoreria", conexion.database());
    }

    @Test
    void formatearTamanoUsaUnidadesLegibles() {
        assertEquals("512 B", MantenimientoService.formatearTamano(512));
        assertEquals("1.0 KB", MantenimientoService.formatearTamano(1024));
        assertEquals("2.00 MB", MantenimientoService.formatearTamano(2 * 1024 * 1024));
        assertEquals("1.50 GB", MantenimientoService.formatearTamano((long) (1.5 * 1024 * 1024 * 1024)));
    }

    @Test
    void parsearJdbcUrlInvalidaLanzaExcepcion() {
        assertThrows(com.licoreria.pos.exception.ReglaNegocioException.class,
                () -> MantenimientoService.parsearJdbcUrl("jdbc:h2:mem:test"));
    }

    @Test
    void evaluarEstadoRespaldoSegunAntiguedad() {
        LocalDateTime ahora = LocalDateTime.of(2026, 8, 31, 12, 0);
        assertEquals("CRITICO", MantenimientoService.evaluarEstadoRespaldo(null, ahora));
        assertEquals("OK", MantenimientoService.evaluarEstadoRespaldo(ahora.minusHours(6), ahora));
        assertEquals("ADVERTENCIA", MantenimientoService.evaluarEstadoRespaldo(ahora.minusHours(30), ahora));
        assertEquals("CRITICO", MantenimientoService.evaluarEstadoRespaldo(ahora.minusDays(8), ahora));
    }

    @Test
    void clasificarTablaPorFragmentacion() {
        assertEquals("OK", MantenimientoService.clasificarEstadoTabla(10, 0.2));
        assertEquals("ADVERTENCIA", MantenimientoService.clasificarEstadoTabla(10, 4));
        assertEquals(40.0, MantenimientoService.clasificarFragmentacion(10, 4), 0.01);
    }

    @Test
    void peorEstadoTomaElMasGrave() {
        assertEquals("OK", MantenimientoService.peorEstado("OK", "OK"));
        assertEquals("ADVERTENCIA", MantenimientoService.peorEstado("OK", "ADVERTENCIA"));
        assertEquals("CRITICO", MantenimientoService.peorEstado("ADVERTENCIA", "CRITICO", "OK"));
    }

    @Test
    void nombreRespaldoRechazaPathTraversal() {
        assertTrue(MantenimientoService.esNombreRespaldoValido("pos_licoreria_20260831_110500.sql"));
        assertFalse(MantenimientoService.esNombreRespaldoValido("../secret.sql"));
        assertFalse(MantenimientoService.esNombreRespaldoValido("pos_licoreria_20260831_110500.sql.bak"));
        assertFalse(MantenimientoService.esNombreRespaldoValido("otro.sql"));
    }

    @Test
    void pareceSqlDeRespaldoDetectaEncabezado() {
        assertTrue(MantenimientoService.pareceSqlDeRespaldo("-- POS Licorería · Respaldo SQL\nCREATE TABLE x (id int);"));
        assertTrue(MantenimientoService.pareceSqlDeRespaldo("-- MySQL dump\nINSERT INTO t VALUES (1);"));
        assertFalse(MantenimientoService.pareceSqlDeRespaldo("hello world"));
        assertFalse(MantenimientoService.pareceSqlDeRespaldo(""));
    }

    @Test
    void citarIdentificadorRechazaInyeccion() {
        assertEquals("`ventas`", MantenimientoService.citarIdentificador("ventas"));
        assertThrows(com.licoreria.pos.exception.ReglaNegocioException.class,
                () -> MantenimientoService.citarIdentificador("ventas`; DROP TABLE"));
    }

    @Test
    void construirAlertasSinRespaldoEsCritico() {
        List<AlertaMantenimientoDTO> alertas = MantenimientoService.construirAlertas(
                "CRITICO", null, 0, 0, true, true, 5_000_000_000L, true, 0, 30);
        assertTrue(alertas.stream().anyMatch(a -> "SIN_RESPALDO".equals(a.getCodigo())));
        assertTrue(alertas.stream().anyMatch(a -> "critico".equals(a.getNivel())));
    }

    @Test
    void construirAlertasOkCuandoTodoEstaBien() {
        List<AlertaMantenimientoDTO> alertas = MantenimientoService.construirAlertas(
                "OK", 2L, 0, 0, true, true, 5_000_000_000L, true, 3, 30);
        assertEquals(1, alertas.size());
        assertEquals("OK", alertas.get(0).getCodigo());
    }
}
