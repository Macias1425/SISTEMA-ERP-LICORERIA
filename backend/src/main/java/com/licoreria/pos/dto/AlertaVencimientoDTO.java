package com.licoreria.pos.dto;

import com.licoreria.pos.model.NivelVencimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaVencimientoDTO {

    private Long productoId;
    private String codigo;
    private String nombre;
    private Integer stockActual;
    private String unidadMinima;
    private LocalDate fechaVencimiento;
    private Long diasRestantes;
    private NivelVencimiento estado;
}
