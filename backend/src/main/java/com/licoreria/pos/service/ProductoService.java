package com.licoreria.pos.service;

import com.licoreria.pos.dto.MovimientoInventarioDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.PresentacionDTO;
import com.licoreria.pos.dto.ProductoDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.FiltroAlertaStock;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.PoliticaPrecio;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final AuditoriaService auditoriaService;
    private final AutorizacionService autorizacionService;
    private final AccesoService accesoService;
    private final CategoriaService categoriaService;
    private final PoliticaPrecioService politicaPrecioService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PaginaDTO<ProductoDTO> listar(
            String busqueda,
            Long categoriaId,
            Boolean activo,
            Boolean esAlcoholico,
            FiltroAlertaStock nivelAlerta,
            int pagina,
            int tamano
    ) {
        String termino = busqueda == null || busqueda.isBlank() ? null : busqueda.trim();
        String nivelAlertaFiltro = nivelAlerta == null ? null : nivelAlerta.name();
        Page<Producto> page = productoRepository.buscarPaginado(
                termino,
                categoriaId,
                activo,
                esAlcoholico,
                nivelAlertaFiltro,
                PaginacionUtil.pageable(pagina, tamano)
        );
        return PaginaDTO.de(page.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> listar(
            String busqueda,
            Long categoriaId,
            Boolean activo,
            Boolean esAlcoholico,
            FiltroAlertaStock nivelAlerta
    ) {
        return listar(busqueda, categoriaId, activo, esAlcoholico, nivelAlerta, 0, PaginacionUtil.TAMANO_MAX)
                .getContenido();
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> listar() {
        return listar(null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ProductoDTO> listarParaPos(String busqueda, Long categoriaId, int pagina, int tamano) {
        return listar(busqueda, categoriaId, true, null, null, pagina, tamano);
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> listarParaPos() {
        return listarParaPos(null, null, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public ProductoDTO obtenerPorId(Long id) {
        return toDto(buscar(id));
    }

    @Transactional
    public ProductoDTO crear(ProductoDTO dto) {
        Usuario operador = accesoService.exigirPermiso(Permiso.PRODUCTOS_GESTIONAR);
        validarCodigoUnico(dto.getCodigo(), null);
        validarPrecios(dto);
        validarUmbrales(dto);
        validarCategoria(dto.getCategoriaId());
        validarVencimiento(dto.getFechaVencimiento());

        Producto producto = Producto.builder()
                .codigo(dto.getCodigo())
                .nombre(dto.getNombre())
                .marca(dto.getMarca())
                .urlImagen(normalizarUrlImagen(dto.getUrlImagen()))
                .categoriaId(dto.getCategoriaId())
                .unidadMinima(dto.getUnidadMinima() == null || dto.getUnidadMinima().isBlank() ? "BOTELLA" : dto.getUnidadMinima())
                .precioCompra(dto.getPrecioCompra())
                .precioVenta(dto.getPrecioVenta())
                .stockActual(0)
                .stockMinimo(dto.getStockMinimo() == null ? 0 : dto.getStockMinimo())
                .stockCritico(dto.getStockCritico() == null ? 0 : dto.getStockCritico())
                .fechaVencimiento(dto.getFechaVencimiento())
                .esAlcoholico(dto.getEsAlcoholico() == null || dto.getEsAlcoholico())
                .activo(dto.getActivo() == null || dto.getActivo())
                .politicaPrecio(dto.getPoliticaPrecio() == null ? PoliticaPrecio.MANUAL : dto.getPoliticaPrecio())
                .margenObjetivoPct(dto.getMargenObjetivoPct())
                .build();
        politicaPrecioService.validarMargen(dto.getMargenObjetivoPct());

        List<PresentacionDTO> presentaciones = dto.getPresentaciones() == null || dto.getPresentaciones().isEmpty()
                ? List.of(PresentacionDTO.builder().nombre("Botella").factorAUnidadMinima(1).activo(true).build())
                : dto.getPresentaciones();

        for (PresentacionDTO presentacionDTO : presentaciones) {
            agregarPresentacionInterna(producto, presentacionDTO);
        }
        asegurarUnidadMinima(producto);
        politicaPrecioService.aplicarEnCatalogo(producto);
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

        auditoriaService.registrar(operador, AccionAuditoria.CONFIGURACION, "Producto", guardado.getId(),
                null, guardado.getCodigo(), "Alta de producto " + guardado.getNombre());
        return toDto(guardado);
    }

    @Transactional
    public ProductoDTO actualizar(Long id, ProductoDTO dto) {
        Usuario operador = accesoService.exigirPermiso(Permiso.PRODUCTOS_GESTIONAR);
        Producto existente = buscar(id);
        validarCodigoUnico(dto.getCodigo(), id);
        validarPrecios(dto);
        validarUmbrales(dto);
        validarCategoria(dto.getCategoriaId());
        validarVencimiento(dto.getFechaVencimiento(), existente.getFechaVencimiento());
        if (Boolean.FALSE.equals(dto.getActivo()) && Boolean.TRUE.equals(existente.getActivo())
                && existente.getStockActual() != null && existente.getStockActual() > 0) {
            throw new ReglaNegocioException(
                    "PRODUCTO_CON_STOCK",
                    "No se puede desactivar un producto con existencia en bodega"
            );
        }

        existente.setCodigo(dto.getCodigo());
        existente.setNombre(dto.getNombre());
        existente.setMarca(dto.getMarca());
        existente.setUrlImagen(normalizarUrlImagen(dto.getUrlImagen()));
        existente.setCategoriaId(dto.getCategoriaId());
        if (dto.getUnidadMinima() != null && !dto.getUnidadMinima().isBlank()) {
            existente.setUnidadMinima(dto.getUnidadMinima());
        }
        BigDecimal compraAnterior = existente.getPrecioCompra();
        BigDecimal ventaAnterior = existente.getPrecioVenta();
        existente.setPrecioCompra(dto.getPrecioCompra());
        if (dto.getPoliticaPrecio() != null) {
            existente.setPoliticaPrecio(dto.getPoliticaPrecio());
        }
        existente.setMargenObjetivoPct(dto.getMargenObjetivoPct());
        politicaPrecioService.validarMargen(dto.getMargenObjetivoPct());
        if (existente.getPoliticaPrecio() == PoliticaPrecio.AUTOMATICO_MARKUP) {
            politicaPrecioService.aplicarEnCatalogo(existente);
        } else {
            existente.setPrecioVenta(dto.getPrecioVenta());
        }
        if (cambioPrecio(compraAnterior, existente.getPrecioCompra()) || cambioPrecio(ventaAnterior, existente.getPrecioVenta())) {
            auditoriaService.registrar(operador, AccionAuditoria.CAMBIO_PRECIO, "Producto", existente.getId(),
                    "compra=" + compraAnterior + ", venta=" + ventaAnterior,
                    "compra=" + existente.getPrecioCompra() + ", venta=" + existente.getPrecioVenta(),
                    "Actualización de catálogo " + existente.getCodigo());
        }
        existente.setStockMinimo(dto.getStockMinimo() == null ? 0 : dto.getStockMinimo());
        existente.setStockCritico(dto.getStockCritico() == null ? 0 : dto.getStockCritico());
        existente.setFechaVencimiento(dto.getFechaVencimiento());
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
        accesoService.exigirPermiso(Permiso.PRODUCTOS_GESTIONAR);
        Producto producto = buscar(productoId);
        Presentacion presentacion = agregarPresentacionInterna(producto, dto);
        productoRepository.saveAndFlush(producto);
        return toPresentacionDto(presentacion);
    }

    /**
     * Edita nombre, factor y vigencia de una presentación existente. No se elimina nunca:
     * las ventas históricas guardan el id de la presentación y desactivarla la retira del POS
     * sin romper la trazabilidad.
     */
    @Transactional
    public PresentacionDTO actualizarPresentacion(Long productoId, Long presentacionId, PresentacionDTO dto) {
        Usuario operador = accesoService.exigirPermiso(Permiso.PRODUCTOS_GESTIONAR);
        Producto producto = buscar(productoId);
        Presentacion presentacion = producto.getPresentaciones().stream()
                .filter(actual -> actual.getId() != null && actual.getId().equals(presentacionId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La presentación " + presentacionId + " no pertenece al producto " + productoId));

        boolean duplicada = producto.getPresentaciones().stream()
                .anyMatch(otra -> otra != presentacion && otra.getNombre().equalsIgnoreCase(dto.getNombre()));
        if (duplicada) {
            throw new ReglaNegocioException("PRESENTACION_DUPLICADA", "Ya existe la presentación " + dto.getNombre());
        }

        String antes = presentacion.getNombre() + " x" + presentacion.getFactorAUnidadMinima();
        presentacion.setNombre(dto.getNombre());
        presentacion.setFactorAUnidadMinima(dto.getFactorAUnidadMinima());
        presentacion.setActivo(dto.getActivo() == null || dto.getActivo());

        exigirUnidadMinimaVigente(producto);
        productoRepository.save(producto);

        auditoriaService.registrar(operador, AccionAuditoria.CONFIGURACION, "Presentacion", presentacionId,
                antes, dto.getNombre() + " x" + dto.getFactorAUnidadMinima(),
                "Cambio de presentación en " + producto.getCodigo());
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

    /** La unidad mínima es la base de todo el inventario: debe existir y estar vigente. */
    private void exigirUnidadMinimaVigente(Producto producto) {
        boolean vigente = producto.getPresentaciones().stream()
                .anyMatch(presentacion -> presentacion.getFactorAUnidadMinima() != null
                        && presentacion.getFactorAUnidadMinima() == 1
                        && !Boolean.FALSE.equals(presentacion.getActivo()));
        if (!vigente) {
            throw new ReglaNegocioException(
                    "FALTA_UNIDAD_MINIMA",
                    "El producto debe conservar una presentación activa con factor 1 (unidad mínima)"
            );
        }
    }

    private Presentacion presentacionUnidadMinima(Producto producto) {
        return producto.getPresentaciones().stream()
                .filter(presentacion -> presentacion.getFactorAUnidadMinima() == 1)
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("FALTA_UNIDAD_MINIMA", "No hay presentación de unidad mínima"));
    }

    private void validarCategoria(Long categoriaId) {
        if (categoriaId != null) {
            categoriaService.exigirActiva(categoriaId);
        }
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

    private void validarPrecios(ProductoDTO dto) {
        if (dto.getPrecioCompra() == null || dto.getPrecioVenta() == null) {
            return;
        }
        if (dto.getPrecioVenta().compareTo(dto.getPrecioCompra()) < 0) {
            throw new ReglaNegocioException(
                    "PRECIO_VENTA_INVALIDO",
                    "El precio de venta no puede ser menor que el precio de compra"
            );
        }
    }

    private void validarCodigoUnico(String codigo, Long idExcluir) {
        String normalizado = codigo == null ? "" : codigo.trim();
        boolean duplicado = idExcluir == null
                ? productoRepository.existsByCodigoIgnoreCase(normalizado)
                : productoRepository.existsByCodigoIgnoreCaseAndIdNot(normalizado, idExcluir);
        if (duplicado) {
            throw new ReglaNegocioException("CODIGO_DUPLICADO", "Ya existe un producto con código " + normalizado);
        }
    }

    private void validarVencimiento(LocalDate fecha) {
        validarVencimiento(fecha, null);
    }

    /** En actualización se permite conservar una fecha vencida ya registrada. */
    private void validarVencimiento(LocalDate fecha, LocalDate fechaAnterior) {
        if (fecha == null) {
            return;
        }
        if (fechaAnterior != null && fecha.equals(fechaAnterior)) {
            return;
        }
        if (fecha.isBefore(LocalDate.now(clock))) {
            throw new ReglaNegocioException("VENCIMIENTO_PASADO", "La fecha de vencimiento no puede ser anterior a hoy");
        }
    }

    private String nombreCategoria(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        try {
            return categoriaService.obtener(categoriaId).getNombre();
        } catch (RecursoNoEncontradoException ignored) {
            return null;
        }
    }

    private boolean cambioPrecio(BigDecimal anterior, BigDecimal nuevo) {
        if (anterior == null && nuevo == null) {
            return false;
        }
        if (anterior == null || nuevo == null) {
            return true;
        }
        return anterior.compareTo(nuevo) != 0;
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
                .urlImagen(producto.getUrlImagen())
                .categoriaId(producto.getCategoriaId())
                .categoriaNombre(nombreCategoria(producto.getCategoriaId()))
                .unidadMinima(producto.getUnidadMinima())
                .precioCompra(producto.getPrecioCompra())
                .costoPromedio(producto.getPrecioCompra())
                .ultimoCostoCompra(producto.getUltimoCostoCompra() != null
                        ? producto.getUltimoCostoCompra()
                        : producto.getPrecioCompra())
                .precioVenta(producto.getPrecioVenta())
                .stockActual(producto.getStockActual())
                .stockMinimo(producto.getStockMinimo())
                .stockCritico(producto.getStockCritico())
                .fechaVencimiento(producto.getFechaVencimiento())
                .esAlcoholico(producto.getEsAlcoholico())
                .activo(producto.getActivo())
                .politicaPrecio(producto.getPoliticaPrecio() == null ? PoliticaPrecio.MANUAL : producto.getPoliticaPrecio())
                .margenObjetivoPct(producto.getMargenObjetivoPct())
                .precioVentaSugerido(politicaPrecioService.precioVentaDesdeMargen(
                        producto.getPrecioCompra(),
                        politicaPrecioService.margenEfectivo(producto)))
                .nivelAlerta(inventarioService.calcularNivelAlerta(producto))
                .eliminable(calcularEliminable(producto))
                .motivoNoEliminable(calcularMotivoNoEliminable(producto))
                .presentaciones(presentaciones)
                .build();
    }

    private boolean calcularEliminable(Producto producto) {
        return producto.getStockActual() == null || producto.getStockActual() <= 0;
    }

    private String calcularMotivoNoEliminable(Producto producto) {
        if (producto.getStockActual() != null && producto.getStockActual() > 0) {
            return "No se puede eliminar un producto con existencia en bodega";
        }
        return null;
    }

    private PresentacionDTO toPresentacionDto(Presentacion presentacion) {
        return PresentacionDTO.builder()
                .id(presentacion.getId())
                .nombre(presentacion.getNombre())
                .factorAUnidadMinima(presentacion.getFactorAUnidadMinima())
                .activo(presentacion.getActivo())
                .build();
    }

    private String normalizarUrlImagen(String url) {
        if (url == null) {
            return null;
        }
        String limpia = url.trim();
        return limpia.isEmpty() ? null : limpia;
    }
}
