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
@Table(name = "mermas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "presentacion_id", nullable = false)
    private Long presentacionId;

    @Column(nullable = false)
    private Integer cantidadPresentacion;

    @Column(nullable = false)
    private Integer cantidadUmm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMerma tipo;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoMerma estado = EstadoMerma.PENDIENTE;

    @Column(name = "solicitado_por", nullable = false)
    private Long solicitadoPor;

    @Column(name = "autorizado_por")
    private Long autorizadoPor;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaResolucion;
}
