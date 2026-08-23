package com.licoreria.pos.exception;

public class AutenticacionException extends RuntimeException {

    private final String codigo;

    public AutenticacionException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
