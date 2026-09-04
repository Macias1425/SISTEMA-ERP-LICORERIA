package com.licoreria.pos.repository;

import com.licoreria.pos.model.Compra;
import com.licoreria.pos.model.EstadoCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findAllByOrderByFechaDesc();

    List<Compra> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    boolean existsByProveedorIdAndDocumentoProveedorIgnoreCaseAndEstadoIn(
            Long proveedorId,
            String documentoProveedor,
            List<EstadoCompra> estados
    );

    @Query("""
            SELECT c FROM Compra c
            WHERE (:desde IS NULL OR c.fecha >= :desde)
              AND (:hasta IS NULL OR c.fecha <= :hasta)
              AND (:estado IS NULL OR c.estado = :estado)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(c.numero) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(c.proveedorNombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(c.documentoProveedor, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                  )
            ORDER BY c.fecha DESC
            """)
    List<Compra> buscar(
            @Param("busqueda") String busqueda,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("estado") EstadoCompra estado
    );

    @Query("""
            SELECT c FROM Compra c
            WHERE (:desde IS NULL OR c.fecha >= :desde)
              AND (:hasta IS NULL OR c.fecha <= :hasta)
              AND (:estado IS NULL OR c.estado = :estado)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(c.numero) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(c.proveedorNombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(c.documentoProveedor, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                  )
            ORDER BY c.fecha DESC
            """)
    Page<Compra> buscarPaginado(
            @Param("busqueda") String busqueda,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("estado") EstadoCompra estado,
            Pageable pageable
    );
}
