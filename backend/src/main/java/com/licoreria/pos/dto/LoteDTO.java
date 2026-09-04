package com.licoreria.pos.dto;

import com.licoreria.pos.model.NivelVencimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteDTO {

    private Long id;
    private String codigo;
    private Long productoId;
    private String productoNombre;
    private String productoCodigo;
    private Long compraId;
    private String proveedorNombre;
    private Integer cantidadInicialUmm;
    private Integer cantidadDisponibleUmm;
    private BigDecimal costoUnitarioUmm;
    private BigDecimal valorInventario;
    private LocalDateTime fechaIngreso;
    private LocalDate fechaVencimiento;
    private Integer diasParaVencer;
    private NivelVencimiento nivelVencimiento;
    private Boolean agotado;
}
