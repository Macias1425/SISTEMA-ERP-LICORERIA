package com.licoreria.pos.service;

import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Presentacion;
import org.springframework.stereotype.Component;

@Component
public class ConversionUnidades {

    /**
     * RN-INV-01: convierte la cantidad de una presentación (caja, six-pack, botella)
     * a unidad mínima de medida (UMM).
     */
    public int aUnidadMinima(Presentacion presentacion, int cantidadPresentacion) {
        if (cantidadPresentacion <= 0) {
            throw new ReglaNegocioException("CANTIDAD_INVALIDA", "La cantidad debe ser mayor a cero");
        }
        if (presentacion == null || presentacion.getFactorAUnidadMinima() == null
                || presentacion.getFactorAUnidadMinima() < 1) {
            throw new ReglaNegocioException("PRESENTACION_INVALIDA", "La presentación no tiene un factor de conversión válido");
        }
        if (!Boolean.TRUE.equals(presentacion.getActivo())) {
            throw new ReglaNegocioException("PRESENTACION_INACTIVA", "La presentación no está activa");
        }
        return Math.multiplyExact(cantidadPresentacion, presentacion.getFactorAUnidadMinima());
    }
}
