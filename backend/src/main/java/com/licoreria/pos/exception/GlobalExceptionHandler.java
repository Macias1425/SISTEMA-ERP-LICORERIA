package com.licoreria.pos.exception;

import com.licoreria.pos.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RecursoNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", ex.getMessage(), null);
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ErrorResponse> handleStock(StockInsuficienteException ex) {
        Map<String, Object> detalles = new LinkedHashMap<>();
        detalles.put("producto", ex.getProducto());
        detalles.put("stockActualUmm", ex.getStockActualUmm());
        detalles.put("cantidadSolicitadaUmm", ex.getCantidadSolicitadaUmm());
        return build(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), detalles);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> handleRegla(ReglaNegocioException ex) {
        return build(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), null);
    }

    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AutenticacionException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getCodigo(), ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "No tiene permiso para esta operación", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleSpringAuth(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Debe iniciar sesión", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDACION", mensaje, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error interno", null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String codigo, String mensaje,
                                                Map<String, Object> detalles) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .codigo(codigo)
                .mensaje(mensaje)
                .detalles(detalles)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
