package com.licoreria.pos.repository;

import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findAllByOrderByFechaHoraDesc();

    Page<Auditoria> findAllByOrderByFechaHoraDesc(Pageable pageable);

    List<Auditoria> findByAccionOrderByFechaHoraDesc(AccionAuditoria accion);

    List<Auditoria> findByAccionAndFechaHoraBetweenOrderByFechaHoraDesc(
            AccionAuditoria accion, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
            SELECT a FROM Auditoria a
            WHERE a.accion = :accion
              AND (:desde IS NULL OR a.fechaHora >= :desde)
              AND (:hasta IS NULL OR a.fechaHora <= :hasta)
            ORDER BY a.fechaHora DESC
            """)
    List<Auditoria> findHistorialPrecios(
            @Param("accion") AccionAuditoria accion,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    long countByAccion(AccionAuditoria accion);

    List<Auditoria> findByEntidadAndEntidadIdOrderByFechaHoraDesc(String entidad, Long entidadId);

    long countByFechaHoraGreaterThanEqual(LocalDateTime inicio);

    long countByAccionIn(List<AccionAuditoria> acciones);

    @Query("""
            SELECT a FROM Auditoria a
            WHERE (:desde IS NULL OR a.fechaHora >= :desde)
              AND (:hasta IS NULL OR a.fechaHora <= :hasta)
              AND (:accion IS NULL OR a.accion = :accion)
              AND (:entidad IS NULL OR :entidad = '' OR LOWER(a.entidad) = LOWER(:entidad))
              AND (:usuarioId IS NULL OR a.usuarioId = :usuarioId)
              AND (:entidadId IS NULL OR a.entidadId = :entidadId)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(COALESCE(a.detalle, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(a.ip, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(a.entidad, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(a.valorAnterior, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(a.valorNuevo, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR CAST(a.entidadId AS string) LIKE CONCAT('%', :busqueda, '%')
                    OR a.usuarioId IN :usuarioIdsBusqueda
                  )
            ORDER BY a.fechaHora DESC
            """)
    List<Auditoria> buscar(
            @Param("busqueda") String busqueda,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("accion") AccionAuditoria accion,
            @Param("entidad") String entidad,
            @Param("entidadId") Long entidadId,
            @Param("usuarioId") Long usuarioId,
            @Param("usuarioIdsBusqueda") List<Long> usuarioIdsBusqueda
    );

    @Query("""
            SELECT a FROM Auditoria a
            WHERE (:desde IS NULL OR a.fechaHora >= :desde)
              AND (:hasta IS NULL OR a.fechaHora <= :hasta)
              AND (:accion IS NULL OR a.accion = :accion)
              AND (:entidad IS NULL OR :entidad = '' OR LOWER(a.entidad) = LOWER(:entidad))
              AND (:usuarioId IS NULL OR a.usuarioId = :usuarioId)
              AND (:entidadId IS NULL OR a.entidadId = :entidadId)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(COALESCE(a.detalle, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(a.ip, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(a.entidad, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(a.valorAnterior, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(a.valorNuevo, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR CAST(a.entidadId AS string) LIKE CONCAT('%', :busqueda, '%')
                    OR a.usuarioId IN :usuarioIdsBusqueda
                  )
            ORDER BY a.fechaHora DESC
            """)
    Page<Auditoria> buscarPaginado(
            @Param("busqueda") String busqueda,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("accion") AccionAuditoria accion,
            @Param("entidad") String entidad,
            @Param("entidadId") Long entidadId,
            @Param("usuarioId") Long usuarioId,
            @Param("usuarioIdsBusqueda") List<Long> usuarioIdsBusqueda,
            Pageable pageable
    );
}
