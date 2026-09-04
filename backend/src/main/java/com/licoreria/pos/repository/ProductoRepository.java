package com.licoreria.pos.repository;



import com.licoreria.pos.model.Producto;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;



import java.util.List;

import java.util.Optional;



public interface ProductoRepository extends JpaRepository<Producto, Long> {



    @EntityGraph(attributePaths = "presentaciones")

    @Override

    List<Producto> findAll();



    @EntityGraph(attributePaths = "presentaciones")

    @Override

    Optional<Producto> findById(Long id);



    Optional<Producto> findByCodigo(String codigo);



    boolean existsByCodigoIgnoreCase(String codigo);



    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);



    long countByCategoriaId(Long categoriaId);



    @EntityGraph(attributePaths = "presentaciones")

    List<Producto> findByActivoTrue();



    @Lock(LockModeType.PESSIMISTIC_WRITE)

    @Query("SELECT p FROM Producto p WHERE p.id = :id")

    Optional<Producto> findByIdForUpdate(@Param("id") Long id);



    @EntityGraph(attributePaths = "presentaciones")

    @Query("""

            SELECT p FROM Producto p

            WHERE (:categoriaId IS NULL OR p.categoriaId = :categoriaId)

              AND (:activo IS NULL OR p.activo = :activo)

              AND (:esAlcoholico IS NULL OR p.esAlcoholico = :esAlcoholico)

              AND (

                    :busqueda IS NULL OR :busqueda = ''

                    OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%'))

                    OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))

                    OR LOWER(COALESCE(p.marca, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))

                  )

            ORDER BY p.nombre ASC

            """)

    List<Producto> buscar(

            @Param("busqueda") String busqueda,

            @Param("categoriaId") Long categoriaId,

            @Param("activo") Boolean activo,

            @Param("esAlcoholico") Boolean esAlcoholico

    );

    @Query(
            value = """
            SELECT p FROM Producto p
            WHERE (:categoriaId IS NULL OR p.categoriaId = :categoriaId)
              AND (:activo IS NULL OR p.activo = :activo)
              AND (:esAlcoholico IS NULL OR p.esAlcoholico = :esAlcoholico)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(p.marca, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                  )
              AND (
                    :nivelAlerta IS NULL
                    OR (
                        :nivelAlerta = 'CRITICO'
                        AND (
                            COALESCE(p.stockActual, 0) <= 0
                            OR (
                                COALESCE(p.stockCritico, 0) > 0
                                AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockCritico, 0)
                            )
                        )
                    )
                    OR (
                        :nivelAlerta = 'MINIMO'
                        AND COALESCE(p.stockActual, 0) > 0
                        AND NOT (
                            COALESCE(p.stockCritico, 0) > 0
                            AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockCritico, 0)
                        )
                        AND COALESCE(p.stockMinimo, 0) > 0
                        AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockMinimo, 0)
                    )
                    OR (
                        :nivelAlerta = 'ALERTA'
                        AND (
                            COALESCE(p.stockActual, 0) <= 0
                            OR (
                                COALESCE(p.stockCritico, 0) > 0
                                AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockCritico, 0)
                            )
                            OR (
                                COALESCE(p.stockMinimo, 0) > 0
                                AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockMinimo, 0)
                            )
                        )
                    )
                    OR (
                        :nivelAlerta = 'OK'
                        AND COALESCE(p.stockActual, 0) > 0
                        AND NOT (
                            COALESCE(p.stockCritico, 0) > 0
                            AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockCritico, 0)
                        )
                        AND (
                            COALESCE(p.stockMinimo, 0) <= 0
                            OR COALESCE(p.stockActual, 0) > COALESCE(p.stockMinimo, 0)
                        )
                    )
                  )
            ORDER BY p.nombre ASC
            """,
            countQuery = """
            SELECT count(p) FROM Producto p
            WHERE (:categoriaId IS NULL OR p.categoriaId = :categoriaId)
              AND (:activo IS NULL OR p.activo = :activo)
              AND (:esAlcoholico IS NULL OR p.esAlcoholico = :esAlcoholico)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(COALESCE(p.marca, '')) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                  )
              AND (
                    :nivelAlerta IS NULL
                    OR (
                        :nivelAlerta = 'CRITICO'
                        AND (
                            COALESCE(p.stockActual, 0) <= 0
                            OR (
                                COALESCE(p.stockCritico, 0) > 0
                                AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockCritico, 0)
                            )
                        )
                    )
                    OR (
                        :nivelAlerta = 'MINIMO'
                        AND COALESCE(p.stockActual, 0) > 0
                        AND NOT (
                            COALESCE(p.stockCritico, 0) > 0
                            AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockCritico, 0)
                        )
                        AND COALESCE(p.stockMinimo, 0) > 0
                        AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockMinimo, 0)
                    )
                    OR (
                        :nivelAlerta = 'ALERTA'
                        AND (
                            COALESCE(p.stockActual, 0) <= 0
                            OR (
                                COALESCE(p.stockCritico, 0) > 0
                                AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockCritico, 0)
                            )
                            OR (
                                COALESCE(p.stockMinimo, 0) > 0
                                AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockMinimo, 0)
                            )
                        )
                    )
                    OR (
                        :nivelAlerta = 'OK'
                        AND COALESCE(p.stockActual, 0) > 0
                        AND NOT (
                            COALESCE(p.stockCritico, 0) > 0
                            AND COALESCE(p.stockActual, 0) <= COALESCE(p.stockCritico, 0)
                        )
                        AND (
                            COALESCE(p.stockMinimo, 0) <= 0
                            OR COALESCE(p.stockActual, 0) > COALESCE(p.stockMinimo, 0)
                        )
                    )
                  )
            """
    )
    Page<Producto> buscarPaginado(
            @Param("busqueda") String busqueda,
            @Param("categoriaId") Long categoriaId,
            @Param("activo") Boolean activo,
            @Param("esAlcoholico") Boolean esAlcoholico,
            @Param("nivelAlerta") String nivelAlerta,
            Pageable pageable
    );

}

