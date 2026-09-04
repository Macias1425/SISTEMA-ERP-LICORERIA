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
public class MarcaResumenDTO {

    private String nombre;
    @Builder.Default
    private Integer productosTotal = 0;
    @Builder.Default
    private Integer productosActivos = 0;
    @Builder.Default
    private BigDecimal precioCompraPromedio = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal precioVentaPromedio = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margenPromedioPct = BigDecimal.ZERO;
}
