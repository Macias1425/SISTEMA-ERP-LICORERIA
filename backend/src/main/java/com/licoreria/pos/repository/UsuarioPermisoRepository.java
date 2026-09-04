package com.licoreria.pos.repository;

import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.UsuarioPermiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioPermisoRepository extends JpaRepository<UsuarioPermiso, Long> {

    List<UsuarioPermiso> findByUsuarioIdOrderByPermisoAsc(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndPermiso(Long usuarioId, Permiso permiso);
}
