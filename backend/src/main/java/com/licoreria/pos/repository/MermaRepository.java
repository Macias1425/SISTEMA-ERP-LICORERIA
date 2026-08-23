package com.licoreria.pos.repository;

import com.licoreria.pos.model.EstadoMerma;
import com.licoreria.pos.model.Merma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MermaRepository extends JpaRepository<Merma, Long> {

    List<Merma> findByEstadoOrderByFechaSolicitudAsc(EstadoMerma estado);

    List<Merma> findAllByOrderByFechaSolicitudDesc();
}
