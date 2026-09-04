package com.licoreria.pos.dto;

import com.licoreria.pos.model.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class UsuarioCreateDTO {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 3, max = 80, message = "El usuario debe tener entre 3 y 80 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El usuario solo puede contener letras, números, punto, guion o guion bajo")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 120, message = "El nombre completo debe tener entre 3 y 120 caracteres")
    private String nombreCompleto;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;
}
