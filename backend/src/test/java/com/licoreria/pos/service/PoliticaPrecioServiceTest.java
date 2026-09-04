package com.licoreria.pos.service;



import com.licoreria.pos.model.AccionAuditoria;

import com.licoreria.pos.model.DecisionPoliticaPrecio;

import com.licoreria.pos.model.PoliticaPrecio;

import com.licoreria.pos.model.Producto;

import com.licoreria.pos.model.Usuario;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;



import java.math.BigDecimal;



import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;



@ExtendWith(MockitoExtension.class)

class PoliticaPrecioServiceTest {



    @Mock

    private AuditoriaService auditoriaService;



    private PoliticaPrecioService politicaPrecioService;



    @BeforeEach

    void setUp() {

        politicaPrecioService = new PoliticaPrecioService(auditoriaService);

    }



    @Test

    void manualNoCambiaVenta() {

        Producto producto = Producto.builder()

                .id(1L)

                .precioCompra(new BigDecimal("100.00"))

                .precioVenta(new BigDecimal("125.00"))

                .politicaPrecio(PoliticaPrecio.MANUAL)

                .build();

        Usuario operador = Usuario.builder().id(2L).build();



        ResultadoPoliticaPrecio resultado = politicaPrecioService.aplicarTrasCompra(

                producto,

                new BigDecimal("100.00"),

                new BigDecimal("110.00"),

                operador,

                "C-001"

        );



        assertEquals(DecisionPoliticaPrecio.SIN_CAMBIO, resultado.decision());

        assertFalse(resultado.ventaAplicada());

        assertEquals(new BigDecimal("125.00"), producto.getPrecioVenta());

        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());

    }



    @Test

    void sugeridoRegistraHistorialSinAplicarVenta() {

        Producto producto = Producto.builder()

                .id(1L)

                .precioCompra(new BigDecimal("100.00"))

                .precioVenta(new BigDecimal("125.00"))

                .politicaPrecio(PoliticaPrecio.SUGERIDO)

                .margenObjetivoPct(new BigDecimal("20"))

                .build();

        Usuario operador = Usuario.builder().id(2L).build();



        ResultadoPoliticaPrecio resultado = politicaPrecioService.aplicarTrasCompra(

                producto,

                new BigDecimal("100.00"),

                new BigDecimal("120.00"),

                operador,

                "C-002"

        );



        assertEquals(DecisionPoliticaPrecio.SOLO_SUGERIDO, resultado.decision());

        assertFalse(resultado.ventaAplicada());

        assertEquals(new BigDecimal("125.00"), producto.getPrecioVenta());

        verify(auditoriaService).registrar(

                eq(operador),

                eq(AccionAuditoria.CAMBIO_PRECIO),

                eq("Producto"),

                eq(1L),

                any(),

                any(),

                org.mockito.ArgumentMatchers.contains("SUGERIDO")

        );

    }



    @Test

    void automaticoMarkupAplicaVentaYRegistraHistorial() {

        Producto producto = Producto.builder()

                .id(1L)

                .precioCompra(new BigDecimal("100.00"))

                .precioVenta(new BigDecimal("125.00"))

                .politicaPrecio(PoliticaPrecio.AUTOMATICO_MARKUP)

                .margenObjetivoPct(new BigDecimal("20"))

                .build();

        Usuario operador = Usuario.builder().id(2L).build();



        ResultadoPoliticaPrecio resultado = politicaPrecioService.aplicarTrasCompra(

                producto,

                new BigDecimal("100.00"),

                new BigDecimal("120.00"),

                operador,

                "C-003"

        );



        assertEquals(DecisionPoliticaPrecio.APLICADA_AUTO, resultado.decision());

        assertTrue(resultado.ventaAplicada());

        assertEquals(new BigDecimal("150.00"), resultado.ventaNueva());

        assertEquals(new BigDecimal("150.00"), producto.getPrecioVenta());

        verify(auditoriaService).registrar(

                eq(operador),

                eq(AccionAuditoria.CAMBIO_PRECIO),

                eq("Producto"),

                eq(1L),

                any(),

                any(),

                eq("Markup automático por compra C-003 (margen 20%)")

        );

    }



    @Test

    void sugeridoSinCambioDeCostoNoRegistraHistorial() {

        Producto producto = Producto.builder()

                .id(1L)

                .precioCompra(new BigDecimal("100.00"))

                .precioVenta(new BigDecimal("125.00"))

                .politicaPrecio(PoliticaPrecio.SUGERIDO)

                .margenObjetivoPct(new BigDecimal("20"))

                .build();

        Usuario operador = Usuario.builder().id(2L).build();



        politicaPrecioService.aplicarTrasCompra(

                producto,

                new BigDecimal("100.00"),

                new BigDecimal("100.00"),

                operador,

                "C-002B"

        );



        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());

    }



    @Test

    void automaticoSinCambioDeCostoNoModificaVenta() {

        Producto producto = Producto.builder()

                .id(1L)

                .precioCompra(new BigDecimal("100.00"))

                .precioVenta(new BigDecimal("125.00"))

                .politicaPrecio(PoliticaPrecio.AUTOMATICO_MARKUP)

                .margenObjetivoPct(new BigDecimal("20"))

                .build();

        Usuario operador = Usuario.builder().id(2L).build();



        politicaPrecioService.aplicarTrasCompra(

                producto,

                new BigDecimal("100.00"),

                new BigDecimal("100.00"),

                operador,

                "C-003B"

        );



        assertEquals(new BigDecimal("125.00"), producto.getPrecioVenta());

        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());

    }



    @Test

    void automaticoCorrigeVentaAunqueCostoNoCambie() {

        Producto producto = Producto.builder()

                .id(1L)

                .precioCompra(new BigDecimal("100.00"))

                .precioVenta(new BigDecimal("10.00"))

                .politicaPrecio(PoliticaPrecio.AUTOMATICO_MARKUP)

                .margenObjetivoPct(new BigDecimal("20"))

                .build();

        Usuario operador = Usuario.builder().id(2L).build();



        ResultadoPoliticaPrecio resultado = politicaPrecioService.aplicarTrasCompra(

                producto,

                new BigDecimal("100.00"),

                new BigDecimal("100.00"),

                operador,

                "C-003C"

        );



        assertTrue(resultado.ventaAplicada());

        assertEquals(new BigDecimal("125.00"), resultado.ventaNueva());

        assertEquals(new BigDecimal("125.00"), producto.getPrecioVenta());

    }



    @Test

    void aplicarEnCatalogoSincronizaVenta() {

        Producto producto = Producto.builder()

                .precioCompra(new BigDecimal("8.00"))

                .precioVenta(new BigDecimal("9.00"))

                .politicaPrecio(PoliticaPrecio.AUTOMATICO_MARKUP)

                .margenObjetivoPct(new BigDecimal("20"))

                .build();



        assertEquals(new BigDecimal("10.00"), politicaPrecioService.aplicarEnCatalogo(producto));

        assertEquals(new BigDecimal("10.00"), producto.getPrecioVenta());

    }



    @Test

    void precioVentaDesdeMargenRedondeaHaciaArriba() {

        assertEquals(new BigDecimal("12.50"), politicaPrecioService.precioVentaDesdeMargen(

                new BigDecimal("10.00"),

                new BigDecimal("20")

        ));

        assertEquals(new BigDecimal("14.93"), politicaPrecioService.precioVentaDesdeMargen(

                new BigDecimal("10.00"),

                new BigDecimal("33")

        ));

    }

}

