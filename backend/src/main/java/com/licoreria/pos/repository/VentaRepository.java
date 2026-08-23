package com.licoreria.pos.repository;

import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findByNumero(String numero);

    List<Venta> findByTurnoCajaIdAndEstado(Long turnoCajaId, EstadoVenta estado);
}
