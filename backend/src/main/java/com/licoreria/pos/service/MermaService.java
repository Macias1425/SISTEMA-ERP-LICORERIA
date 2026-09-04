package com.licoreria.pos.service;

import com.licoreria.pos.dto.AutorizacionDTO;
import com.licoreria.pos.dto.MermaRequestDTO;
import com.licoreria.pos.dto.MermaResponseDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.EstadoMerma;
import com.licoreria.pos.model.Merma;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.MermaRepository;
import com.licoreria.pos.repository.PresentacionRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MermaService {

    private final MermaRepository mermaRepository;
    private final ProductoRepository productoRepository;
    private final PresentacionRepository presentacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;
    private final ConversionUnidades conversionUnidades;
    private final AutorizacionService autorizacionService;
    private final AuditoriaService auditoriaService;
    private final PermisoService permisoService;

    @Transactional(readOnly = true)
    public PaginaDTO<MermaResponseDTO> listar(EstadoMerma estado, Long productoId, LocalDate desde, LocalDate hasta,
                                              int pagina, int tamano) {
        Usuario operador = autorizacionService.operadorActual();
        permisoService.exigirPermiso(operador, Permiso.INVENTARIO_VER);
        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime fin = hasta == null ? null : hasta.atTime(LocalTime.MAX);
        Page<Merma> page = mermaRepository.buscarPaginado(estado, productoId, inicio, fin,
                PaginacionUtil.pageable(pagina, tamano));
        return PaginaDTO.de(page.map(merma -> toDto(merma, operador)));
    }

    @Transactional(readOnly = true)
    public List<MermaResponseDTO> listar(EstadoMerma estado, Long productoId, LocalDate desde, LocalDate hasta) {
        return listar(estado, productoId, desde, hasta, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public MermaResponseDTO obtener(Long id) {
        Usuario operador = autorizacionService.operadorActual();
        permisoService.exigirPermiso(operador, Permiso.INVENTARIO_VER);
        return toDto(mermaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Merma no encontrada: " + id)), operador);
    }

    @Transactional
    public MermaResponseDTO solicitar(MermaRequestDTO dto) {
        Usuario solicitante = autorizacionService.operadorActual();
        permisoService.exigirPermiso(solicitante, Permiso.MERMA_SOLICITAR);
        if (dto.getMotivo() == null || dto.getMotivo().trim().length() < 5) {
            throw new ReglaNegocioException("MOTIVO_INSUFICIENTE", "El motivo de la merma debe tener al menos 5 caracteres");
        }

        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + dto.getProductoId()));
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ReglaNegocioException("PRODUCTO_INACTIVO", "El producto no está activo: " + producto.getNombre());
        }

        Presentacion presentacion = inventarioService.obtenerPresentacion(dto.getProductoId(), dto.getPresentacionId());
        int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, dto.getCantidad());
        inventarioService.exigirStockDisponible(dto.getProductoId(), cantidadUmm);

        Merma merma = Merma.builder()
                .productoId(dto.getProductoId())
                .presentacionId(dto.getPresentacionId())
                .cantidadPresentacion(dto.getCantidad())
                .cantidadUmm(cantidadUmm)
                .tipo(dto.getTipo())
                .motivo(dto.getMotivo().trim())
                .estado(EstadoMerma.PENDIENTE)
                .solicitadoPor(solicitante.getId())
                .fechaSolicitud(LocalDateTime.now())
                .build();
        Merma guardada = mermaRepository.save(merma);
        auditoriaService.registrar(solicitante, AccionAuditoria.MERMA_SOLICITADA, "Merma",
                guardada.getId(), null, "PENDIENTE", dto.getMotivo());
        return toDto(guardada, solicitante);
    }

    @Transactional
    public MermaResponseDTO aprobar(Long mermaId, AutorizacionDTO autorizacion) {
        Merma merma = buscarPendiente(mermaId);
        autorizacionService.operadorActual();
        Usuario autorizador = autorizacionService.exigirCredencialAdmin(
                autorizacion,
                "La merma solo puede aprobarla o rechazarla un administrador con usuario y clave"
        );

        inventarioService.exigirStockDisponible(merma.getProductoId(), merma.getCantidadUmm());
        inventarioService.descontar(
                merma.getProductoId(),
                merma.getPresentacionId(),
                merma.getCantidadPresentacion(),
                TipoMovimiento.MERMA,
                merma.getMotivo(),
                autorizador.getId(),
                null,
                null,
                merma.getId()
        );

        merma.setEstado(EstadoMerma.APROBADA);
        merma.setAutorizadoPor(autorizador.getId());
        merma.setFechaResolucion(LocalDateTime.now());
        Merma guardada = mermaRepository.save(merma);
        auditoriaService.registrar(autorizador, AccionAuditoria.MERMA_APROBADA, "Merma", guardada.getId(),
                "PENDIENTE", "APROBADA", merma.getMotivo());
        return toDto(guardada, autorizador);
    }

    @Transactional
    public MermaResponseDTO rechazar(Long mermaId, AutorizacionDTO autorizacion) {
        Merma merma = buscarPendiente(mermaId);
        autorizacionService.operadorActual();
        Usuario autorizador = autorizacionService.exigirCredencialAdmin(
                autorizacion,
                "La merma solo puede aprobarla o rechazarla un administrador con usuario y clave"
        );

        merma.setEstado(EstadoMerma.RECHAZADA);
        merma.setAutorizadoPor(autorizador.getId());
        merma.setFechaResolucion(LocalDateTime.now());
        Merma guardada = mermaRepository.save(merma);
        auditoriaService.registrar(autorizador, AccionAuditoria.MERMA_RECHAZADA, "Merma", guardada.getId(),
                "PENDIENTE", "RECHAZADA", merma.getMotivo());
        return toDto(guardada, autorizador);
    }

    private Merma buscarPendiente(Long mermaId) {
        Merma merma = mermaRepository.findById(mermaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Merma no encontrada: " + mermaId));
        if (merma.getEstado() != EstadoMerma.PENDIENTE) {
            throw new ReglaNegocioException("MERMA_NO_PENDIENTE", "La merma ya fue resuelta: " + merma.getEstado());
        }
        return merma;
    }

    private MermaResponseDTO toDto(Merma merma, Usuario operador) {
        boolean esAdmin = operador != null && operador.getRol() == Rol.ADMIN;
        boolean pendiente = merma.getEstado() == EstadoMerma.PENDIENTE;
        boolean resoluble = pendiente && esAdmin;
        String motivoNoResolucion = null;
        if (pendiente && !esAdmin) {
            motivoNoResolucion = "Solo un administrador puede aprobar o rechazar mermas";
        } else if (!pendiente) {
            motivoNoResolucion = "La merma ya fue resuelta";
        }

        Producto producto = productoRepository.findById(merma.getProductoId()).orElse(null);
        String presentacionNombre = null;
        if (merma.getPresentacionId() != null && merma.getProductoId() != null) {
            presentacionNombre = presentacionRepository.findByIdAndProductoId(merma.getPresentacionId(), merma.getProductoId())
                    .map(Presentacion::getNombre)
                    .orElse(null);
        }

        return MermaResponseDTO.builder()
                .id(merma.getId())
                .productoId(merma.getProductoId())
                .productoNombre(producto == null ? null : producto.getNombre())
                .presentacionId(merma.getPresentacionId())
                .presentacionNombre(presentacionNombre)
                .cantidadPresentacion(merma.getCantidadPresentacion())
                .cantidadUmm(merma.getCantidadUmm())
                .tipo(merma.getTipo())
                .motivo(merma.getMotivo())
                .estado(merma.getEstado())
                .solicitadoPor(merma.getSolicitadoPor())
                .solicitadoPorNombre(nombreUsuario(merma.getSolicitadoPor()))
                .autorizadoPor(merma.getAutorizadoPor())
                .autorizadoPorNombre(nombreUsuario(merma.getAutorizadoPor()))
                .fechaSolicitud(merma.getFechaSolicitud())
                .fechaResolucion(merma.getFechaResolucion())
                .aprobable(resoluble)
                .rechazable(resoluble)
                .motivoNoResolucion(motivoNoResolucion)
                .build();
    }

    private String nombreUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return usuarioRepository.findById(usuarioId)
                .map(Usuario::getNombreCompleto)
                .orElse(null);
    }
}
