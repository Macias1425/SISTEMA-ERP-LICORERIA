package com.licoreria.pos.repository;

import com.licoreria.pos.model.Presentacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PresentacionRepository extends JpaRepository<Presentacion, Long> {

    List<Presentacion> findByProductoIdAndActivoTrue(Long productoId);

    Optional<Presentacion> findByIdAndProductoId(Long id, Long productoId);
}
