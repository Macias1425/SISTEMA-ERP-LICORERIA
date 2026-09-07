package com.licoreria.pos.dto;

import com.licoreria.pos.model.FormaPago;
import com.licoreria.pos.model.TipoCliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
public class VentaRequestDTO {

    private Long clienteId;

    @Builder.Default
    private TipoCliente tipoCliente = TipoCliente.DETAL;

    private LocalDate fechaNacimientoCliente;

    @Builder.Default
    private Boolean confirmacionMayoriaEdad = false;

    /** Usuario y clave de un ADMIN cuando el precio sale del catálogo. */
    @Valid
    private AutorizacionDTO autorizacionSupervisor;

    @Builder.Default
    private FormaPago formaPago = FormaPago.EFECTIVO;

    @DecimalMin(value = "0.0", message = "El monto recibido no puede ser negativo")
    private BigDecimal montoRecibido;

    /** Obligatorio solo si formaPago = STRIPE y Stripe está habilitado. */
    @Size(max = 80, message = "El id de PaymentIntent es demasiado largo")
    private String stripePaymentIntentId;

    @Size(max = 80, message = "La clave de idempotencia es demasiado larga")
    private String claveIdempotencia;

    @Valid
    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Builder.Default
    private List<DetalleVentaDTO> detalles = new ArrayList<>();
}
