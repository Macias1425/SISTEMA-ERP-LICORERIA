package com.licoreria.pos.repository;

import com.licoreria.pos.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByProductoIdOrderByFechaDesc(Long productoId);
}
