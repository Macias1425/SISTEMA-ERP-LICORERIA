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
public class AbonoCreditoDTO {

    @NotNull(message = "El monto del abono es obligatorio")
    @DecimalMin(value = "0.01", message = "El abono debe ser mayor a cero")
    private BigDecimal monto;
}
