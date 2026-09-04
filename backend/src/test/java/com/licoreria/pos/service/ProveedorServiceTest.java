package com.licoreria.pos.service;

import com.licoreria.pos.dto.ProveedorDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Proveedor;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.PrecioProveedorRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private PrecioProveedorRepository precioProveedorRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private AccesoService accesoService;

    private ProveedorService proveedorService;

    @BeforeEach
    void setUp() {
        proveedorService = new ProveedorService(
                proveedorRepository,
                precioProveedorRepository,
                productoRepository,
                inventarioService,
                accesoService
        );
    }

    @Test
    void rechazaProveedorDuplicado() {
        Usuario admin = Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build();
        when(accesoService.exigirPermiso(Permiso.PROVEEDORES_GESTIONAR)).thenReturn(admin);
        when(proveedorRepository.existsByNombreIgnoreCase("Distribuidora Norte")).thenReturn(true);

        ProveedorDTO dto = ProveedorDTO.builder().nombre("Distribuidora Norte").build();

        assertThrows(ReglaNegocioException.class, () -> proveedorService.crear(dto));
        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }

    @Test
    void exigeProveedorActivo() {
        Proveedor inactivo = Proveedor.builder().id(2L).nombre("Cerrado").activo(false).build();
        when(proveedorRepository.findById(2L)).thenReturn(java.util.Optional.of(inactivo));

        assertThrows(ReglaNegocioException.class, () -> proveedorService.exigirActivo(2L));
    }

    @Test
    void creaProveedorActivo() {
        Usuario admin = Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build();
        when(accesoService.exigirPermiso(Permiso.PROVEEDORES_GESTIONAR)).thenReturn(admin);
        when(proveedorRepository.existsByNombreIgnoreCase("La Bodega SA")).thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(invocation -> {
            Proveedor proveedor = invocation.getArgument(0);
            proveedor.setId(9L);
            return proveedor;
        });
        when(precioProveedorRepository.countByProveedorId(9L)).thenReturn(0L);

        ProveedorDTO creado = proveedorService.crear(ProveedorDTO.builder()
                .nombre("La Bodega SA")
                .documento("J0310000123456")
                .build());

        assertEquals("La Bodega SA", creado.getNombre());
        assertEquals(true, creado.getActivo());
    }
}
