package com.licoreria.pos.repository;

import com.licoreria.pos.model.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Proveedor> findByActivoTrueOrderByNombreAsc();

    List<Proveedor> findAllByOrderByNombreAsc();

    @Query("""
            SELECT p FROM Proveedor p
            WHERE (:activo IS NULL OR p.activo = :activo)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(p.documento, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(p.contactoNombre, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                  )
            ORDER BY p.nombre ASC
            """)
    List<Proveedor> buscar(@Param("busqueda") String busqueda, @Param("activo") Boolean activo);

    @Query("""
            SELECT p FROM Proveedor p
            WHERE (:activo IS NULL OR p.activo = :activo)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(p.documento, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(p.contactoNombre, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                  )
            ORDER BY p.nombre ASC
            """)
    Page<Proveedor> buscarPaginado(
            @Param("busqueda") String busqueda,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
