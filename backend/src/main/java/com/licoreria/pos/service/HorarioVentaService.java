package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.dto.EstadoNormativaDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.HorarioVentaLicor;
import com.licoreria.pos.repository.HorarioVentaLicorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class HorarioVentaService {

    private final PosProperties posProperties;
    private final HorarioVentaLicorRepository horarioRepository;
    private final Clock clock;

    public void validarVentaLicor(boolean hayAlcohol) {
        if (!hayAlcohol || !posProperties.getNormativa().isHorarioHabilitado()) {
            return;
        }
        if (!permitidoAhora()) {
            LocalDateTime ahora = LocalDateTime.now(clock);
            throw new ReglaNegocioException(
                    "FUERA_DE_HORARIO",
                    "No se puede facturar licor fuera del horario autorizado. Hora del servidor: " + ahora.toLocalTime()
            );
        }
    }

    @Transactional(readOnly = true)
    public EstadoNormativaDTO estadoActual() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        boolean habilitado = posProperties.getNormativa().isHorarioHabilitado();
        boolean permitido = !habilitado || permitidoAhora();
        return EstadoNormativaDTO.builder()
                .horaServidor(ahora)
                .horarioHabilitado(habilitado)
                .ventaLicorPermitidaAhora(permitido)
                .edadMinimaAlcohol(posProperties.getNormativa().getEdadMinimaAlcohol())
                .mensaje(permitido ? "Venta de licor permitida" : "Venta de licor bloqueada por horario")
                .build();
    }

    private boolean permitidoAhora() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        HorarioVentaLicor horario = horarioRepository.findByDiaSemanaAndActivoTrue(ahora.getDayOfWeek())
                .orElse(null);
        if (horario == null) {
            return false;
        }
        LocalTime hora = ahora.toLocalTime();
        return !hora.isBefore(horario.getHoraInicio()) && hora.isBefore(horario.getHoraFin());
    }
}
