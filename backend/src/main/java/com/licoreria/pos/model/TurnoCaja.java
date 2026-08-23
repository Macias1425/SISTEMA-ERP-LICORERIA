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
import java.time.LocalDateTime;

@Entity
@Table(name = "turnos_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInicial;

    private LocalDateTime fechaCierre;

    @Column(precision = 12, scale = 2)
    private BigDecimal ventasEfectivo;

    @Column(precision = 12, scale = 2)
    private BigDecimal montoEsperado;

    @Column(precision = 12, scale = 2)
    private BigDecimal montoFisico;

    @Column(precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Column(length = 255)
    private String observacionArqueo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoTurnoCaja estado = EstadoTurnoCaja.ABIERTO;
}
