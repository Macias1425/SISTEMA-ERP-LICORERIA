package com.licoreria.pos.dto;

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
public class EstadoNormativaDTO {

    private LocalDateTime horaServidor;
    private boolean horarioHabilitado;
    private boolean ventaLicorPermitidaAhora;
    private int edadMinimaAlcohol;
    private BigDecimal tasaIva;
    private int volumenMinimoUmm;
    private String mensaje;
    /** RN-CTL-01: ventas ≥ este monto pueden exigir admin (0 = desactivado). */
    private BigDecimal montoSupervisorRequerido;
    /** RN-CTL-02: límite de ventas por turno (0 = sin límite). */
    private Integer maxVentasPorTurno;
}
