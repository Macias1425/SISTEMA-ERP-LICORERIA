package com.licoreria.pos.service;

import com.licoreria.pos.dto.AuditoriaDTO;
import com.licoreria.pos.dto.AuditoriaResumenDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Auditoria;
import com.licoreria.pos.model.NivelRiesgoAuditoria;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.AuditoriaRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final Set<AccionAuditoria> ACCIONES_VENTA = EnumSet.of(
            AccionAuditoria.VENTA, AccionAuditoria.ANULACION_FACTURA);
    private static final Set<AccionAuditoria> ACCIONES_CAJA = EnumSet.of(
            AccionAuditoria.APERTURA_CAJA, AccionAuditoria.CIERRE_CAJA);
    private static final Set<AccionAuditoria> ACCIONES_INVENTARIO = EnumSet.of(
            AccionAuditoria.AJUSTE_STOCK,
            AccionAuditoria.ENTRADA_STOCK,
            AccionAuditoria.COMPRA,
            AccionAuditoria.MERMA_SOLICITADA,
            AccionAuditoria.MERMA_APROBADA,
            AccionAuditoria.MERMA_RECHAZADA);

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    @Transactional
    public void registrar(Usuario usuario, AccionAuditoria accion, String entidad, Long entidadId,
                          String valorAnterior, String valorNuevo, String detalle) {
        registrar(
                usuario == null ? null : usuario.getId(),
                usuario == null || usuario.getRol() == null ? null : usuario.getRol().name(),
                accion, entidad, entidadId, valorAnterior, valorNuevo, detalle
        );
    }

    @Transactional
    public void registrar(Long usuarioId, String rol, AccionAuditoria accion, String entidad, Long entidadId,
                          String valorAnterior, String valorNuevo, String detalle) {
        Auditoria log = Auditoria.builder()
                .usuarioId(usuarioId)
                .rol(rol)
                .accion(accion)
                .entidad(entidad)
                .entidadId(entidadId)
                .valorAnterior(valorAnterior)
                .valorNuevo(valorNuevo)
                .detalle(detalle)
                .ip(ipActual())
                .fechaHora(LocalDateTime.now(clock))
                .build();
        auditoriaRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<AuditoriaDTO> listar(int pagina, int tamano) {
        return PaginaDTO.de(auditoriaRepository.findAllByOrderByFechaHoraDesc(PaginacionUtil.pageable(pagina, tamano))
                .map(this::toDto));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> listar() {
        return listar(0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> listarPorAccion(AccionAuditoria accion) {
        return auditoriaRepository.findByAccionOrderByFechaHoraDesc(accion).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> listarPorEntidad(String entidad, Long entidadId) {
        return auditoriaRepository.findByEntidadAndEntidadIdOrderByFechaHoraDesc(entidad, entidadId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> buscar(
            String busqueda,
            LocalDate desde,
            LocalDate hasta,
            AccionAuditoria accion,
            String entidad,
            Long entidadId,
            Long usuarioId,
            NivelRiesgoAuditoria nivelRiesgo,
            String categoria) {
        RangoFechas rango = validarRango(desde, hasta);
        String termino = normalizar(busqueda);
        String entidadNorm = normalizar(entidad);
        List<Long> usuarioIdsBusqueda = idsUsuariosPorBusqueda(termino);

        return auditoriaRepository.buscar(
                        termino,
                        rango.desde(),
                        rango.hasta(),
                        accion,
                        entidadNorm,
                        entidadId,
                        usuarioId,
                        usuarioIdsBusqueda
                ).stream()
                .map(this::toDto)
                .filter(dto -> coincideNivelRiesgo(dto, nivelRiesgo))
                .filter(dto -> coincideCategoria(dto, categoria))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<AuditoriaDTO> buscar(
            String busqueda,
            LocalDate desde,
            LocalDate hasta,
            AccionAuditoria accion,
            String entidad,
            Long entidadId,
            Long usuarioId,
            NivelRiesgoAuditoria nivelRiesgo,
            String categoria,
            int pagina,
            int tamano) {
        boolean postFiltro = nivelRiesgo != null || (categoria != null && !categoria.isBlank());
        if (postFiltro) {
            return PaginacionUtil.deLista(
                    buscar(busqueda, desde, hasta, accion, entidad, entidadId, usuarioId, nivelRiesgo, categoria),
                    pagina,
                    tamano
            );
        }
        RangoFechas rango = validarRango(desde, hasta);
        String termino = normalizar(busqueda);
        String entidadNorm = normalizar(entidad);
        List<Long> usuarioIdsBusqueda = idsUsuariosPorBusqueda(termino);
        return PaginaDTO.de(auditoriaRepository.buscarPaginado(
                termino,
                rango.desde(),
                rango.hasta(),
                accion,
                entidadNorm,
                entidadId,
                usuarioId,
                usuarioIdsBusqueda,
                PaginacionUtil.pageable(pagina, tamano)
        ).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public AuditoriaResumenDTO resumen() {
        LocalDateTime inicioHoy = LocalDate.now(clock).atStartOfDay();
        long total = auditoriaRepository.count();
        long hoy = auditoriaRepository.countByFechaHoraGreaterThanEqual(inicioHoy);
        long ventas = auditoriaRepository.countByAccionIn(List.copyOf(ACCIONES_VENTA));
        long caja = auditoriaRepository.countByAccionIn(List.copyOf(ACCIONES_CAJA));
        long riesgoAlto = auditoriaRepository.findAllByOrderByFechaHoraDesc().stream()
                .filter(a -> nivelRiesgo(a.getAccion()) == NivelRiesgoAuditoria.ALTO)
                .count();
        return AuditoriaResumenDTO.builder()
                .eventosTotales(total)
                .eventosHoy(hoy)
                .ventasAuditadas(ventas)
                .eventosCaja(caja)
                .riesgoAlto(riesgoAlto)
                .build();
    }

    @Transactional(readOnly = true)
    public AuditoriaDTO obtener(Long id) {
        Auditoria log = auditoriaRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("AUDITORIA_NO_ENCONTRADA", "Evento no encontrado"));
        return toDto(log);
    }

    static NivelRiesgoAuditoria nivelRiesgo(AccionAuditoria accion) {
        if (accion == null) {
            return NivelRiesgoAuditoria.BAJO;
        }
        return switch (accion) {
            case ANULACION_FACTURA, BORRADO_PRODUCTO, MERMA_APROBADA, CONFIGURACION -> NivelRiesgoAuditoria.ALTO;
            case CAMBIO_PRECIO, AJUSTE_STOCK, MERMA_SOLICITADA, MERMA_RECHAZADA, APERTURA_CAJA, CIERRE_CAJA ->
                    NivelRiesgoAuditoria.MEDIO;
            default -> NivelRiesgoAuditoria.BAJO;
        };
    }

    static String etiquetaEvento(AccionAuditoria accion) {
        if (accion == null) {
            return "Evento";
        }
        return switch (accion) {
            case VENTA -> "Venta registrada";
            case ANULACION_FACTURA -> "Factura anulada";
            case COMPRA -> "Compra registrada";
            case CAMBIO_PRECIO -> "Cambio de precio";
            case ENTRADA_STOCK -> "Entrada de stock";
            case AJUSTE_STOCK -> "Ajuste de inventario";
            case CONFIGURACION -> "Configuración";
            case APERTURA_CAJA -> "Caja abierta";
            case CIERRE_CAJA -> "Caja cerrada";
            case MERMA_SOLICITADA -> "Merma solicitada";
            case MERMA_APROBADA -> "Merma aprobada";
            case MERMA_RECHAZADA -> "Merma rechazada";
            case BORRADO_PRODUCTO -> "Producto eliminado";
        };
    }

    static String moduloOperativo(AccionAuditoria accion) {
        if (accion == null) {
            return "Sistema";
        }
        return switch (accion) {
            case VENTA, ANULACION_FACTURA -> "Ventas";
            case APERTURA_CAJA, CIERRE_CAJA -> "Caja";
            case COMPRA -> "Compras";
            case ENTRADA_STOCK, AJUSTE_STOCK, MERMA_SOLICITADA, MERMA_APROBADA, MERMA_RECHAZADA -> "Inventario";
            case CAMBIO_PRECIO, BORRADO_PRODUCTO -> "Catálogo";
            case CONFIGURACION -> "Configuración";
        };
    }

    static String tablaEntidad(String entidad) {
        if (entidad == null || entidad.isBlank()) {
            return "—";
        }
        return switch (entidad.trim().toLowerCase(Locale.ROOT)) {
            case "producto" -> "productos";
            case "factura" -> "facturas";
            case "compra" -> "compras";
            case "turnocaja" -> "cash_register_sessions";
            case "configuracion", "configuración" -> "configuracion";
            case "usuario" -> "usuarios";
            case "merma" -> "mermas";
            case "inventario" -> "inventario_movimientos";
            default -> entidad.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        };
    }

    private boolean coincideNivelRiesgo(AuditoriaDTO dto, NivelRiesgoAuditoria nivelRiesgo) {
        return nivelRiesgo == null || dto.getNivelRiesgo() == nivelRiesgo;
    }

    private boolean coincideCategoria(AuditoriaDTO dto, String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return true;
        }
        return switch (categoria.trim().toLowerCase(Locale.ROOT)) {
            case "riesgo_alto" -> dto.getNivelRiesgo() == NivelRiesgoAuditoria.ALTO;
            case "caja" -> ACCIONES_CAJA.contains(dto.getAccion());
            case "inventario" -> ACCIONES_INVENTARIO.contains(dto.getAccion());
            case "ventas" -> ACCIONES_VENTA.contains(dto.getAccion());
            case "accesos_denegados" -> contieneAccesoDenegado(dto);
            default -> true;
        };
    }

    private boolean contieneAccesoDenegado(AuditoriaDTO dto) {
        String texto = ((dto.getDetalle() == null ? "" : dto.getDetalle()) + " "
                + (dto.getValorNuevo() == null ? "" : dto.getValorNuevo())).toLowerCase(Locale.ROOT);
        return texto.contains("deneg") || texto.contains("bloque") || texto.contains("login fall");
    }

    private List<Long> idsUsuariosPorBusqueda(String termino) {
        if (termino == null || termino.isBlank()) {
            return List.of(-1L);
        }
        List<Long> ids = usuarioRepository.buscar(termino, null, null).stream()
                .map(Usuario::getId)
                .toList();
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    private RangoFechas validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return new RangoFechas(null, null);
        }
        LocalDate inicio = desde != null ? desde : hasta;
        LocalDate fin = hasta != null ? hasta : desde;
        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "La fecha inicial no puede ser posterior a la final");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > 366) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "El rango no puede superar 366 días");
        }
        return new RangoFechas(inicio.atStartOfDay(), fin.atTime(LocalTime.MAX));
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private String ipActual() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            return attrs.getRequest().getRemoteAddr();
        } catch (Exception ignored) {
            return null;
        }
    }

    private AuditoriaDTO toDto(Auditoria log) {
        AccionAuditoria accion = log.getAccion();
        return AuditoriaDTO.builder()
                .id(log.getId())
                .usuarioId(log.getUsuarioId())
                .usuarioNombre(nombreUsuario(log.getUsuarioId()))
                .rol(log.getRol())
                .accion(accion)
                .codigoEvento(accion == null ? null : accion.name())
                .eventoEtiqueta(etiquetaEvento(accion))
                .nivelRiesgo(nivelRiesgo(accion))
                .modulo(moduloOperativo(accion))
                .entidad(log.getEntidad())
                .tablaEntidad(tablaEntidad(log.getEntidad()))
                .entidadId(log.getEntidadId())
                .valorAnterior(log.getValorAnterior())
                .valorNuevo(log.getValorNuevo())
                .detalle(log.getDetalle())
                .ip(log.getIp())
                .fechaHora(log.getFechaHora())
                .build();
    }

    private String nombreUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return usuarioRepository.findById(usuarioId).map(Usuario::getNombreCompleto).orElse(null);
    }

    private record RangoFechas(LocalDateTime desde, LocalDateTime hasta) {
    }
}
