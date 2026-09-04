package com.licoreria.pos.service;

import com.licoreria.pos.model.DecisionPoliticaPrecio;
import com.licoreria.pos.model.PoliticaPrecio;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ResultadoPoliticaPrecio(
        DecisionPoliticaPrecio decision,
        BigDecimal ventaAnterior,
        BigDecimal ventaNueva,
        BigDecimal ventaSugerida,
        String mensaje,
        boolean ventaAplicada,
        PoliticaPrecio politica
) {
}
