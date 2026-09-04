package com.licoreria.pos.dto;

import com.licoreria.pos.model.EstadoMerma;
import com.licoreria.pos.model.TipoMerma;
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
public class MermaResponseDTO {

    private Long id;
    private Long productoId;
    private String productoNombre;
    private Long presentacionId;
    private String presentacionNombre;
    private Integer cantidadPresentacion;
    private Integer cantidadUmm;
    private TipoMerma tipo;
    private String motivo;
    private EstadoMerma estado;
    private Long solicitadoPor;
    private String solicitadoPorNombre;
    private Long autorizadoPor;
    private String autorizadoPorNombre;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    private Boolean aprobable;
    private Boolean rechazable;
    private String motivoNoResolucion;
}
