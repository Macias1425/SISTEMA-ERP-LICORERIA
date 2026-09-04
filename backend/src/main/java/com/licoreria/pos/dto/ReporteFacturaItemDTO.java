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
public class ReporteFacturaItemDTO {

    private Long facturaId;
    private Long ventaId;
    private String numero;
    private LocalDateTime fechaEmision;
    private String clienteNombre;
    private String cajeroNombre;
    private BigDecimal total;
    private String estado;
}
