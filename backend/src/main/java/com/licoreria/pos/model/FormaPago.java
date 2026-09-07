package com.licoreria.pos.model;

public enum FormaPago {
    EFECTIVO,
    TARJETA,
    /** Cobro con Stripe (PaymentIntent). Solo si pos.stripe.enabled=true. */
    STRIPE,
    /** Venta a crédito / fiado (saldo por cliente). */
    CREDITO
}
