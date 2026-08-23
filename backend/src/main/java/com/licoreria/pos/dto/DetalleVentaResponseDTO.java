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
public class DetalleVentaResponseDTO {

    private Long productoId;
    private Long presentacionId;
    private Integer cantidad;
    private Integer cantidadUmm;
    private BigDecimal precioUnitarioUmm;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
