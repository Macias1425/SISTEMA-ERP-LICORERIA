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
public class AlertaMantenimientoDTO {

    /** info | advertencia | critico */
    private String nivel;
    private String codigo;
    private String mensaje;
}
