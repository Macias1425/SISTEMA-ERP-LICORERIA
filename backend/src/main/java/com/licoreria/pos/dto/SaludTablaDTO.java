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
public class SaludTablaDTO {

    private String nombre;
    private String motor;
    @Builder.Default
    private Long filas = 0L;
    @Builder.Default
    private Double tamanoMb = 0.0;
    @Builder.Default
    private Double espacioLibreMb = 0.0;
    @Builder.Default
    private Double fragmentacionPct = 0.0;
    /** OK | ADVERTENCIA | ERROR */
    private String estado;
    private String comentario;
}
