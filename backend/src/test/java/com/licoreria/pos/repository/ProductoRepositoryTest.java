package com.licoreria.pos.repository;

import com.licoreria.pos.model.Producto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void buscarPaginadoSinFiltros() {
        productoRepository.save(Producto.builder()
                .codigo("TEST-001")
                .nombre("Producto prueba")
                .precioCompra(new BigDecimal("10.00"))
                .precioVenta(new BigDecimal("15.00"))
                .stockActual(5)
                .stockMinimo(2)
                .stockCritico(1)
                .activo(true)
                .build());

        Page<Producto> page = productoRepository.buscarPaginado(
                null, null, null, null, null, PageRequest.of(0, 20)
        );

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
    }
}
