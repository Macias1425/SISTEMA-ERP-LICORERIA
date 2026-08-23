package com.licoreria.pos.repository;

import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRolAndActivoTrue(Rol rol);
}
