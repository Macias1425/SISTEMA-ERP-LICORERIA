package com.licoreria.pos.dto;

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
public class AlertaSeccionDTO {

    /** Identificador estable para que el frontend enlace el badge con su módulo. */
    private String clave;
    private String etiqueta;
    private String ruta;
    private Integer cantidad;
    /** CRITICA o AVISO: define el color y si suma al contador urgente. */
    private String nivel;
    private String detalle;
}
