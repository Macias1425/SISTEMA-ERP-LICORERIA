package com.licoreria.pos.service;

import com.licoreria.pos.dto.HorarioAccesoDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.HorarioAccesoUsuario;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.HorarioAccesoUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HorarioAccesoService {

    private static final int MINUTOS_MINIMOS_HORARIO = 60;

    private final HorarioAccesoUsuarioRepository horarioRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<HorarioAccesoDTO> listar(Long usuarioId) {
        return horarioRepository.findByUsuarioIdOrderByDiaSemanaAsc(usuarioId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean accesoPermitidoAhora(Usuario usuario) {
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            return false;
        }
        if (usuario.getRol() == Rol.ADMIN) {
            return true;
        }
        if (!Boolean.TRUE.equals(usuario.getHorarioAccesoHabilitado())) {
            return true;
        }
        return permitidoAhora(usuario.getId());
    }

    @Transactional(readOnly = true)
    public boolean permitidoAhora(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        HorarioAccesoUsuario horario = horarioRepository
                .findByUsuarioIdAndDiaSemana(usuarioId, ahora.getDayOfWeek())
                .orElse(null);
        if (horario == null || !Boolean.TRUE.equals(horario.getActivo())) {
            return false;
        }
        LocalTime hora = ahora.toLocalTime();
        return !hora.isBefore(horario.getHoraInicio()) && hora.isBefore(horario.getHoraFin());
    }

    @Transactional
    public List<HorarioAccesoDTO> guardar(Long usuarioId, boolean habilitado, List<HorarioAccesoDTO> horarios) {
        if (habilitado) {
            validarHorarios(horarios);
            horarioRepository.deleteByUsuarioId(usuarioId);
            List<HorarioAccesoUsuario> guardados = new ArrayList<>();
            for (HorarioAccesoDTO linea : horarios) {
                guardados.add(horarioRepository.save(HorarioAccesoUsuario.builder()
                        .usuarioId(usuarioId)
                        .diaSemana(linea.getDiaSemana())
                        .horaInicio(linea.getHoraInicio())
                        .horaFin(linea.getHoraFin())
                        .activo(Boolean.TRUE.equals(linea.getActivo()))
                        .build()));
            }
            guardados.sort(Comparator.comparing(HorarioAccesoUsuario::getDiaSemana));
            return guardados.stream().map(this::toDto).toList();
        }
        horarioRepository.deleteByUsuarioId(usuarioId);
        return List.of();
    }

    @Transactional
    public List<HorarioAccesoDTO> asegurarHorariosPorDefecto(Long usuarioId) {
        List<HorarioAccesoUsuario> existentes = horarioRepository.findByUsuarioIdOrderByDiaSemanaAsc(usuarioId);
        if (!existentes.isEmpty()) {
            return existentes.stream().map(this::toDto).toList();
        }
        List<HorarioAccesoUsuario> creados = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            boolean laboral = dia.getValue() <= DayOfWeek.FRIDAY.getValue();
            boolean sabado = dia == DayOfWeek.SATURDAY;
            creados.add(horarioRepository.save(HorarioAccesoUsuario.builder()
                    .usuarioId(usuarioId)
                    .diaSemana(dia)
                    .horaInicio(LocalTime.of(7, 0))
                    .horaFin(sabado ? LocalTime.of(14, 0) : LocalTime.of(19, 0))
                    .activo(laboral || sabado)
                    .build()));
        }
        return creados.stream().map(this::toDto).toList();
    }

    private void validarHorarios(List<HorarioAccesoDTO> horarios) {
        if (horarios == null || horarios.isEmpty()) {
            throw new ReglaNegocioException("HORARIO_INCOMPLETO", "Debe configurar los 7 días de la semana");
        }
        Set<DayOfWeek> vistos = EnumSet.noneOf(DayOfWeek.class);
        int activos = 0;
        for (HorarioAccesoDTO horario : horarios) {
            if (horario.getDiaSemana() == null) {
                throw new ReglaNegocioException("HORARIO_INVALIDO", "Cada horario debe indicar el día de la semana");
            }
            if (!vistos.add(horario.getDiaSemana())) {
                throw new ReglaNegocioException("HORARIO_DUPLICADO", "Hay más de un horario para " + horario.getDiaSemana());
            }
            if (horario.getHoraInicio() == null || horario.getHoraFin() == null) {
                throw new ReglaNegocioException("HORARIO_INVALIDO", "Debe indicar hora de inicio y fin");
            }
            if (!horario.getHoraInicio().isBefore(horario.getHoraFin())) {
                throw new ReglaNegocioException("HORARIO_INVALIDO",
                        "La hora de inicio debe ser anterior a la de fin (" + horario.getDiaSemana() + ")");
            }
            if (Boolean.TRUE.equals(horario.getActivo())) {
                activos += 1;
                long minutos = Duration.between(horario.getHoraInicio(), horario.getHoraFin()).toMinutes();
                if (minutos < MINUTOS_MINIMOS_HORARIO) {
                    throw new ReglaNegocioException(
                            "HORARIO_CORTO",
                            "El horario de " + horario.getDiaSemana() + " debe cubrir al menos "
                                    + MINUTOS_MINIMOS_HORARIO + " minutos"
                    );
                }
            }
        }
        if (vistos.size() != DayOfWeek.values().length) {
            throw new ReglaNegocioException("HORARIO_INCOMPLETO", "Debe configurar los 7 días de la semana");
        }
        if (activos == 0) {
            throw new ReglaNegocioException(
                    "HORARIO_SIN_DIAS",
                    "Con horario de acceso activo debe haber al menos un día habilitado"
            );
        }
    }

    private HorarioAccesoDTO toDto(HorarioAccesoUsuario horario) {
        return HorarioAccesoDTO.builder()
                .id(horario.getId())
                .diaSemana(horario.getDiaSemana())
                .horaInicio(horario.getHoraInicio())
                .horaFin(horario.getHoraFin())
                .activo(horario.getActivo())
                .build();
    }
}
