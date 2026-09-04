package com.licoreria.pos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "configuracion_pos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionPos {

    @Id
    private Long id;

    @Column(nullable = false, length = 120)
    @Builder.Default
    private String nombreNegocio = "Licorería POS";

    @Column(length = 180)
    private String direccionNegocio;

    @Column(length = 30)
    private String telefonoNegocio;

    @Column(nullable = false)
    @Builder.Default
    private Integer edadMinimaAlcohol = 18;

    @Column(nullable = false)
    @Builder.Default
    private Boolean horarioHabilitado = true;

    @Column(name = "tasa_iva", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal tasaIva = new BigDecimal("0.15");

    @Column(nullable = false)
    @Builder.Default
    private Integer volumenMinimoUmm = 6;

    @Column(nullable = false)
    @Builder.Default
    private Boolean requiereTurnoAbiertoParaAnular = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxIntentosLogin = 5;

    @Column(nullable = false)
    @Builder.Default
    private Integer bloqueoMinutos = 15;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoAlertaVenta = new BigDecimal("5000.00");

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoSupervisorRequerido = new BigDecimal("15000.00");

    @Column(nullable = false)
    @Builder.Default
    private Integer maxVentasPorTurno = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean alertarOverridePrecio = true;

    // ------------------------------------------------- Régimen fiscal (autorización DGI y rango)

    @Column(nullable = false)
    @Builder.Default
    private Boolean facturacionFiscalHabilitada = false;

    @Column(name = "ruc_emisor", length = 20)
    private String rucEmisor;

    /** Número de autorización de facturación otorgado por la DGI. */
    @Column(name = "autorizacion_dgi", length = 60)
    private String autorizacionDgi;

    @Column(length = 3)
    @Builder.Default
    private String establecimiento = "000";

    @Column(name = "punto_emision", length = 3)
    @Builder.Default
    private String puntoEmision = "001";

    @Column(name = "tipo_documento_fiscal", length = 2)
    @Builder.Default
    private String tipoDocumentoFiscal = "01";

    @Column(name = "rango_inicial")
    @Builder.Default
    private Long rangoInicial = 1L;

    @Column(name = "rango_final")
    @Builder.Default
    private Long rangoFinal = 1000L;

    /** Último correlativo emitido; el siguiente documento usa este valor + 1. */
    @Column(name = "correlativo_actual")
    @Builder.Default
    private Long correlativoActual = 0L;

    @Column(name = "fecha_limite_emision")
    private LocalDate fechaLimiteEmision;

    private LocalDateTime actualizadoEn;
}
