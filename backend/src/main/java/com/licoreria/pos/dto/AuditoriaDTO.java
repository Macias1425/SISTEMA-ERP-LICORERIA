package com.licoreria.pos.dto;

import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.NivelRiesgoAuditoria;
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
    private String usuarioNombre;
    private String rol;
    private AccionAuditoria accion;
    private String codigoEvento;
    private String eventoEtiqueta;
    private NivelRiesgoAuditoria nivelRiesgo;
    private String modulo;
    private String entidad;
    private String tablaEntidad;
    private Long entidadId;
    private String valorAnterior;
    private String valorNuevo;
    private String detalle;
    private String ip;
    private LocalDateTime fechaHora;
}
