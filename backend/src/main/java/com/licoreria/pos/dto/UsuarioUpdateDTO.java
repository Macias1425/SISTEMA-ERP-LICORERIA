package com.licoreria.pos.dto;

import com.licoreria.pos.model.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioUpdateDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;

    @NotNull(message = "El estado es obligatorio")
    private Boolean activo;
}
