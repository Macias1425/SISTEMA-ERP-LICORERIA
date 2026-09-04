package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteCompraItemDTO {

    private Long compraId;
    private Long proveedorId;
    private String numero;
    private LocalDateTime fecha;
    private String proveedorNombre;
    private BigDecimal total;
}
