package com.licoreria.pos.repository;

import com.licoreria.pos.model.ListaPrecio;
import com.licoreria.pos.model.TipoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListaPrecioRepository extends JpaRepository<ListaPrecio, Long> {

    Optional<ListaPrecio> findByProductoIdAndTipoCliente(Long productoId, TipoCliente tipoCliente);

    List<ListaPrecio> findByProductoId(Long productoId);
}
