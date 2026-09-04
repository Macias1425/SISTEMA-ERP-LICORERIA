package com.licoreria.pos.repository;

import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByRolAndActivoTrue(Rol rol);

    @Query("""
            SELECT u FROM Usuario u
            WHERE (:activo IS NULL OR u.activo = :activo)
              AND (:rol IS NULL OR u.rol = :rol)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(u.username) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                  )
            ORDER BY u.nombreCompleto ASC, u.username ASC
            """)
    List<Usuario> buscar(@Param("busqueda") String busqueda, @Param("rol") Rol rol, @Param("activo") Boolean activo);

    @Query("""
            SELECT u FROM Usuario u
            WHERE (:activo IS NULL OR u.activo = :activo)
              AND (:rol IS NULL OR u.rol = :rol)
              AND (:rolesVacios = true OR u.rol IN :roles)
              AND (:debeCambiarPassword IS NULL OR u.debeCambiarPassword = :debeCambiarPassword)
              AND (
                    :busqueda IS NULL OR :busqueda = ''
                    OR LOWER(u.username) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                    OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                  )
            ORDER BY u.nombreCompleto ASC, u.username ASC
            """)
    Page<Usuario> buscarPaginado(
            @Param("busqueda") String busqueda,
            @Param("rol") Rol rol,
            @Param("activo") Boolean activo,
            @Param("debeCambiarPassword") Boolean debeCambiarPassword,
            @Param("rolesVacios") boolean rolesVacios,
            @Param("roles") List<Rol> roles,
            Pageable pageable
    );
}
