package com.licoreria.pos.service;

import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Punto único de control de acceso por permiso efectivo (rol base + adicionales).
 * Complementa {@link AutorizacionService}, que resuelve identidad y credenciales de administrador.
 */
@Service
@RequiredArgsConstructor
public class AccesoService {

    private final AutorizacionService autorizacionService;
    private final PermisoService permisoService;

    /** Exige que el operador autenticado tenga el permiso indicado. */
    public Usuario exigirPermiso(Permiso permiso) {
        Usuario operador = autorizacionService.operadorActual();
        permisoService.exigirPermiso(operador, permiso);
        return operador;
    }

    /** Exige al menos uno de los permisos indicados (accesos con más de una vía válida). */
    public Usuario exigirAlguno(Permiso... permisos) {
        Usuario operador = autorizacionService.operadorActual();
        boolean autorizado = Arrays.stream(permisos)
                .anyMatch(permiso -> permisoService.tienePermiso(operador, permiso));
        if (!autorizado) {
            throw new ReglaNegocioException("PERMISO_DENEGADO", "No tiene permiso para: " + etiquetas(permisos));
        }
        return operador;
    }

    /** Verifica un permiso sobre un usuario concreto (ej. supervisor que autoriza una excepción). */
    public void exigirPermisoDe(Usuario usuario, Permiso permiso, String mensaje) {
        if (!permisoService.tienePermiso(usuario, permiso)) {
            throw new ReglaNegocioException("PERMISO_DENEGADO", mensaje);
        }
    }

    public boolean tienePermiso(Usuario usuario, Permiso permiso) {
        return permisoService.tienePermiso(usuario, permiso);
    }

    private String etiquetas(Permiso... permisos) {
        return Arrays.stream(permisos)
                .map(Permiso::getEtiqueta)
                .collect(Collectors.joining(" o "));
    }
}
