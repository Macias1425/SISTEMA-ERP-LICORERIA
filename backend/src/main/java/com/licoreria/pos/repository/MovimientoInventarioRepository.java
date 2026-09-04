package com.licoreria.pos.repository;



import com.licoreria.pos.model.MovimientoInventario;

import com.licoreria.pos.model.TipoMovimiento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;



import java.time.LocalDateTime;

import java.util.List;



public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {



    List<MovimientoInventario> findByProductoIdOrderByFechaDesc(Long productoId);



    List<MovimientoInventario> findTop80ByOrderByFechaDesc();



    @Query("""

            SELECT m FROM MovimientoInventario m

            WHERE (:productoId IS NULL OR m.productoId = :productoId)

              AND (:tipo IS NULL OR m.tipo = :tipo)

              AND (:desde IS NULL OR m.fecha >= :desde)

              AND (:hasta IS NULL OR m.fecha <= :hasta)

            ORDER BY m.fecha DESC

            """)

    List<MovimientoInventario> buscar(

            @Param("productoId") Long productoId,

            @Param("tipo") TipoMovimiento tipo,

            @Param("desde") LocalDateTime desde,

            @Param("hasta") LocalDateTime hasta

    );

    @Query("""
            SELECT m FROM MovimientoInventario m
            WHERE (:productoId IS NULL OR m.productoId = :productoId)
              AND (:tipo IS NULL OR m.tipo = :tipo)
              AND (:desde IS NULL OR m.fecha >= :desde)
              AND (:hasta IS NULL OR m.fecha <= :hasta)
            ORDER BY m.fecha DESC
            """)
    Page<MovimientoInventario> buscarPaginado(
            @Param("productoId") Long productoId,
            @Param("tipo") TipoMovimiento tipo,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );

}

