package com.licoreria.pos.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CompraAnulacionDTO {

    @NotBlank
    @Size(min = 5, max = 255)
    private String motivo;

    private AutorizacionDTO autorizacion;
}
