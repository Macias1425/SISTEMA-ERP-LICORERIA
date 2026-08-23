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
public class CierreCajaDTO {

    @NotNull(message = "El conteo físico es obligatorio para cerrar caja")
    @DecimalMin(value = "0.0", message = "El conteo físico no puede ser negativo")
    private BigDecimal montoFisico;

    private String observacionArqueo;
}
