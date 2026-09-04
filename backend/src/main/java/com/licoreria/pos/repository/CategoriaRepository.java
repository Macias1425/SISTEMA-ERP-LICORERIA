package com.licoreria.pos.repository;

import com.licoreria.pos.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNombre(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("""
            SELECT c FROM Categoria c
            WHERE (:activo IS NULL OR c.activo = :activo)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(c.descripcion, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                  )
            ORDER BY c.nombre ASC
            """)
    Page<Categoria> buscarPaginado(
            @Param("busqueda") String busqueda,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
