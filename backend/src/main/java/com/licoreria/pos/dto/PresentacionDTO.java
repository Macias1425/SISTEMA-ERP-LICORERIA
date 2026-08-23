package com.licoreria.pos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentacionDTO {

    private Long id;

    @NotBlank(message = "El nombre de la presentación es obligatorio")
    private String nombre;

    @NotNull(message = "El factor de conversión es obligatorio")
    @Min(value = 1, message = "El factor debe ser al menos 1 (unidad mínima)")
    private Integer factorAUnidadMinima;

    @Builder.Default
    private Boolean activo = true;
}
