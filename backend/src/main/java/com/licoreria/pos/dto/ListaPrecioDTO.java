package com.licoreria.pos.dto;

import com.licoreria.pos.model.TipoCliente;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class ListaPrecioDTO {

    private Long id;

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotNull(message = "El tipo de cliente es obligatorio")
    private TipoCliente tipoCliente;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    private BigDecimal precioUmm;

    @Min(value = 0, message = "El volumen mínimo no puede ser negativo")
    @Builder.Default
    private Integer volumenMinimoUmm = 0;
}
