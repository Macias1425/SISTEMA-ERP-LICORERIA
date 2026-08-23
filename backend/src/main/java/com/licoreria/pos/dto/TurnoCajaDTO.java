package com.licoreria.pos.dto;

import com.licoreria.pos.model.EstadoTurnoCaja;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoCajaDTO {

    private Long id;
    private Long usuarioId;
    private LocalDateTime fechaApertura;
    private BigDecimal montoInicial;
    private LocalDateTime fechaCierre;
    private BigDecimal ventasEfectivo;
    private BigDecimal montoEsperado;
    private BigDecimal montoFisico;
    private BigDecimal diferencia;
    private String resultadoArqueo;
    private String observacionArqueo;
    private EstadoTurnoCaja estado;
}
