package com.licoreria.pos.dto;

import com.licoreria.pos.model.DecisionPoliticaPrecio;
import com.licoreria.pos.model.PoliticaPrecio;
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
public class ImpactoRecepcionDTO {

    private Long productoId;
    private String productoNombre;

    private BigDecimal costoUltimoAnterior;
    private BigDecimal costoUltimoNuevo;
    private BigDecimal costoPromedioAnterior;
    private BigDecimal costoPromedioNuevo;

    private Integer cantidadAntes;
    private Integer cantidadRecibida;
    private Integer cantidadDespues;

    private BigDecimal ventaAnterior;
    private BigDecimal ventaNueva;
    private BigDecimal ventaSugerida;

    private BigDecimal markupActualPct;
    private BigDecimal margenActualPct;
    private BigDecimal margenMinimoPct;

    private PoliticaPrecio politicaPrecio;
    private DecisionPoliticaPrecio decisionPrecio;
    private String mensajeDecision;
    private Boolean ventaAplicada;
    private Boolean alertaMargen;
}
