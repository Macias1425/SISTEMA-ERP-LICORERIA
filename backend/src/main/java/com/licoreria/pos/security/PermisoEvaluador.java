package com.licoreria.pos.security;

import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.service.PermisoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Permite escribir la autorización HTTP en términos de permisos y no de roles:
 * {@code @PreAuthorize("@acceso.tiene('PRODUCTOS_GESTIONAR')")}.
 *
 * Filtrar por rol en el controlador anulaba los permisos adicionales concedidos a un
 * usuario: el servicio los aceptaba, pero la petición ya había sido rechazada con 403.
 */
@Slf4j
@Component("acceso")
@RequiredArgsConstructor
public class PermisoEvaluador {

    private final UsuarioActualService usuarioActualService;
    private final PermisoService permisoService;

    /** Verdadero si el usuario autenticado tiene al menos uno de los permisos indicados. */
    public boolean tiene(String... permisos) {
        Usuario usuario;
        try {
            usuario = usuarioActualService.usuario();
        } catch (RuntimeException ex) {
            return false;
        }
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            return false;
        }
        return Arrays.stream(permisos)
                .map(this::resolver)
                .filter(java.util.Objects::nonNull)
                .anyMatch(permiso -> permisoService.tienePermiso(usuario, permiso));
    }

    private Permiso resolver(String nombre) {
        try {
            return Permiso.valueOf(nombre);
        } catch (IllegalArgumentException ex) {
            log.warn("Permiso desconocido en una regla de autorización: {}", nombre);
            return null;
        }
    }
}
