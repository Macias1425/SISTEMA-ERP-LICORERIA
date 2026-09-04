package com.licoreria.pos.dto;

import com.licoreria.pos.model.TipoCliente;
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
public class CotizacionLineaDTO {

    private Long productoId;
    private String productoNombre;
    private Long presentacionId;
    private String presentacionNombre;
    private Integer factorAUnidadMinima;
    private Integer cantidad;
    private Integer cantidadUmm;

    /** Precio de catálogo resuelto por el servidor (fuente de verdad). */
    private BigDecimal precioUnitarioUmm;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private TipoCliente tipoAplicadoLinea;

    /** Precio que envió el POS; permite detectar override antes de cobrar. */
    private BigDecimal precioSolicitado;
    private Boolean requiereAutorizacion;

    private Integer stockDisponibleUmm;
    private Boolean stockSuficiente;
    private Boolean vencido;
    private Boolean esAlcoholico;
    private String aviso;
}
