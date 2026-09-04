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
public class FinanzasSerieDiariaDTO {

    private LocalDate fecha;
    @Builder.Default
    private BigDecimal ventas = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal compras = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultado = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margenBruto = BigDecimal.ZERO;
}
