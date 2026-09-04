package com.licoreria.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionDTO {

    @NotBlank(message = "El nombre del negocio es obligatorio")
    @Size(min = 3, max = 120, message = "El nombre del negocio debe tener entre 3 y 120 caracteres")
    private String nombreNegocio;

    @Size(max = 180, message = "La dirección no puede superar 180 caracteres")
    private String direccionNegocio;

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    private String telefonoNegocio;

    @NotNull
    @Min(value = 18, message = "La edad mínima legal para alcohol es 18 años")
    @Max(value = 99, message = "La edad mínima no puede superar 99 años")
    private Integer edadMinimaAlcohol;

    @NotNull
    private Boolean horarioHabilitado;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "El IVA no puede ser negativo")
    @DecimalMax(value = "1.0", inclusive = true, message = "El IVA no puede superar 100%")
    @JsonAlias("tasaIsv")
    private BigDecimal tasaIva;

    @NotNull
    @Min(value = 1, message = "El volumen mayorista debe ser al menos 1 UMM")
    @Max(value = 9999, message = "El volumen mayorista no puede superar 9999 UMM")
    private Integer volumenMinimoUmm;

    @NotNull
    private Boolean requiereTurnoAbiertoParaAnular;

    @NotNull
    @Min(value = 3, message = "Debe permitir al menos 3 intentos de login")
    @Max(value = 20, message = "No se permiten más de 20 intentos de login")
    private Integer maxIntentosLogin;

    @NotNull
    @Min(value = 5, message = "El bloqueo mínimo es de 5 minutos")
    @Max(value = 120, message = "El bloqueo máximo es de 120 minutos")
    private Integer bloqueoMinutos;

    @Valid
    @NotEmpty(message = "Debe configurar los horarios de la semana")
    @Builder.Default
    private List<HorarioVentaDTO> horarios = new ArrayList<>();

    // ------------------------------------------------- Régimen fiscal

    private Boolean facturacionFiscalHabilitada;

    @Size(max = 20, message = "El RUC no puede superar 20 caracteres")
    @JsonAlias("rtnEmisor")
    private String rucEmisor;

    @Size(max = 60, message = "La autorización DGI no puede superar 60 caracteres")
    @JsonAlias("cai")
    private String autorizacionDgi;

    @Size(max = 3, message = "El código de establecimiento usa 3 dígitos")
    private String establecimiento;

    @Size(max = 3, message = "El punto de emisión usa 3 dígitos")
    private String puntoEmision;

    @Size(max = 2, message = "El tipo de documento usa 2 dígitos")
    private String tipoDocumentoFiscal;

    private Long rangoInicial;
    private Long rangoFinal;
    private Long correlativoActual;
    private LocalDate fechaLimiteEmision;

    private LocalDateTime actualizadoEn;
    private Integer diasHorarioActivos;
    private EstadoFiscalDTO estadoFiscal;

    @JsonGetter("tasaIsv")
    public BigDecimal getTasaIsv() {
        return tasaIva;
    }

    @JsonGetter("rtnEmisor")
    public String getRtnEmisor() {
        return rucEmisor;
    }

    @JsonGetter("cai")
    public String getCai() {
        return autorizacionDgi;
    }
}
