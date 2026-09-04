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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String numero;

    @Column(name = "venta_id", nullable = false, unique = true)
    private Long ventaId;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;

    @Column(length = 150)
    private String clienteNombre;

    @Column(name = "cliente_ruc", length = 20)
    private String clienteRuc;

    /** Número dentro del rango autorizado por la DGI (null si el régimen fiscal está desactivado). */
    @Column(name = "numero_fiscal", length = 40)
    private String numeroFiscal;

    @Column(name = "autorizacion_dgi", length = 60)
    private String autorizacionDgi;

    @Column(name = "rango_autorizado", length = 120)
    private String rangoAutorizado;

    @Column(name = "fecha_limite_emision")
    private LocalDate fechaLimiteEmision;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal impuesto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoFactura estado = EstadoFactura.EMITIDA;

    @Column(length = 255)
    private String motivoAnulacion;

    @Column(name = "anulado_por")
    private Long anuladoPor;

    @Column(name = "solicitado_anulacion_por")
    private Long solicitadoAnulacionPor;

    private LocalDateTime fechaAnulacion;
}
