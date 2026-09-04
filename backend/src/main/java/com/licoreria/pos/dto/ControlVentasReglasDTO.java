package com.licoreria.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class ControlVentasReglasDTO {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "El monto de alerta no puede ser negativo")
    private BigDecimal montoAlertaVenta;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "El monto supervisor no puede ser negativo")
    private BigDecimal montoSupervisorRequerido;

    @NotNull
    @Min(value = 0, message = "El límite de ventas por turno no puede ser negativo")
    @Max(value = 9999, message = "El límite de ventas por turno no puede superar 9999")
    private Integer maxVentasPorTurno;

    @NotNull
    private Boolean alertarOverridePrecio;

    private LocalDateTime actualizadoEn;
}
