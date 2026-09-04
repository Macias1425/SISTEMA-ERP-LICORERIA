package com.licoreria.pos.dto;

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
public class CategoriaDTO {

    private Long id;

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El estado es obligatorio")
    @Builder.Default
    private Boolean activo = true;
}
