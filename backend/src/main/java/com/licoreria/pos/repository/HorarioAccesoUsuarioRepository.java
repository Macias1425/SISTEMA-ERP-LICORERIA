package com.licoreria.pos.repository;

import com.licoreria.pos.model.HorarioAccesoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface HorarioAccesoUsuarioRepository extends JpaRepository<HorarioAccesoUsuario, Long> {

    List<HorarioAccesoUsuario> findByUsuarioIdOrderByDiaSemanaAsc(Long usuarioId);

    Optional<HorarioAccesoUsuario> findByUsuarioIdAndDiaSemana(Long usuarioId, DayOfWeek diaSemana);

    void deleteByUsuarioId(Long usuarioId);
}
