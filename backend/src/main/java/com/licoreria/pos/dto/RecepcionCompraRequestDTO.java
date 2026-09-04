package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecepcionCompraRequestDTO {

    @Builder.Default
    private Boolean actualizarCostos = true;

    /** Vacío = recibir todo el saldo pendiente de cada línea. */
    @Builder.Default
    private List<RecepcionLineaDTO> lineas = new ArrayList<>();
}
