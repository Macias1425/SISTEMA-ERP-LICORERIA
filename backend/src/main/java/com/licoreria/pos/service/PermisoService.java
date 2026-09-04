package com.licoreria.pos.service;

import com.licoreria.pos.dto.ActualizarPermisosUsuarioDTO;
import com.licoreria.pos.dto.PermisoItemDTO;
import com.licoreria.pos.dto.PermisosRolDTO;
import com.licoreria.pos.dto.UsuarioPermisosDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.model.UsuarioPermiso;
import com.licoreria.pos.repository.UsuarioPermisoRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermisoService {

    private static final EnumSet<Permiso> ADMIN = EnumSet.allOf(Permiso.class);

    /** El cajero hereda solo POS + caja; todo lo demás se concede como permiso adicional. */
    private static final EnumSet<Permiso> CAJERO = EnumSet.of(
            Permiso.VENTAS_CREAR,
            Permiso.CAJA_OPERAR
    );

    private static final EnumSet<Permiso> ALMACENISTA = EnumSet.of(
            Permiso.DASHBOARD_VER,
            Permiso.REPORTES_VER,
            Permiso.FINANZAS_VER,
            Permiso.PRODUCTOS_VER,
            Permiso.PRODUCTOS_GESTIONAR,
            Permiso.CATEGORIAS_GESTIONAR,
            Permiso.PROVEEDORES_GESTIONAR,
            Permiso.MARCAS_PRECIOS_GESTIONAR,
            Permiso.INVENTARIO_VER,
            Permiso.INVENTARIO_AJUSTAR,
            Permiso.COMPRAS_GESTIONAR,
            Permiso.MERMA_SOLICITAR
    );

    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacionService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<PermisoItemDTO> catalogo() {
        exigirPermiso(autorizacionService.operadorActual(), Permiso.PERMISOS_GESTIONAR);
        return Arrays.stream(Permiso.values())
                .sorted(Comparator.comparing(Permiso::getModulo).thenComparing(Permiso::getEtiqueta))
                .map(this::toItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermisosRolDTO> permisosPorRoles() {
        exigirPermiso(autorizacionService.operadorActual(), Permiso.PERMISOS_GESTIONAR);
        return Arrays.stream(Rol.values())
                .map(rol -> PermisosRolDTO.builder()
                        .rol(rol)
                        .permisos(ordenados(permisosBasePorRol(rol)))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public PermisosRolDTO permisosPorRol(Rol rol) {
        exigirPermiso(autorizacionService.operadorActual(), Permiso.PERMISOS_GESTIONAR);
        return PermisosRolDTO.builder()
                .rol(rol)
                .permisos(ordenados(permisosBasePorRol(rol)))
                .build();
    }

    @Transactional(readOnly = true)
    public UsuarioPermisosDTO permisosUsuario(Long usuarioId) {
        exigirPermiso(autorizacionService.operadorActual(), Permiso.PERMISOS_GESTIONAR);
        Usuario usuario = buscarUsuario(usuarioId);
        return toUsuarioPermisos(usuario);
    }

    @Transactional
    public UsuarioPermisosDTO actualizarAdicionales(Long usuarioId, ActualizarPermisosUsuarioDTO dto) {
        Usuario admin = autorizacionService.operadorActual();
        exigirPermiso(admin, Permiso.PERMISOS_GESTIONAR);
        Usuario usuario = buscarUsuario(usuarioId);
        List<Permiso> solicitados = dto.getPermisosAdicionales() == null
                ? List.of()
                : dto.getPermisosAdicionales();
        validarAdicionales(usuario, solicitados);

        List<Permiso> antes = permisosAdicionales(usuarioId);
        usuarioPermisoRepository.deleteByUsuarioId(usuarioId);
        for (Permiso permiso : solicitados) {
            usuarioPermisoRepository.save(UsuarioPermiso.builder()
                    .usuarioId(usuarioId)
                    .permiso(permiso)
                    .build());
        }

        auditoriaService.registrar(admin, AccionAuditoria.CONFIGURACION, "UsuarioPermisos", usuarioId,
                resumenLista(antes), resumenLista(solicitados),
                "Permisos adicionales para " + usuario.getUsername());
        return toUsuarioPermisos(usuario);
    }

    @Transactional(readOnly = true)
    public Set<Permiso> permisosBasePorRol(Rol rol) {
        if (rol == null) {
            return EnumSet.noneOf(Permiso.class);
        }
        return switch (rol) {
            case ADMIN -> EnumSet.copyOf(ADMIN);
            case CAJERO -> EnumSet.copyOf(CAJERO);
            case ALMACENISTA -> EnumSet.copyOf(ALMACENISTA);
        };
    }

    @Transactional(readOnly = true)
    public List<Permiso> permisosAdicionales(Long usuarioId) {
        return usuarioPermisoRepository.findByUsuarioIdOrderByPermisoAsc(usuarioId).stream()
                .map(UsuarioPermiso::getPermiso)
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<Permiso> permisosEfectivos(Usuario usuario) {
        if (usuario == null) {
            return EnumSet.noneOf(Permiso.class);
        }
        Set<Permiso> efectivos = new LinkedHashSet<>(permisosBasePorRol(usuario.getRol()));
        efectivos.addAll(permisosAdicionales(usuario.getId()));
        return efectivos;
    }

    @Transactional(readOnly = true)
    public List<Permiso> permisosEfectivosOrdenados(Usuario usuario) {
        return ordenados(permisosEfectivos(usuario));
    }

    @Transactional(readOnly = true)
    public boolean tienePermiso(Usuario usuario, Permiso permiso) {
        if (usuario == null || permiso == null) {
            return false;
        }
        return permisosEfectivos(usuario).contains(permiso);
    }

    public void exigirPermiso(Usuario usuario, Permiso permiso) {
        if (!tienePermiso(usuario, permiso)) {
            throw new ReglaNegocioException("PERMISO_DENEGADO", "No tiene permiso para: " + permiso.getEtiqueta());
        }
    }

    private void validarAdicionales(Usuario usuario, List<Permiso> solicitados) {
        Set<Permiso> unicos = EnumSet.noneOf(Permiso.class);
        for (Permiso permiso : solicitados) {
            if (permiso == null) {
                throw new ReglaNegocioException("PERMISO_INVALIDO", "Hay un permiso inválido en la solicitud");
            }
            if (!unicos.add(permiso)) {
                throw new ReglaNegocioException("PERMISO_DUPLICADO", "El permiso " + permiso + " está repetido");
            }
            if (permisosBasePorRol(usuario.getRol()).contains(permiso)) {
                throw new ReglaNegocioException(
                        "PERMISO_REDUNDANTE",
                        "El permiso " + permiso.getEtiqueta() + " ya lo otorga el rol " + usuario.getRol()
                );
            }
        }
    }

    private UsuarioPermisosDTO toUsuarioPermisos(Usuario usuario) {
        List<Permiso> rol = ordenados(permisosBasePorRol(usuario.getRol()));
        List<Permiso> adicionales = permisosAdicionales(usuario.getId());
        return UsuarioPermisosDTO.builder()
                .usuarioId(usuario.getId())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .rol(usuario.getRol())
                .permisosRol(rol)
                .permisosAdicionales(adicionales)
                .permisosEfectivos(ordenados(permisosEfectivos(usuario)))
                .build();
    }

    private PermisoItemDTO toItem(Permiso permiso) {
        return PermisoItemDTO.builder()
                .codigo(permiso)
                .etiqueta(permiso.getEtiqueta())
                .modulo(permiso.getModulo())
                .build();
    }

    private List<Permiso> ordenados(Set<Permiso> permisos) {
        return permisos.stream()
                .sorted(Comparator.comparing(Permiso::getModulo).thenComparing(Permiso::getEtiqueta))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String resumenLista(List<Permiso> permisos) {
        if (permisos == null || permisos.isEmpty()) {
            return "[]";
        }
        return permisos.stream().map(Permiso::name).collect(Collectors.joining(",", "[", "]"));
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }
}
