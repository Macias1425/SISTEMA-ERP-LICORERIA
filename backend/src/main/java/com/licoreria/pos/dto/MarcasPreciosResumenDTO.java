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
public class MarcasPreciosResumenDTO {

    @Builder.Default
    private Integer marcasRegistradas = 0;
    @Builder.Default
    private Integer productosConMarca = 0;
    @Builder.Default
    private Integer productosSinMarca = 0;
    @Builder.Default
    private Long cambiosPrecioMes = 0L;
    @Builder.Default
    private Long cambiosPrecioTotal = 0L;
}
