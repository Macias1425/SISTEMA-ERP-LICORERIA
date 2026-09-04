package com.licoreria.pos.repository;



import com.licoreria.pos.model.EstadoMerma;

import com.licoreria.pos.model.Merma;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;



import java.time.LocalDateTime;

import java.util.List;



public interface MermaRepository extends JpaRepository<Merma, Long> {



    List<Merma> findByEstadoOrderByFechaSolicitudAsc(EstadoMerma estado);



    List<Merma> findAllByOrderByFechaSolicitudDesc();



    @Query("""

            SELECT m FROM Merma m

            WHERE (:estado IS NULL OR m.estado = :estado)

              AND (:productoId IS NULL OR m.productoId = :productoId)

              AND (:desde IS NULL OR m.fechaSolicitud >= :desde)

              AND (:hasta IS NULL OR m.fechaSolicitud <= :hasta)

            ORDER BY m.fechaSolicitud DESC

            """)

    List<Merma> buscar(

            @Param("estado") EstadoMerma estado,

            @Param("productoId") Long productoId,

            @Param("desde") LocalDateTime desde,

            @Param("hasta") LocalDateTime hasta

    );

    @Query("""
            SELECT m FROM Merma m
            WHERE (:estado IS NULL OR m.estado = :estado)
              AND (:productoId IS NULL OR m.productoId = :productoId)
              AND (:desde IS NULL OR m.fechaSolicitud >= :desde)
              AND (:hasta IS NULL OR m.fechaSolicitud <= :hasta)
            ORDER BY m.fechaSolicitud DESC
            """)
    Page<Merma> buscarPaginado(
            @Param("estado") EstadoMerma estado,
            @Param("productoId") Long productoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );

}

