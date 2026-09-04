package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Datos del negocio que necesita cualquier operador para imprimir un comprobante.
 * Es una vista reducida de la configuración: no expone parámetros de seguridad ni
 * el correlativo fiscal, así que basta con estar autenticado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosNegocioDTO {

    private String nombreNegocio;
    private String direccionNegocio;
    private String telefonoNegocio;
    private String rucEmisor;
    private String autorizacionDgi;
    private BigDecimal tasaIva;
    private Integer edadMinimaAlcohol;
    private Boolean facturacionFiscalHabilitada;
}
