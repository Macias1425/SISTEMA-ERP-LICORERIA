package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraSugerenciaItemDTO {
    private Long productoId;
    private String codigo;
    private String nombre;
    private Long presentacionId;
    private String presentacionNombre;
    private Integer stockActual;
    private Integer vendidoPeriodoUmm;
    private BigDecimal promedioDiarioUmm;
    private Integer sugeridoUmm;
    private Integer diasCobertura;
    private String motivo;
}
