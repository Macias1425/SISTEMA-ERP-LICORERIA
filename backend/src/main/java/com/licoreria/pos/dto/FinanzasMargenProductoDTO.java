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
public class FinanzasMargenProductoDTO {

    private Long productoId;
    private String codigo;
    private String nombre;
    @Builder.Default
    private Integer cantidadUmm = 0;
    @Builder.Default
    private BigDecimal ingresos = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal costo = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margen = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margenPct = BigDecimal.ZERO;
}
