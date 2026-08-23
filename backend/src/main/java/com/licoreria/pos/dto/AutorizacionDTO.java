package com.licoreria.pos.dto;

import jakarta.validation.constraints.NotBlank;
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
public class AutorizacionDTO {

    @NotBlank(message = "El usuario autorizador es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña del autorizador es obligatoria")
    private String password;
}
