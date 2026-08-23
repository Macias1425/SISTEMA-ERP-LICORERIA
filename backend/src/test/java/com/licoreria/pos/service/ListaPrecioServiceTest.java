package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.repository.ListaPrecioRepository;
import com.licoreria.pos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ListaPrecioServiceTest {

    @Mock
    private ListaPrecioRepository listaPrecioRepository;
    @Mock
    private ProductoRepository productoRepository;

    private ListaPrecioService service;

    @BeforeEach
    void setUp() {
        service = new ListaPrecioService(listaPrecioRepository, productoRepository, new PosProperties(),
                org.mockito.Mockito.mock(AuditoriaService.class));
    }

    @Test
    void mayoristaSinVolumenMinimoCaeADetal() {
        assertEquals(TipoCliente.DETAL, service.resolverTipoAplicado(TipoCliente.MAYORISTA, 5));
    }

    @Test
    void mayoristaConVolumenAplicaTarifaMayorista() {
        assertEquals(TipoCliente.MAYORISTA, service.resolverTipoAplicado(TipoCliente.MAYORISTA, 12));
    }
}
