package com.licoreria.pos.dto;

import com.licoreria.pos.model.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventarioResponseDTO {

    private Long id;
    private Long productoId;
    private String productoNombre;
    private Long presentacionId;
    private String presentacionNombre;
    private TipoMovimiento tipo;
    private Integer cantidadPresentacion;
    private Integer cantidadUmm;
    private Integer stockResultante;
    private String motivo;
    private LocalDateTime fecha;
    private Long usuarioId;
    private String usuarioNombre;
    private Long compraId;
    private Long ventaId;
    private Long mermaId;
    /** ENTRADA o SALIDA según impacto en stock. */
    private String direccion;
    /** Lotes afectados en orden FEFO, ej. "L20260830-4x6". */
    private String detalleLotes;
}
