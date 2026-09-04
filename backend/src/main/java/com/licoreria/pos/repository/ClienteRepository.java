package com.licoreria.pos.repository;

import com.licoreria.pos.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("""
            SELECT c FROM Cliente c
            WHERE (:activo IS NULL OR c.activo = :activo)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(c.ruc, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(c.telefono, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                  )
            ORDER BY c.nombre ASC
            """)
    Page<Cliente> buscarPaginado(
            @Param("busqueda") String busqueda,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
