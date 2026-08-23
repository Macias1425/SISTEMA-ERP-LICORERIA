package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoNormativaDTO {

    private LocalDateTime horaServidor;
    private boolean horarioHabilitado;
    private boolean ventaLicorPermitidaAhora;
    private int edadMinimaAlcohol;
    private String mensaje;
}
