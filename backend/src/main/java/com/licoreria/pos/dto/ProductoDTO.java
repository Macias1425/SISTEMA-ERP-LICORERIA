package com.licoreria.pos.dto;

import com.licoreria.pos.model.NivelAlerta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDTO {

    private Long id;

    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String marca;
    private Long categoriaId;

    @Builder.Default
    private String unidadMinima = "BOTELLA";

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio de compra no puede ser negativo")
    private BigDecimal precioCompra;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio de venta no puede ser negativo")
    private BigDecimal precioVenta;

    /** Stock reportado siempre en UMM. En actualización se ignora: el stock solo cambia por movimientos. */
    @Min(value = 0, message = "El stock inicial no puede ser negativo")
    private Integer stockActual;

    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @Builder.Default
    private Integer stockMinimo = 0;

    @Min(value = 0, message = "El stock crítico no puede ser negativo")
    @Builder.Default
    private Integer stockCritico = 0;

    @Builder.Default
    private Boolean esAlcoholico = true;

    @Builder.Default
    private Boolean activo = true;

    private NivelAlerta nivelAlerta;

    @Valid
    @Builder.Default
    private List<PresentacionDTO> presentaciones = new ArrayList<>();
}
