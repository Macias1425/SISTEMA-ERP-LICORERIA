package com.licoreria.pos.service;

import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Auditoria;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarcasPreciosServiceTest {

    @Test
    void parseParesExtraeCompraYVenta() {
        Map<String, String> pares = MarcasPreciosService.parsePares("compra=200.00, venta=250.00");
        assertEquals("200.00", pares.get("compra"));
        assertEquals("250.00", pares.get("venta"));
    }

    @Test
    void normalizarMarcaSinValor() {
        assertEquals("Sin marca", MarcasPreciosService.normalizarMarca(null));
        assertEquals("Bacardi", MarcasPreciosService.normalizarMarca("  Bacardi  "));
    }

    @Test
    void mapearHistorialCatalogo() {
        Auditoria evento = Auditoria.builder()
                .id(1L)
                .accion(AccionAuditoria.CAMBIO_PRECIO)
                .entidad("Producto")
                .entidadId(10L)
                .valorAnterior("compra=100.00, venta=150.00")
                .valorNuevo("compra=110.00, venta=165.00")
                .detalle("Actualización de catálogo P001")
                .fechaHora(LocalDateTime.now())
                .build();

        var dto = MarcasPreciosService.mapearHistorial(evento, Map.of(), Map.of());
        assertEquals("COMPRA_VENTA", dto.getTipoCambio());
        assertEquals(new BigDecimal("10.00"), dto.getVariacionPct());
    }

    @Test
    void porcentajeMargenCeroSiTotalEsCero() {
        assertEquals(BigDecimal.ZERO, MarcasPreciosService.porcentaje(BigDecimal.TEN, BigDecimal.ZERO));
    }
}
