package com.licoreria.pos.dto;

import com.licoreria.pos.model.AccionAuditoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaDTO {

    private Long id;
    private Long usuarioId;
    private String rol;
    private AccionAuditoria accion;
    private String entidad;
    private Long entidadId;
    private String valorAnterior;
    private String valorNuevo;
    private String detalle;
    private String ip;
    private LocalDateTime fechaHora;
}
