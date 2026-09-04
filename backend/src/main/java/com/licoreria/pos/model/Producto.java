package com.licoreria.pos.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 80)
    private String marca;

    @Column(name = "url_imagen", length = 512)
    private String urlImagen;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String unidadMinima = "BOTELLA";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCompra;

    /** Último costo unitario en UMM registrado por una recepción de compra. */
    @Column(name = "ultimo_costo_compra", precision = 12, scale = 4)
    private BigDecimal ultimoCostoCompra;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    /** Existencia física siempre en unidad mínima (UMM). Nunca por caja o six-pack. */
    @Column(nullable = false)
    @Builder.Default
    private Integer stockActual = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer stockMinimo = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer stockCritico = 0;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(nullable = false)
    @Builder.Default
    private Boolean esAlcoholico = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /** Cómo reacciona el precio de venta cuando el costo cambia por compras. */
    @Enumerated(EnumType.STRING)
    @Column(name = "politica_precio", nullable = false, length = 24)
    @Builder.Default
    private PoliticaPrecio politicaPrecio = PoliticaPrecio.MANUAL;

    /** Margen sobre precio de venta (%) para políticas SUGERIDO y AUTOMATICO_MARKUP. */
    @Column(name = "margen_objetivo_pct", precision = 5, scale = 2)
    private BigDecimal margenObjetivoPct;

    @Version
    private Long version;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Presentacion> presentaciones = new ArrayList<>();
}
