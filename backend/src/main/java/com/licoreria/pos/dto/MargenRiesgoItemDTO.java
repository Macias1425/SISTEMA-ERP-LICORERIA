package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MargenRiesgoItemDTO {

    private Long productoId;
    private String codigo;
    private String nombre;
    private String marca;
    private Integer stockActual;

    /** Costo del catálogo (promedio ponderado que dejó la última compra). */
    private BigDecimal costoUmm;
    /** Costo real de los lotes vivos; si difiere del catálogo, el margen está mal medido. */
    private BigDecimal costoLotesUmm;
    private BigDecimal precioDetalUmm;
    private BigDecimal precioMayoristaUmm;

    private BigDecimal margenDetal;
    private BigDecimal margenDetalPorcentaje;
    private BigDecimal margenMayoristaPorcentaje;

    /** PERDIDA, CRITICO, BAJO o SANO. */
    private String nivel;
    private String motivo;
    /** Precio de venta sugerido para alcanzar el margen objetivo. */
    private BigDecimal precioSugeridoUmm;
    private BigDecimal capitalEnRiesgo;
}
