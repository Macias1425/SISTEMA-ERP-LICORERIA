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
public class HistorialPrecioDTO {

    private Long id;
    private LocalDateTime fechaHora;
    private Long usuarioId;
    private String usuarioNombre;
    private Long productoId;
    private String productoCodigo;
    private String productoNombre;
    private String marca;
    private String tipoCambio;
    private String origen;
    private String valorAnterior;
    private String valorNuevo;
    private BigDecimal variacionPct;
    private String detalle;
}
