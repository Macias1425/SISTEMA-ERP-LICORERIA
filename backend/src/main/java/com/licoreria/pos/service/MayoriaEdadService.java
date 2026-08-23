package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.TipoVerificacionEdad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class MayoriaEdadService {

    private final PosProperties posProperties;
    private final Clock clock;

    public TipoVerificacionEdad validar(boolean hayAlcohol, LocalDate fechaNacimiento, boolean confirmacionCajero) {
        if (!hayAlcohol) {
            return null;
        }

        int edadMinima = posProperties.getNormativa().getEdadMinimaAlcohol();
        LocalDate hoy = LocalDate.now(clock);

        if (fechaNacimiento != null) {
            if (fechaNacimiento.isAfter(hoy)) {
                throw new ReglaNegocioException("FECHA_NACIMIENTO_INVALIDA", "La fecha de nacimiento no puede ser futura");
            }
            int edad = Period.between(fechaNacimiento, hoy).getYears();
            if (edad < edadMinima) {
                throw new ReglaNegocioException(
                        "MENOR_DE_EDAD",
                        "La venta de licor está bloqueada: se requiere al menos " + edadMinima + " años"
                );
            }
            return TipoVerificacionEdad.FECHA_NACIMIENTO;
        }

        if (confirmacionCajero) {
            return TipoVerificacionEdad.CONFIRMACION_CAJERO;
        }

        throw new ReglaNegocioException(
                "VERIFICACION_EDAD_REQUERIDA",
                "Para vender alcohol debe capturar la fecha de nacimiento o confirmar visualmente la mayoría de edad"
        );
    }
}
