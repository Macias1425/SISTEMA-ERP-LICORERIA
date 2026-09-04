package com.licoreria.pos.repository;

import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.ProductoDTO;
import com.licoreria.pos.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ProductoListarIntegrationTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoService productoService;

    @Test
    void buscarPaginadoEnMysql() {
        assertNotNull(productoRepository.buscarPaginado(
                null, null, true, null, null, PageRequest.of(0, 20)
        ));
    }

    @Test
    void listarServicioEnMysql() {
        PaginaDTO<ProductoDTO> pagina = productoService.listar(null, null, true, null, null, 0, 20);
        assertNotNull(pagina);
        assertNotNull(pagina.getContenido());
    }
}
