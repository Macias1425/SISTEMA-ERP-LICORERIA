package com.licoreria.pos.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String numero;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TipoCliente tipoClienteSolicitado = TipoCliente.DETAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TipoCliente tipoClienteAplicado = TipoCliente.DETAL;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal impuesto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TipoVerificacionEdad verificacionEdad;

    private LocalDate fechaNacimientoCliente;

    @Column(nullable = false)
    @Builder.Default
    private Boolean confirmacionCajero = false;

    @Column(name = "autorizado_precio_por")
    private Long autorizadoPrecioPor;

    @Column(length = 255)
    private String observacion;

    @Column(name = "turno_caja_id")
    private Long turnoCajaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FormaPago formaPago = FormaPago.EFECTIVO;

    @Column(name = "monto_recibido", precision = 12, scale = 2)
    private BigDecimal montoRecibido;

    /** Id del PaymentIntent de Stripe cuando formaPago = STRIPE. */
    @Column(name = "stripe_payment_intent_id", length = 80)
    private String stripePaymentIntentId;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal vuelto = BigDecimal.ZERO;

    @Column(name = "clave_idempotencia", unique = true, length = 80)
    private String claveIdempotencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoVenta estado = EstadoVenta.ABIERTA;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @Builder.Default
    private List<DetalleVenta> detalles = new ArrayList<>();
}
