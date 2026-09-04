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
public class ProductoPrecioCatalogoDTO {

    private Long id;
    private String codigo;
    private String nombre;
    private String marca;
    private String categoriaNombre;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private BigDecimal precioMayorista;
    private BigDecimal margenPct;
    private Boolean activo;
    private LocalDateTime ultimoCambioPrecio;
}
