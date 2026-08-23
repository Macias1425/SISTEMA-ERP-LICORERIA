package com.licoreria.pos.repository;

import com.licoreria.pos.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    Optional<Factura> findByVentaId(Long ventaId);
}
