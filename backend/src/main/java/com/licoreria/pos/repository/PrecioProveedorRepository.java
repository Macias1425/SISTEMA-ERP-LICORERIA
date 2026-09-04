package com.licoreria.pos.repository;

import com.licoreria.pos.model.PrecioProveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrecioProveedorRepository extends JpaRepository<PrecioProveedor, Long> {

    List<PrecioProveedor> findByProveedorIdOrderByProductoIdAscPresentacionIdAsc(Long proveedorId);

    List<PrecioProveedor> findByProveedorIdAndActivoTrueOrderByProductoIdAsc(Long proveedorId);

    Page<PrecioProveedor> findByProveedorIdOrderByProductoIdAscPresentacionIdAsc(Long proveedorId, Pageable pageable);

    Page<PrecioProveedor> findByProveedorIdAndActivoTrueOrderByProductoIdAsc(Long proveedorId, Pageable pageable);

    long countByProveedorId(Long proveedorId);

    Optional<PrecioProveedor> findByProveedorIdAndProductoIdAndPresentacionId(
            Long proveedorId, Long productoId, Long presentacionId
    );

    Optional<PrecioProveedor> findByProveedorIdAndProductoIdAndPresentacionIdAndActivoTrue(
            Long proveedorId, Long productoId, Long presentacionId
    );
}
