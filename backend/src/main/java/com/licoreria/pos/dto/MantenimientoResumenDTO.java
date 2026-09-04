package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MantenimientoResumenDTO {

    private String baseDatos;
    private String motor;
    private String version;
    private String host;
    private Integer tablas;
    @Builder.Default
    private Long registrosEstimados = 0L;
    @Builder.Default
    private Long respaldosTotales = 0L;
    @Builder.Default
    private Long espacioRespaldosBytes = 0L;
    private String espacioRespaldosLegible;
    private LocalDateTime ultimoRespaldo;
    private Long horasDesdeUltimoRespaldo;
    private String directorioRespaldos;
    @Builder.Default
    private Boolean mysqldumpDisponible = false;
    @Builder.Default
    private Boolean mysqlDisponible = false;
    @Builder.Default
    private Boolean restauracionHabilitada = false;
    @Builder.Default
    private Integer maxRespaldos = 30;
    @Builder.Default
    private Integer tablasConAdvertencia = 0;
    @Builder.Default
    private Integer tablasConError = 0;
    @Builder.Default
    private Long espacioDiscoLibreBytes = 0L;
    private String espacioDiscoLibreLegible;
    /** OK | ADVERTENCIA | CRITICO */
    private String estadoSalud;
    /** OK | ADVERTENCIA | CRITICO */
    private String estadoRespaldo;
    @Builder.Default
    private List<AlertaMantenimientoDTO> alertas = new ArrayList<>();
    @Builder.Default
    private List<SaludTablaDTO> tablasSalud = new ArrayList<>();
}
