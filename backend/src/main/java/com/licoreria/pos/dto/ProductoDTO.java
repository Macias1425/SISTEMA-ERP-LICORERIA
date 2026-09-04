package com.licoreria.pos.dto;

import com.licoreria.pos.model.NivelAlerta;
import com.licoreria.pos.model.PoliticaPrecio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDTO {

    private Long id;

    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String marca;
    private String urlImagen;
    private Long categoriaId;
    private String categoriaNombre;

    @Builder.Default
    private String unidadMinima = "BOTELLA";

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio de compra no puede ser negativo")
    private BigDecimal precioCompra;

    /** Costo promedio ponderado (CPP) en UMM; alias de precioCompra. */
    private BigDecimal costoPromedio;

    /** Último costo unitario de compra en UMM. */
    private BigDecimal ultimoCostoCompra;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio de venta no puede ser negativo")
    private BigDecimal precioVenta;

    /** Stock reportado siempre en UMM. En actualización se ignora: el stock solo cambia por movimientos. */
    @Min(value = 0, message = "El stock inicial no puede ser negativo")
    private Integer stockActual;

    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @Builder.Default
    private Integer stockMinimo = 0;

    @Min(value = 0, message = "El stock crítico no puede ser negativo")
    @Builder.Default
    private Integer stockCritico = 0;

    private LocalDate fechaVencimiento;

    @Builder.Default
    private Boolean esAlcoholico = true;

    @Builder.Default
    private Boolean activo = true;

    @Builder.Default
    private PoliticaPrecio politicaPrecio = PoliticaPrecio.MANUAL;

    /** Margen sobre venta (%) para políticas sugerido / automático. */
    private BigDecimal margenObjetivoPct;

    /** Solo lectura: precio de venta calculado con el margen objetivo y costo actual. */
    private BigDecimal precioVentaSugerido;

    private NivelAlerta nivelAlerta;

    private Boolean eliminable;
    private String motivoNoEliminable;

    @Valid
    @Builder.Default
    private List<PresentacionDTO> presentaciones = new ArrayList<>();
}
