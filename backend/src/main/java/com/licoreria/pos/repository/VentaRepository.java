package com.licoreria.pos.repository;

import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findByNumero(String numero);

    List<Venta> findByTurnoCajaIdAndEstado(Long turnoCajaId, EstadoVenta estado);

    List<Venta> findByTurnoCajaIdOrderByFechaDesc(Long turnoCajaId);

    Page<Venta> findAllByOrderByFechaDesc(Pageable pageable);

    Page<Venta> findByTurnoCajaIdOrderByFechaDesc(Long turnoCajaId, Pageable pageable);

    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    Optional<Venta> findByClaveIdempotencia(String claveIdempotencia);

    long countByTurnoCajaIdAndEstado(Long turnoCajaId, EstadoVenta estado);

    List<Venta> findByFechaBetweenAndAutorizadoPrecioPorIsNotNullOrderByFechaDesc(
            LocalDateTime inicio, LocalDateTime fin);
}
