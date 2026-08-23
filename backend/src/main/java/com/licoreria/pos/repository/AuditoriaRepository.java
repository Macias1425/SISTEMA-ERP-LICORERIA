package com.licoreria.pos.repository;

import com.licoreria.pos.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findAllByOrderByFechaHoraDesc();

    List<Auditoria> findByEntidadAndEntidadIdOrderByFechaHoraDesc(String entidad, Long entidadId);
}
