package com.licoreria.pos.dto;

import com.licoreria.pos.model.Permiso;
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
public class PermisoItemDTO {

    private Permiso codigo;
    private String etiqueta;
    private String modulo;
}
