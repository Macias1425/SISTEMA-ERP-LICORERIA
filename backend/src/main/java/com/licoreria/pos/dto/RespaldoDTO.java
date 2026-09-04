package com.licoreria.pos.dto;

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
public class RespaldoDTO {

    private String nombre;
    private Long tamanoBytes;
    private String tamanoLegible;
    private LocalDateTime fechaCreacion;
    private String metodo;
    @Builder.Default
    private Boolean vacio = false;
    @Builder.Default
    private Boolean restaurable = true;
}
