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
    private Long presentacionId;
    private Integer cantidadPresentacion;
    private Integer cantidadUmm;
    private TipoMerma tipo;
    private String motivo;
    private EstadoMerma estado;
    private Long solicitadoPor;
    private Long autorizadoPor;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
}
