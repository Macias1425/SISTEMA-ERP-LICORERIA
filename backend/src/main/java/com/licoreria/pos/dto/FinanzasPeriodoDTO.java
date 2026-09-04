package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanzasPeriodoDTO {

    private LocalDate desde;
    private LocalDate hasta;

    @Builder.Default
    private Integer ventasCantidad = 0;
    @Builder.Default
    private BigDecimal ventasSubtotal = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ventasImpuesto = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ventasTotal = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ventasEfectivoTotal = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ventasTarjetaTotal = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ticketPromedio = BigDecimal.ZERO;

    @Builder.Default
    private Integer comprasCantidad = 0;
    @Builder.Default
    private BigDecimal comprasTotal = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal costoVentas = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margenBruto = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margenBrutoPct = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultado = BigDecimal.ZERO;

    @Builder.Default
    private Integer facturasEmitidas = 0;
    @Builder.Default
    private Integer facturasAnuladas = 0;
    @Builder.Default
    private BigDecimal facturasTotalEmitido = BigDecimal.ZERO;
    @Builder.Default
    private Integer turnosCantidad = 0;

    private FinanzasFlujoCajaDTO flujo;
    private FinanzasComparativaDTO comparativa;

    @Builder.Default
    private List<FinanzasSerieDiariaDTO> serieDiaria = new ArrayList<>();
    @Builder.Default
    private List<FinanzasMargenProductoDTO> margenProductos = new ArrayList<>();
}
