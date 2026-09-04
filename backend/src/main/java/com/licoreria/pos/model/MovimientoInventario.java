package com.licoreria.pos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "presentacion_id")
    private Long presentacionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private Integer cantidadPresentacion;

    /** Cantidad real descontada o ingresada en unidad mínima. */
    @Column(nullable = false)
    private Integer cantidadUmm;

    @Column(nullable = false)
    private Integer stockResultante;

    @Column(length = 255)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "compra_id")
    private Long compraId;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "merma_id")
    private Long mermaId;

    /** Para ajustes: true suma stock, false resta. */
    @Column(name = "incremento")
    private Boolean incremento;

    /** Trazabilidad FEFO: lotes afectados por el movimiento, ej. "L20260830-4x6". */
    @Column(name = "detalle_lotes", length = 255)
    private String detalleLotes;
}
