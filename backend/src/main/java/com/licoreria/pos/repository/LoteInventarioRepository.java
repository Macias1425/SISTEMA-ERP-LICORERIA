package com.licoreria.pos.repository;

import com.licoreria.pos.model.LoteInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteInventarioRepository extends JpaRepository<LoteInventario, Long> {

    /**
     * Orden FEFO: primero lo que vence antes; los lotes sin vencimiento van al final,
     * y entre iguales manda el ingreso más antiguo.
     */
    @Query("""
            select l from LoteInventario l
            where l.productoId = :productoId and l.cantidadDisponibleUmm > 0
            order by case when l.fechaVencimiento is null then 1 else 0 end,
                     l.fechaVencimiento asc, l.fechaIngreso asc, l.id asc
            """)
    List<LoteInventario> fefo(@Param("productoId") Long productoId);

    @Query("""
            select l from LoteInventario l
            where l.productoId = :productoId
            order by l.fechaIngreso desc, l.id desc
            """)
    List<LoteInventario> historial(@Param("productoId") Long productoId);

    @Query("""
            select l from LoteInventario l
            where l.productoId = :productoId
            order by l.fechaIngreso desc, l.id desc
            """)
    Page<LoteInventario> historialPaginado(@Param("productoId") Long productoId, Pageable pageable);

    @Query("""
            select l from LoteInventario l
            where l.productoId = :productoId and l.cantidadDisponibleUmm > 0
            order by l.fechaIngreso desc, l.id desc
            """)
    List<LoteInventario> disponiblesMasRecientes(@Param("productoId") Long productoId);

    @Query("""
            select l from LoteInventario l
            where l.cantidadDisponibleUmm > 0
              and l.fechaVencimiento is not null
              and l.fechaVencimiento <= :limite
            order by l.fechaVencimiento asc, l.id asc
            """)
    List<LoteInventario> porVencer(@Param("limite") LocalDate limite);

    @Query("""
            select l from LoteInventario l
            where l.cantidadDisponibleUmm > 0
              and l.fechaVencimiento is not null
              and l.fechaVencimiento <= :limite
            order by l.fechaVencimiento asc, l.id asc
            """)
    Page<LoteInventario> porVencerPaginado(@Param("limite") LocalDate limite, Pageable pageable);

    @Query("select coalesce(sum(l.cantidadDisponibleUmm), 0) from LoteInventario l where l.productoId = :productoId")
    int disponibleTotal(@Param("productoId") Long productoId);

    Optional<LoteInventario> findFirstByCodigo(String codigo);
}
