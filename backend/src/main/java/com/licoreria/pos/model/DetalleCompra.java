package com.licoreria.pos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "detalle_compras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "presentacion_id", nullable = false)
    private Long presentacionId;

    @Column(nullable = false)
    private Integer cantidad;

    /** Cantidad originalmente ordenada (empaque). */
    @Column(name = "cantidad_ordenada")
    private Integer cantidadOrdenada;

    @Column(name = "cantidad_recibida")
    private Integer cantidadRecibida;

    @Column(name = "cantidad_rechazada")
    private Integer cantidadRechazada;

    @Column(name = "notas_qc", length = 500)
    private String notasQc;

    @Column(nullable = false)
    private Integer cantidadUmm;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
}
