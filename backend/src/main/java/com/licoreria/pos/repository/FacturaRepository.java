package com.licoreria.pos.repository;

import com.licoreria.pos.model.EstadoFactura;
import com.licoreria.pos.model.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    Optional<Factura> findByVentaId(Long ventaId);

    List<Factura> findAllByOrderByFechaEmisionDesc();

    List<Factura> findByFechaEmisionBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("""
            SELECT f FROM Factura f
            WHERE (:estado IS NULL OR f.estado = :estado)
              AND (:desde IS NULL OR f.fechaEmision >= :desde)
              AND (:hasta IS NULL OR f.fechaEmision <= :hasta)
              AND (
                    :cajeroId IS NULL
                    OR f.ventaId IN (SELECT v.id FROM Venta v WHERE v.usuarioId = :cajeroId)
                  )
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(f.numero) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(f.clienteNombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(f.clienteRuc, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR f.ventaId IN (
                        SELECT v2.id FROM Venta v2 WHERE v2.usuarioId IN :usuarioIdsBusqueda
                    )
                  )
            ORDER BY f.fechaEmision DESC
            """)
    List<Factura> buscar(
            @Param("estado") EstadoFactura estado,
            @Param("busqueda") String busqueda,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("cajeroId") Long cajeroId,
            @Param("usuarioIdsBusqueda") List<Long> usuarioIdsBusqueda
    );

    @Query("""
            SELECT f FROM Factura f
            WHERE (:estado IS NULL OR f.estado = :estado)
              AND (:desde IS NULL OR f.fechaEmision >= :desde)
              AND (:hasta IS NULL OR f.fechaEmision <= :hasta)
              AND (
                    :cajeroId IS NULL
                    OR f.ventaId IN (SELECT v.id FROM Venta v WHERE v.usuarioId = :cajeroId)
                  )
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(f.numero) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(f.clienteNombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(f.clienteRuc, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR f.ventaId IN (
                        SELECT v2.id FROM Venta v2 WHERE v2.usuarioId IN :usuarioIdsBusqueda
                    )
                  )
            ORDER BY f.fechaEmision DESC
            """)
    Page<Factura> buscarPaginado(
            @Param("estado") EstadoFactura estado,
            @Param("busqueda") String busqueda,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("cajeroId") Long cajeroId,
            @Param("usuarioIdsBusqueda") List<Long> usuarioIdsBusqueda,
            Pageable pageable
    );
}
