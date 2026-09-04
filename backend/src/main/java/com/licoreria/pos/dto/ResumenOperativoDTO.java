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
public class ResumenOperativoDTO {

    private LocalDate fecha;
    @Builder.Default
    private Integer ventasCantidad = 0;
    @Builder.Default
    private BigDecimal ventasSubtotal = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ventasImpuesto = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ventasTotal = BigDecimal.ZERO;
    @Builder.Default
    private Integer comprasCantidad = 0;
    @Builder.Default
    private BigDecimal comprasTotal = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultadoDia = BigDecimal.ZERO;
    @Builder.Default
    private Integer facturasEmitidas = 0;
    @Builder.Default
    private Integer facturasAnuladas = 0;
    @Builder.Default
    private BigDecimal facturasTotalEmitido = BigDecimal.ZERO;
    @Builder.Default
    private Integer turnosAbiertos = 0;
    private boolean ventaLicorPermitidaAhora;
    private String mensajeNormativa;
    @Builder.Default
    private List<AlertaStockDTO> alertasStock = new ArrayList<>();
}
