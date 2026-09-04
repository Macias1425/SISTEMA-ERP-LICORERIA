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
public class ControlVentasCajeroDTO {

    private Long cajeroId;
    private String cajeroNombre;
    private long totalVentas;
    private BigDecimal montoTotal;
    private long alertas;
    private long overrides;
    private long anulaciones;
    private BigDecimal ticketPromedio;
}
