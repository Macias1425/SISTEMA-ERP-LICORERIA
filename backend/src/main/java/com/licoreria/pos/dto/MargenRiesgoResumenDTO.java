package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MargenRiesgoResumenDTO {

    private BigDecimal margenObjetivoPorcentaje;
    private Integer productosEvaluados;
    private Integer productosEnPerdida;
    private Integer productosCriticos;
    private Integer productosBajos;
    private Integer productosSanos;
    private BigDecimal margenPromedioPonderado;
    private BigDecimal capitalEnRiesgo;
    private BigDecimal valorInventarioCosto;

    @Builder.Default
    private List<MargenRiesgoItemDTO> items = new ArrayList<>();
    private Integer pagina;
    private Integer tamano;
    private Long totalElementos;
    private Integer totalPaginas;
}
