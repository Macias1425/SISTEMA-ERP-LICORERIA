package com.licoreria.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class AperturaCajaDTO {

    @NotNull(message = "El fondo inicial es obligatorio")
    @DecimalMin(value = "0.0", message = "El fondo inicial no puede ser negativo")
    private BigDecimal montoInicial;
}
