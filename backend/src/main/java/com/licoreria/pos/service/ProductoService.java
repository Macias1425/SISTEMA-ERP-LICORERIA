package com.licoreria.pos.service;

import com.licoreria.pos.dto.MovimientoInventarioDTO;
import com.licoreria.pos.dto.PresentacionDTO;
import com.licoreria.pos.dto.ProductoDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final AuditoriaService auditoriaService;
    private final AutorizacionService autorizacionService;

    @Transactional(readOnly = true)
    public List<ProductoDTO> listar() {
        return productoRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ProductoDTO obtenerPorId(Long id) {
        return toDto(buscar(id));
    }

    @Transactional
    public ProductoDTO crear(ProductoDTO dto) {
        autorizacionService.exigirRol(Rol.ALMACENISTA, Rol.ADMIN);
        productoRepository.findByCodigo(dto.getCodigo()).ifPresent(existente -> {
            throw new ReglaNegocioException("CODIGO_DUPLICADO", "Ya existe un producto con código " + dto.getCodigo());
        });
        validarUmbrales(dto);

        Producto producto = Producto.builder()
                .codigo(dto.getCodigo())
                .nombre(dto.getNombre())
                .marca(dto.getMarca())
                .categoriaId(dto.getCategoriaId())
                .unidadMinima(dto.getUnidadMinima() == null || dto.getUnidadMinima().isBlank() ? "BOTELLA" : dto.getUnidadMinima())
                .precioCompra(dto.getPrecioCompra())
                .precioVenta(dto.getPrecioVenta())
                .stockActual(0)
                .stockMinimo(dto.getStockMinimo() == null ? 0 : dto.getStockMinimo())
                .stockCritico(dto.getStockCritico() == null ? 0 : dto.getStockCritico())
                .esAlcoholico(dto.getEsAlcoholico() == null || dto.getEsAlcoholico())
                .activo(dto.getActivo() == null || dto.getActivo())
                .build();

        List<PresentacionDTO> presentaciones = dto.getPresentaciones() == null || dto.getPresentaciones().isEmpty()
                ? List.of(PresentacionDTO.builder().nombre("Botella").factorAUnidadMinima(1).activo(true).build())
                : dto.getPresentaciones();

        for (PresentacionDTO presentacionDTO : presentaciones) {
            agregarPresentacionInterna(producto, presentacionDTO);
        }
        asegurarUnidadMinima(producto);
        Producto guardado = productoRepository.save(producto);

        int stockInicial = dto.getStockActual() == null ? 0 : dto.getStockActual();
        if (stockInicial > 0) {
            Presentacion umm = presentacionUnidadMinima(guardado);
            inventarioService.registrarMovimiento(MovimientoInventarioDTO.builder()
                    .productoId(guardado.getId())
                    .presentacionId(umm.getId())
                    .tipo(TipoMovimiento.ENTRADA)
                    .cantidad(stockInicial)
                    .incremento(true)
                    .motivo("Stock inicial")
                    .build());
            guardado = buscar(guardado.getId());
        }

        return toDto(guardado);
    }

    @Transactional
    public ProductoDTO actualizar(Long id, ProductoDTO dto) {
        autorizacionService.exigirRol(Rol.ALMACENISTA, Rol.ADMIN);
        Producto existente = buscar(id);
        validarUmbrales(dto);

        existente.setCodigo(dto.getCodigo());
        existente.setNombre(dto.getNombre());
        existente.setMarca(dto.getMarca());
        existente.setCategoriaId(dto.getCategoriaId());
        if (dto.getUnidadMinima() != null && !dto.getUnidadMinima().isBlank()) {
            existente.setUnidadMinima(dto.getUnidadMinima());
        }
        existente.setPrecioCompra(dto.getPrecioCompra());
        existente.setPrecioVenta(dto.getPrecioVenta());
        existente.setStockMinimo(dto.getStockMinimo() == null ? 0 : dto.getStockMinimo());
        existente.setStockCritico(dto.getStockCritico() == null ? 0 : dto.getStockCritico());
        if (dto.getEsAlcoholico() != null) {
            existente.setEsAlcoholico(dto.getEsAlcoholico());
        }
        if (dto.getActivo() != null) {
            existente.setActivo(dto.getActivo());
        }
        return toDto(productoRepository.save(existente));
    }

    @Transactional
    public PresentacionDTO agregarPresentacion(Long productoId, PresentacionDTO dto) {
        autorizacionService.exigirRol(Rol.ALMACENISTA, Rol.ADMIN);
        Producto producto = buscar(productoId);
        Presentacion presentacion = agregarPresentacionInterna(producto, dto);
        productoRepository.save(producto);
        return toPresentacionDto(presentacion);
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario operador = autorizacionService.exigirRol(Rol.ADMIN);
        Producto producto = buscar(id);
        if (producto.getStockActual() != null && producto.getStockActual() > 0) {
            throw new ReglaNegocioException("PRODUCTO_CON_STOCK", "No se puede eliminar un producto con existencia");
        }
        String codigo = producto.getCodigo();
        productoRepository.delete(producto);
        auditoriaService.registrar(operador, AccionAuditoria.BORRADO_PRODUCTO, "Producto", id,
                codigo, null, "Eliminación de producto " + codigo);
    }

    private Presentacion agregarPresentacionInterna(Producto producto, PresentacionDTO dto) {
        boolean duplicada = producto.getPresentaciones().stream()
                .anyMatch(actual -> actual.getNombre().equalsIgnoreCase(dto.getNombre()));
        if (duplicada) {
            throw new ReglaNegocioException("PRESENTACION_DUPLICADA", "Ya existe la presentación " + dto.getNombre());
        }
        Presentacion presentacion = Presentacion.builder()
                .producto(producto)
                .nombre(dto.getNombre())
                .factorAUnidadMinima(dto.getFactorAUnidadMinima())
                .activo(dto.getActivo() == null || dto.getActivo())
                .build();
        producto.getPresentaciones().add(presentacion);
        return presentacion;
    }

    private void asegurarUnidadMinima(Producto producto) {
        boolean tieneUmm = producto.getPresentaciones().stream()
                .anyMatch(presentacion -> presentacion.getFactorAUnidadMinima() != null
                        && presentacion.getFactorAUnidadMinima() == 1);
        if (!tieneUmm) {
            throw new ReglaNegocioException(
                    "FALTA_UNIDAD_MINIMA",
                    "El producto debe tener al menos una presentación con factor 1 (unidad mínima)"
            );
        }
    }

    private Presentacion presentacionUnidadMinima(Producto producto) {
        return producto.getPresentaciones().stream()
                .filter(presentacion -> presentacion.getFactorAUnidadMinima() == 1)
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("FALTA_UNIDAD_MINIMA", "No hay presentación de unidad mínima"));
    }

    private void validarUmbrales(ProductoDTO dto) {
        int minimo = dto.getStockMinimo() == null ? 0 : dto.getStockMinimo();
        int critico = dto.getStockCritico() == null ? 0 : dto.getStockCritico();
        if (critico > minimo && minimo > 0) {
            throw new ReglaNegocioException(
                    "UMBRAL_INVALIDO",
                    "El stock crítico no puede ser mayor que el stock mínimo"
            );
        }
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + id));
    }

    private ProductoDTO toDto(Producto producto) {
        List<PresentacionDTO> presentaciones = producto.getPresentaciones() == null
                ? new ArrayList<>()
                : producto.getPresentaciones().stream().map(this::toPresentacionDto).toList();

        return ProductoDTO.builder()
                .id(producto.getId())
                .codigo(producto.getCodigo())
                .nombre(producto.getNombre())
                .marca(producto.getMarca())
                .categoriaId(producto.getCategoriaId())
                .unidadMinima(producto.getUnidadMinima())
                .precioCompra(producto.getPrecioCompra())
                .precioVenta(producto.getPrecioVenta())
                .stockActual(producto.getStockActual())
                .stockMinimo(producto.getStockMinimo())
                .stockCritico(producto.getStockCritico())
                .esAlcoholico(producto.getEsAlcoholico())
                .activo(producto.getActivo())
                .nivelAlerta(inventarioService.calcularNivelAlerta(producto))
                .presentaciones(presentaciones)
                .build();
    }

    private PresentacionDTO toPresentacionDto(Presentacion presentacion) {
        return PresentacionDTO.builder()
                .id(presentacion.getId())
                .nombre(presentacion.getNombre())
                .factorAUnidadMinima(presentacion.getFactorAUnidadMinima())
                .activo(presentacion.getActivo())
                .build();
    }
}
