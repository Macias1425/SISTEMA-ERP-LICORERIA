package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanzasComparativaDTO {

    private LocalDate periodoAnteriorDesde;
    private LocalDate periodoAnteriorHasta;
    @Builder.Default
    private BigDecimal ventasActual = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ventasAnterior = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ventasVariacionPct = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal comprasActual = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal comprasAnterior = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal comprasVariacionPct = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultadoActual = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultadoAnterior = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultadoVariacionPct = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margenActual = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margenAnterior = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margenVariacionPct = BigDecimal.ZERO;
}
