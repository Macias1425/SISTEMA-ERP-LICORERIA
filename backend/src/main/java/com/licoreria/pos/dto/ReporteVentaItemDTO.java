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
public class ReporteVentaItemDTO {

    private Long ventaId;
    private String numero;
    private LocalDateTime fecha;
    private String clienteNombre;
    private Long usuarioId;
    private String cajeroNombre;
    private String formaPago;
    private String tipoCliente;
    private BigDecimal subtotal;
    private BigDecimal impuesto;
    private BigDecimal total;
    private String estado;
}
