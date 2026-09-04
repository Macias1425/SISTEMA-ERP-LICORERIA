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
public class PrecioProveedorDTO {

    private Long id;
    private Long proveedorId;
    private String proveedorNombre;

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;
    private String productoNombre;
    private String productoCodigo;

    @NotNull(message = "La presentación es obligatoria")
    private Long presentacionId;
    private String presentacionNombre;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a cero")
    private BigDecimal precioUnitario;

    @Builder.Default
    private Boolean activo = true;
}
