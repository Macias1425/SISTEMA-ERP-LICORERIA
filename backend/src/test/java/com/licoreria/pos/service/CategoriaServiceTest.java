package com.licoreria.pos.service;

import com.licoreria.pos.dto.CategoriaDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Categoria;
import com.licoreria.pos.repository.CategoriaRepository;
import com.licoreria.pos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private AccesoService accesoService;

    private CategoriaService categoriaService;

    @BeforeEach
    void setUp() {
        categoriaService = new CategoriaService(categoriaRepository, productoRepository, accesoService);
    }

    @Test
    void creaCategoria() {
        when(categoriaRepository.existsByNombreIgnoreCase("Cervezas")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocation -> {
            Categoria categoria = invocation.getArgument(0);
            categoria.setId(1L);
            return categoria;
        });

        var creada = categoriaService.crear(CategoriaDTO.builder().nombre("Cervezas").activo(true).build());
        assertEquals("Cervezas", creada.getNombre());
        assertEquals(1L, creada.getId());
    }

    @Test
    void noCreaCategoriaDuplicada() {
        when(categoriaRepository.existsByNombreIgnoreCase("Cervezas")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> categoriaService.crear(
                CategoriaDTO.builder().nombre("Cervezas").activo(true).build()));
    }

    @Test
    void noDesactivaCategoriaConProductos() {
        Categoria categoria = Categoria.builder().id(3L).nombre("Licores").activo(true).build();
        when(categoriaRepository.findById(3L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Licores", 3L)).thenReturn(false);
        when(productoRepository.countByCategoriaId(3L)).thenReturn(2L);

        assertThrows(ReglaNegocioException.class, () -> categoriaService.actualizar(3L,
                CategoriaDTO.builder().nombre("Licores").activo(false).build()));
    }
}
