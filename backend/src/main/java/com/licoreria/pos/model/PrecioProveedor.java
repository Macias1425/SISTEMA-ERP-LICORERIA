package com.licoreria.pos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "precios_proveedor",
        uniqueConstraints = @UniqueConstraint(columnNames = {"proveedor_id", "producto_id", "presentacion_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrecioProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "presentacion_id", nullable = false)
    private Long presentacionId;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
