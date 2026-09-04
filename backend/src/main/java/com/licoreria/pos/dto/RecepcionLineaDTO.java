package com.licoreria.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class RecepcionLineaDTO {

    private Long detalleId;

    private Long productoId;
    private Long presentacionId;

    @Min(value = 0, message = "La cantidad recibida no puede ser negativa")
    @Builder.Default
    private Integer cantidadRecibida = 0;

    @Min(value = 0, message = "La cantidad rechazada no puede ser negativa")
    @Builder.Default
    private Integer cantidadRechazada = 0;

    private String notasQc;

    /** Solo aplica con política MANUAL: precio de venta explícito enviado en la recepción. */
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio de venta no puede ser negativo")
    private BigDecimal precioVentaExplicito;
}
