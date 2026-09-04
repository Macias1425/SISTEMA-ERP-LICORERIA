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
public class FinanzasFlujoCajaDTO {

    @Builder.Default
    private BigDecimal entradasVentas = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal entradasEfectivo = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal entradasTarjeta = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal salidasCompras = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal flujoNeto = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ivaCobrado = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal faltanteCaja = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal sobranteCaja = BigDecimal.ZERO;
    @Builder.Default
    private Integer turnosCuadrados = 0;
    @Builder.Default
    private Integer turnosConDiferencia = 0;
}
