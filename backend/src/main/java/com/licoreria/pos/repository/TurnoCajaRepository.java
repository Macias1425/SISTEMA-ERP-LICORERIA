package com.licoreria.pos.repository;

import com.licoreria.pos.model.EstadoTurnoCaja;
import com.licoreria.pos.model.TurnoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {

    Optional<TurnoCaja> findByUsuarioIdAndEstado(Long usuarioId, EstadoTurnoCaja estado);

    List<TurnoCaja> findByUsuarioIdOrderByFechaAperturaDesc(Long usuarioId);
}
