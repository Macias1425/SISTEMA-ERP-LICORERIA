package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleCompraResponseDTO {

    private Long id;
    private Long productoId;
    private String productoNombre;
    private Long presentacionId;
    private String presentacionNombre;
    private Integer cantidad;
    private Integer cantidadOrdenada;
    private Integer cantidadRecibida;
    private Integer cantidadRechazada;
    private Integer cantidadPendiente;
    private String notasQc;
    private Integer cantidadUmm;
    private BigDecimal costoUnitario;
    private BigDecimal subtotal;
    private LocalDate fechaVencimiento;
}
