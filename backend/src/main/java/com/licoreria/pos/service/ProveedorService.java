package com.licoreria.pos.service;

import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.PrecioProveedorDTO;
import com.licoreria.pos.dto.ProveedorDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.PrecioProveedor;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Proveedor;
import com.licoreria.pos.repository.PrecioProveedorRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.ProveedorRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final PrecioProveedorRepository precioProveedorRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final AccesoService accesoService;

    @Transactional(readOnly = true)
    public PaginaDTO<ProveedorDTO> listar(String busqueda, Boolean activo, int pagina, int tamano) {
        accesoService.exigirPermiso(Permiso.PROVEEDORES_GESTIONAR);
        String termino = busqueda == null || busqueda.isBlank() ? null : busqueda.trim();
        return PaginaDTO.de(proveedorRepository.buscarPaginado(
                termino, activo, PaginacionUtil.pageable(pagina, tamano)
        ).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public List<ProveedorDTO> listar(String busqueda, Boolean activo) {
        return listar(busqueda, activo, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public List<ProveedorDTO> listarActivos() {
        accesoService.exigirPermiso(Permiso.PROVEEDORES_GESTIONAR);
        return proveedorRepository.findByActivoTrueOrderByNombreAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ProveedorDTO obtener(Long id) {
        accesoService.exigirPermiso(Permiso.PROVEEDORES_GESTIONAR);
        return toDto(buscar(id));
    }

    @Transactional
    public ProveedorDTO crear(ProveedorDTO dto) {
        accesoService.exigirPermiso(Permiso.PROVEEDORES_GESTIONAR);
        String nombre = dto.getNombre().trim();
        if (proveedorRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException("PROVEEDOR_DUPLICADO", "Ya existe el proveedor " + nombre);
        }
        Proveedor proveedor = Proveedor.builder()
                .nombre(nombre)
                .documento(texto(dto.getDocumento()))
                .contactoNombre(texto(dto.getContactoNombre()))
                .telefono(texto(dto.getTelefono()))
                .email(texto(dto.getEmail()))
                .activo(dto.getActivo() == null || dto.getActivo())
                .build();
        return toDto(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorDTO actualizar(Long id, ProveedorDTO dto) {
        accesoService.exigirPermiso(Permiso.PROVEEDORES_GESTIONAR);
        Proveedor proveedor = buscar(id);
        String nombre = dto.getNombre().trim();
        if (proveedorRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ReglaNegocioException("PROVEEDOR_DUPLICADO", "Ya existe el proveedor " + nombre);
        }
        if (Boolean.FALSE.equals(dto.getActivo()) && Boolean.TRUE.equals(proveedor.getActivo())) {
            validarDesactivacion(id);
        }
        proveedor.setNombre(nombre);
        proveedor.setDocumento(texto(dto.getDocumento()));
        proveedor.setContactoNombre(texto(dto.getContactoNombre()));
        proveedor.setTelefono(texto(dto.getTelefono()));
        proveedor.setEmail(texto(dto.getEmail()));
        if (dto.getActivo() != null) {
            proveedor.setActivo(dto.getActivo());
        }
        return toDto(proveedorRepository.save(proveedor));
    }

    public Proveedor exigirActivo(Long proveedorId) {
        Proveedor proveedor = buscar(proveedorId);
        if (!Boolean.TRUE.equals(proveedor.getActivo())) {
            throw new ReglaNegocioException("PROVEEDOR_INACTIVO", "El proveedor no está activo: " + proveedor.getNombre());
        }
        return proveedor;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<PrecioProveedorDTO> listarPrecios(Long proveedorId, Boolean soloActivos, int pagina, int tamano) {
        accesoService.exigirPermiso(Permiso.PROVEEDORES_GESTIONAR);
        buscar(proveedorId);
        var page = Boolean.TRUE.equals(soloActivos)
                ? precioProveedorRepository.findByProveedorIdAndActivoTrueOrderByProductoIdAsc(
                        proveedorId, PaginacionUtil.pageable(pagina, tamano))
                : precioProveedorRepository.findByProveedorIdOrderByProductoIdAscPresentacionIdAsc(
                        proveedorId, PaginacionUtil.pageable(pagina, tamano));
        return PaginaDTO.de(page.map(this::toPrecioDto));
    }

    @Transactional(readOnly = true)
    public List<PrecioProveedorDTO> listarPrecios(Long proveedorId, Boolean soloActivos) {
        return listarPrecios(proveedorId, soloActivos, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public PrecioProveedorDTO consultarPrecio(Long proveedorId, Long productoId, Long presentacionId) {
        accesoService.exigirPermiso(Permiso.PROVEEDORES_GESTIONAR);
        exigirActivo(proveedorId);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + productoId));
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ReglaNegocioException("PRODUCTO_INACTIVO", "El producto no está activo: " + producto.getNombre());
        }
        inventarioService.obtenerPresentacion(productoId, presentacionId);
        return precioProveedorRepository
                .findByProveedorIdAndProductoIdAndPresentacionIdAndActivoTrue(proveedorId, productoId, presentacionId)
                .map(this::toPrecioDto)
                .orElse(null);
    }

    @Transactional
    public PrecioProveedorDTO guardarPrecio(Long proveedorId, PrecioProveedorDTO dto) {
        accesoService.exigirPermiso(Permiso.PROVEEDORES_GESTIONAR);
        Proveedor proveedor = exigirActivo(proveedorId);
        validarPrecioPositivo(dto.getPrecioUnitario());
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + dto.getProductoId()));
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ReglaNegocioException("PRODUCTO_INACTIVO", "El producto no está activo: " + producto.getNombre());
        }
        Presentacion presentacion = inventarioService.obtenerPresentacion(dto.getProductoId(), dto.getPresentacionId());

        PrecioProveedor precio = precioProveedorRepository
                .findByProveedorIdAndProductoIdAndPresentacionId(proveedorId, dto.getProductoId(), dto.getPresentacionId())
                .orElseGet(() -> PrecioProveedor.builder()
                        .proveedorId(proveedor.getId())
                        .productoId(producto.getId())
                        .presentacionId(presentacion.getId())
                        .build());

        precio.setPrecioUnitario(dto.getPrecioUnitario().setScale(2, java.math.RoundingMode.HALF_UP));
        precio.setActivo(dto.getActivo() == null || dto.getActivo());
        return toPrecioDto(precioProveedorRepository.save(precio));
    }

    private void validarDesactivacion(Long proveedorId) {
        if (precioProveedorRepository.countByProveedorId(proveedorId) > 0) {
            // Se permite desactivar aunque tenga catálogo; solo informamos en DTO.
            return;
        }
    }

    private void validarPrecioPositivo(BigDecimal precio) {
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("PRECIO_INVALIDO", "El precio del proveedor debe ser mayor a cero");
        }
    }

    private Proveedor buscar(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + id));
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private ProveedorDTO toDto(Proveedor proveedor) {
        long precios = precioProveedorRepository.countByProveedorId(proveedor.getId());
        boolean activo = Boolean.TRUE.equals(proveedor.getActivo());
        return ProveedorDTO.builder()
                .id(proveedor.getId())
                .nombre(proveedor.getNombre())
                .documento(proveedor.getDocumento())
                .contactoNombre(proveedor.getContactoNombre())
                .telefono(proveedor.getTelefono())
                .email(proveedor.getEmail())
                .activo(proveedor.getActivo())
                .cantidadPrecios((int) precios)
                .desactivable(activo)
                .motivoNoDesactivable(activo ? null : "El proveedor ya está inactivo")
                .build();
    }

    private PrecioProveedorDTO toPrecioDto(PrecioProveedor precio) {
        Producto producto = productoRepository.findById(precio.getProductoId()).orElse(null);
        String presentacionNombre = null;
        if (producto != null) {
            try {
                presentacionNombre = inventarioService
                        .obtenerPresentacion(precio.getProductoId(), precio.getPresentacionId())
                        .getNombre();
            } catch (RuntimeException ignored) {
                presentacionNombre = null;
            }
        }
        Proveedor proveedor = proveedorRepository.findById(precio.getProveedorId()).orElse(null);
        return PrecioProveedorDTO.builder()
                .id(precio.getId())
                .proveedorId(precio.getProveedorId())
                .proveedorNombre(proveedor == null ? null : proveedor.getNombre())
                .productoId(precio.getProductoId())
                .productoNombre(producto == null ? null : producto.getNombre())
                .productoCodigo(producto == null ? null : producto.getCodigo())
                .presentacionId(precio.getPresentacionId())
                .presentacionNombre(presentacionNombre)
                .precioUnitario(precio.getPrecioUnitario())
                .activo(precio.getActivo())
                .build();
    }
}
