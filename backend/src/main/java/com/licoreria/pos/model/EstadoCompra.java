package com.licoreria.pos.model;

public enum EstadoCompra {
    /** Orden registrada; aún no ingresa stock. */
    PENDIENTE,
    /** Recepción parcial: queda saldo pendiente por recibir o rechazar. */
    PARCIAL,
    /** Mercancía recibida al 100 % (recibido + rechazado cubre lo ordenado). */
    RECIBIDA,
    /** Orden parcial cerrada manualmente; el proveedor no enviará el resto. */
    CERRADA,
    /** Orden cancelada o recepción revertida. */
    ANULADA
}
