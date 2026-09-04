package com.licoreria.pos.dto;

import com.licoreria.pos.model.TipoEventoControlVenta;
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
public class ControlVentaEventoDTO {

    private TipoEventoControlVenta tipo;
    private LocalDateTime fecha;
    private String referencia;
    private Long entidadId;
    private String cajeroNombre;
    private Long cajeroId;
    private BigDecimal monto;
    private String detalle;
    private String nivel;
}
