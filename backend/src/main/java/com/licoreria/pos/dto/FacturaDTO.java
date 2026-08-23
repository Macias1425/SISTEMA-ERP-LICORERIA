package com.licoreria.pos.dto;

import com.licoreria.pos.model.EstadoFactura;
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
public class FacturaDTO {

    private Long id;
    private String numero;
    private Long ventaId;
    private LocalDateTime fechaEmision;
    private String clienteNombre;
    private String clienteRtn;
    private BigDecimal subtotal;
    private BigDecimal impuesto;
    private BigDecimal total;
    private EstadoFactura estado;
    private String motivoAnulacion;
    private Long anuladoPor;
    private LocalDateTime fechaAnulacion;
}
