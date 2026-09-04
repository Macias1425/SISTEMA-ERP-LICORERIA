package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaResumenDTO {

    @Builder.Default
    private Long eventosTotales = 0L;
    @Builder.Default
    private Long eventosHoy = 0L;
    @Builder.Default
    private Long ventasAuditadas = 0L;
    @Builder.Default
    private Long eventosCaja = 0L;
    @Builder.Default
    private Long riesgoAlto = 0L;
}
