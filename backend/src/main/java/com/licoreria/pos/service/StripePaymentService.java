package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.dto.StripeConfigDTO;
import com.licoreria.pos.dto.StripePaymentIntentRequestDTO;
import com.licoreria.pos.dto.StripePaymentIntentResponseDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Permiso;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class StripePaymentService {

    private final PosProperties posProperties;
    private final AccesoService accesoService;

    public boolean estaActivo() {
        PosProperties.Stripe cfg = posProperties.getStripe();
        return cfg.isEnabled()
                && cfg.getSecretKey() != null
                && !cfg.getSecretKey().isBlank()
                && cfg.getPublishableKey() != null
                && !cfg.getPublishableKey().isBlank();
    }

    public StripeConfigDTO configPublica() {
        accesoService.exigirPermiso(Permiso.VENTAS_CREAR);
        PosProperties.Stripe cfg = posProperties.getStripe();
        boolean activo = estaActivo();
        return StripeConfigDTO.builder()
                .enabled(activo)
                .publishableKey(activo ? cfg.getPublishableKey() : null)
                .currency(cfg.getCurrency() == null || cfg.getCurrency().isBlank() ? "usd" : cfg.getCurrency().toLowerCase())
                .build();
    }

    public StripePaymentIntentResponseDTO crearPaymentIntent(StripePaymentIntentRequestDTO request) {
        accesoService.exigirPermiso(Permiso.VENTAS_CREAR);
        exigirActivo();
        configurarApiKey();

        long amount = aUnidadMenor(request.getMonto());
        String currency = moneda();
        try {
            PaymentIntent intent = PaymentIntent.create(PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency(currency)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putMetadata("origen", "pos-licoreria")
                    .build());

            return StripePaymentIntentResponseDTO.builder()
                    .paymentIntentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .currency(currency)
                    .amount(amount)
                    .build();
        } catch (StripeException e) {
            throw new ReglaNegocioException("STRIPE_ERROR", "No se pudo iniciar el cobro Stripe: " + e.getMessage());
        }
    }

    /**
     * Valida que el PaymentIntent exista, esté succeeded y coincida con el total de la venta.
     */
    public void exigirPagoExitoso(String paymentIntentId, BigDecimal totalVenta) {
        exigirActivo();
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new ReglaNegocioException("STRIPE_REQUERIDO", "Debe completar el pago con Stripe antes de registrar la venta");
        }
        configurarApiKey();
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId.trim());
            if (!"succeeded".equalsIgnoreCase(intent.getStatus())) {
                throw new ReglaNegocioException(
                        "STRIPE_NO_CONFIRMADO",
                        "El pago Stripe no está confirmado (estado: " + intent.getStatus() + ")"
                );
            }
            long esperado = aUnidadMenor(totalVenta);
            if (intent.getAmount() == null || intent.getAmount() != esperado) {
                throw new ReglaNegocioException(
                        "STRIPE_MONTO",
                        "El monto pagado en Stripe no coincide con el total de la venta"
                );
            }
            String currency = moneda();
            if (intent.getCurrency() != null && !currency.equalsIgnoreCase(intent.getCurrency())) {
                throw new ReglaNegocioException("STRIPE_MONEDA", "La moneda del pago Stripe no coincide con la configurada");
            }
        } catch (StripeException e) {
            throw new ReglaNegocioException("STRIPE_ERROR", "No se pudo verificar el pago Stripe: " + e.getMessage());
        }
    }

    private void exigirActivo() {
        if (!estaActivo()) {
            throw new ReglaNegocioException(
                    "STRIPE_DESACTIVADO",
                    "Stripe no está habilitado. Configure STRIPE_ENABLED y las claves de prueba/producción"
            );
        }
    }

    private void configurarApiKey() {
        Stripe.apiKey = posProperties.getStripe().getSecretKey().trim();
    }

    private String moneda() {
        String currency = posProperties.getStripe().getCurrency();
        return currency == null || currency.isBlank() ? "usd" : currency.trim().toLowerCase();
    }

    /** Convierte monto mayor (ej. 12.50) a unidad menor (1250). */
    static long aUnidadMenor(BigDecimal monto) {
        if (monto == null) {
            return 0L;
        }
        return monto.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }
}
