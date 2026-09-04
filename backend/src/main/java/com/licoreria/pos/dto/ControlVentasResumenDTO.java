package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlVentasResumenDTO {

    private LocalDate desde;
    private LocalDate hasta;
    private long totalVentas;
    private BigDecimal montoTotal;
    private BigDecimal ticketPromedio;
    private long ventasAltoMonto;
    private BigDecimal montoAltoMonto;
    private long overridesPrecio;
    private long ventasSupervisor;
    private long anulaciones;
    private long alertasTotales;
    private BigDecimal tasaAlertasPct;
    @Builder.Default
    private List<ControlVentasDiaDTO> serieDiaria = new ArrayList<>();
    @Builder.Default
    private List<ControlVentasCajeroDTO> porCajero = new ArrayList<>();
    @Builder.Default
    private List<ControlVentaEventoDTO> eventosRecientes = new ArrayList<>();
    private ControlVentasReglasDTO reglas;
}
