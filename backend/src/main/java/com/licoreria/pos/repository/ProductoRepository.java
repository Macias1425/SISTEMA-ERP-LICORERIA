package com.licoreria.pos.repository;

import com.licoreria.pos.model.Producto;
import jakarta.persistence.LockModeType;
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

    @EntityGraph(attributePaths = "presentaciones")
    List<Producto> findByActivoTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}
