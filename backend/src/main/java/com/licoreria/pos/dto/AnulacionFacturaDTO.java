package com.licoreria.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AnulacionFacturaDTO {

    @Valid
    @NotNull(message = "Se requiere autorización de un administrador")
    private AutorizacionDTO autorizacion;

    @NotBlank(message = "El motivo de anulación es obligatorio")
    @Size(min = 5, message = "El motivo debe tener al menos 5 caracteres")
    private String motivo;
}
