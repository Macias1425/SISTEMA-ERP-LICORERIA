package com.licoreria.pos.service;

import com.licoreria.pos.dto.CategoriaDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Categoria;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.repository.CategoriaRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final AccesoService accesoService;

    @Transactional(readOnly = true)
    public PaginaDTO<CategoriaDTO> listar(String busqueda, Boolean activo, int pagina, int tamano) {
        String termino = busqueda == null || busqueda.isBlank() ? null : busqueda.trim();
        return PaginaDTO.de(categoriaRepository.buscarPaginado(
                termino, activo, PaginacionUtil.pageable(pagina, tamano)
        ).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listar() {
        return listar(null, null, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public CategoriaDTO obtener(Long id) {
        return toDto(buscar(id));
    }

    @Transactional
    public CategoriaDTO crear(CategoriaDTO dto) {
        accesoService.exigirPermiso(Permiso.CATEGORIAS_GESTIONAR);
        String nombre = dto.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException("CATEGORIA_DUPLICADA", "Ya existe la categoría " + nombre);
        }
        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .descripcion(texto(dto.getDescripcion()))
                .activo(dto.getActivo() == null || dto.getActivo())
                .build();
        return toDto(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaDTO actualizar(Long id, CategoriaDTO dto) {
        accesoService.exigirPermiso(Permiso.CATEGORIAS_GESTIONAR);
        Categoria categoria = buscar(id);
        String nombre = dto.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ReglaNegocioException("CATEGORIA_DUPLICADA", "Ya existe la categoría " + nombre);
        }
        if (Boolean.FALSE.equals(dto.getActivo()) && productoRepository.countByCategoriaId(id) > 0
                && Boolean.TRUE.equals(categoria.getActivo())) {
            throw new ReglaNegocioException(
                    "CATEGORIA_EN_USO",
                    "No se puede desactivar una categoría con productos asignados"
            );
        }
        categoria.setNombre(nombre);
        categoria.setDescripcion(texto(dto.getDescripcion()));
        categoria.setActivo(dto.getActivo() == null || dto.getActivo());
        return toDto(categoriaRepository.save(categoria));
    }

    public Categoria exigirActiva(Long categoriaId) {
        Categoria categoria = buscar(categoriaId);
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new ReglaNegocioException("CATEGORIA_INACTIVA", "La categoría no está activa");
        }
        return categoria;
    }

    @Transactional(readOnly = true)
    public String nombrePorId(Long id) {
        if (id == null) {
            return null;
        }
        return categoriaRepository.findById(id).map(Categoria::getNombre).orElse(null);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + id));
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private CategoriaDTO toDto(Categoria categoria) {
        return CategoriaDTO.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .activo(categoria.getActivo())
                .build();
    }
}
