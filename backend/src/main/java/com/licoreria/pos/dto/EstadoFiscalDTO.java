package com.licoreria.pos.dto;

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
public class EstadoFiscalDTO {

    private Boolean habilitada;
    private String autorizacionDgi;
    private String rucEmisor;
    private String rangoAutorizado;
    private Long rangoInicial;
    private Long rangoFinal;
    private Long correlativoActual;
    private String proximoNumero;
    private Long documentosDisponibles;
    private Integer porcentajeConsumido;
    private LocalDate fechaLimiteEmision;
    private Integer diasParaVencer;
    /** OK, POR_AGOTARSE, AGOTADO, VENCIDO, NO_CONFIGURADO o DESACTIVADA. */
    private String estado;
    private String mensaje;
}
