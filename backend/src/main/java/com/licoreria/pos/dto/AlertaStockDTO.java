package com.licoreria.pos.dto;

import com.licoreria.pos.model.NivelAlerta;
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
public class AlertaStockDTO {

    private Long productoId;
    private String codigo;
    private String nombre;
    private Integer stockActual;
    private Integer stockMinimo;
    private Integer stockCritico;
    private String unidadMinima;
    private NivelAlerta nivelAlerta;
}
