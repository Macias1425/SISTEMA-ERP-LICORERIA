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
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 30)
    private String rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AccionAuditoria accion;

    @Column(nullable = false, length = 60)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(length = 2000)
    private String valorAnterior;

    @Column(length = 2000)
    private String valorNuevo;

    @Column(length = 500)
    private String detalle;

    @Column(length = 60)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime fechaHora;
}
