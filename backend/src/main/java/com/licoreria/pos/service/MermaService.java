package com.licoreria.pos.service;

import com.licoreria.pos.dto.AutorizacionDTO;
import com.licoreria.pos.dto.MermaRequestDTO;
import com.licoreria.pos.dto.MermaResponseDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.EstadoMerma;
import com.licoreria.pos.model.Merma;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.MermaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MermaService {

    private final MermaRepository mermaRepository;
    private final InventarioService inventarioService;
    private final ConversionUnidades conversionUnidades;
    private final AutorizacionService autorizacionService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<MermaResponseDTO> listar(EstadoMerma estado) {
        autorizacionService.exigirRol(Rol.ALMACENISTA, Rol.ADMIN, Rol.CAJERO);
        List<Merma> mermas = estado == null
                ? mermaRepository.findAllByOrderByFechaSolicitudDesc()
                : mermaRepository.findByEstadoOrderByFechaSolicitudAsc(estado);
        return mermas.stream().map(this::toDto).toList();
    }

    @Transactional
    public MermaResponseDTO solicitar(MermaRequestDTO dto) {
        Usuario solicitante = autorizacionService.exigirRol(Rol.ALMACENISTA, Rol.ADMIN, Rol.CAJERO);
        Presentacion presentacion = inventarioService.obtenerPresentacion(dto.getProductoId(), dto.getPresentacionId());
        int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, dto.getCantidad());

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
        return toDto(guardada);
    }

    @Transactional
    public MermaResponseDTO aprobar(Long mermaId, AutorizacionDTO autorizacion) {
        Merma merma = buscarPendiente(mermaId);
        autorizacionService.operadorActual();
        Usuario autorizador = autorizacionService.exigirCredencialAdmin(
                autorizacion,
                "La merma solo puede aprobarla o rechazarla un administrador con usuario y clave"
        );

        inventarioService.descontar(
                merma.getProductoId(),
                merma.getPresentacionId(),
                merma.getCantidadPresentacion(),
                TipoMovimiento.MERMA,
                merma.getMotivo(),
                autorizador.getId()
        );

        merma.setEstado(EstadoMerma.APROBADA);
        merma.setAutorizadoPor(autorizador.getId());
        merma.setFechaResolucion(LocalDateTime.now());
        Merma guardada = mermaRepository.save(merma);
        auditoriaService.registrar(autorizador, AccionAuditoria.MERMA_APROBADA, "Merma", guardada.getId(),
                "PENDIENTE", "APROBADA", merma.getMotivo());
        return toDto(guardada);
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
        return toDto(guardada);
    }

    private Merma buscarPendiente(Long mermaId) {
        Merma merma = mermaRepository.findById(mermaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Merma no encontrada: " + mermaId));
        if (merma.getEstado() != EstadoMerma.PENDIENTE) {
            throw new ReglaNegocioException("MERMA_NO_PENDIENTE", "La merma ya fue resuelta: " + merma.getEstado());
        }
        return merma;
    }

    private MermaResponseDTO toDto(Merma merma) {
        return MermaResponseDTO.builder()
                .id(merma.getId())
                .productoId(merma.getProductoId())
                .presentacionId(merma.getPresentacionId())
                .cantidadPresentacion(merma.getCantidadPresentacion())
                .cantidadUmm(merma.getCantidadUmm())
                .tipo(merma.getTipo())
                .motivo(merma.getMotivo())
                .estado(merma.getEstado())
                .solicitadoPor(merma.getSolicitadoPor())
                .autorizadoPor(merma.getAutorizadoPor())
                .fechaSolicitud(merma.getFechaSolicitud())
                .fechaResolucion(merma.getFechaResolucion())
                .build();
    }
}
