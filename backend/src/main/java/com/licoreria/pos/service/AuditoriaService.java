package com.licoreria.pos.service;

import com.licoreria.pos.dto.AuditoriaDTO;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Auditoria;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
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
    public List<AuditoriaDTO> listar() {
        return auditoriaRepository.findAllByOrderByFechaHoraDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> listarPorEntidad(String entidad, Long entidadId) {
        return auditoriaRepository.findByEntidadAndEntidadIdOrderByFechaHoraDesc(entidad, entidadId)
                .stream().map(this::toDto).toList();
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
        return AuditoriaDTO.builder()
                .id(log.getId())
                .usuarioId(log.getUsuarioId())
                .rol(log.getRol())
                .accion(log.getAccion())
                .entidad(log.getEntidad())
                .entidadId(log.getEntidadId())
                .valorAnterior(log.getValorAnterior())
                .valorNuevo(log.getValorNuevo())
                .detalle(log.getDetalle())
                .ip(log.getIp())
                .fechaHora(log.getFechaHora())
                .build();
    }
}
