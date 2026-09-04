package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoTareaDTO {

    private boolean exito;
    private String tipo;
    private String mensaje;
    @Builder.Default
    private List<String> detalles = new ArrayList<>();
    @Builder.Default
    private Long duracionMs = 0L;
    @Builder.Default
    private Integer tablasOk = 0;
    @Builder.Default
    private Integer tablasAdvertencia = 0;
    @Builder.Default
    private Integer tablasError = 0;
    private String respaldoSeguridad;
}
