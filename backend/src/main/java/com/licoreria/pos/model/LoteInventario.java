package com.licoreria.pos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lote de existencia con costo y vencimiento propios. Permite trazabilidad de la
 * botella vendida y rotación FEFO (primero el que vence antes).
 * El saldo global sigue viviendo en {@link Producto#getStockActual()}; el lote lo desglosa.
 */
@Entity
@Table(name = "lotes_inventario", indexes = {
        @Index(name = "idx_lote_producto", columnList = "producto_id"),
        @Index(name = "idx_lote_vencimiento", columnList = "fecha_vencimiento")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "presentacion_id")
    private Long presentacionId;

    @Column(name = "compra_id")
    private Long compraId;

    @Column(name = "proveedor_nombre", length = 150)
    private String proveedorNombre;

    /** Cantidad con la que nació el lote, en unidad mínima. */
    @Column(name = "cantidad_inicial_umm", nullable = false)
    private Integer cantidadInicialUmm;

    /** Saldo vivo del lote, en unidad mínima. */
    @Column(name = "cantidad_disponible_umm", nullable = false)
    private Integer cantidadDisponibleUmm;

    @Column(name = "costo_unitario_umm", precision = 12, scale = 4)
    private BigDecimal costoUnitarioUmm;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDateTime fechaIngreso;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
}
