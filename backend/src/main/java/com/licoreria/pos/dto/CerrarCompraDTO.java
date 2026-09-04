package com.licoreria.pos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CerrarCompraDTO {

    @NotBlank(message = "El motivo de cierre es obligatorio")
    private String motivo;
}
