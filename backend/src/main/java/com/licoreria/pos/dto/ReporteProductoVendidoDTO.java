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
public class ReporteProductoVendidoDTO {

    private Long productoId;
    private String codigo;
    private String nombre;
    private Integer cantidadUmm;
    private Integer tickets;
    private BigDecimal total;
}
