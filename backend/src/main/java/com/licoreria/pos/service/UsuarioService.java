package com.licoreria.pos.service;

import com.licoreria.pos.dto.HorarioAccesoDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.ResetPasswordDTO;
import com.licoreria.pos.dto.UsuarioCreateDTO;
import com.licoreria.pos.dto.UsuarioResponseDTO;
import com.licoreria.pos.dto.UsuarioUpdateDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.EstadoTurnoCaja;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.TurnoCajaRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.security.PoliticaPassword;
import com.licoreria.pos.security.UsuarioActualService;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaPassword politicaPassword;
    private final UsuarioActualService usuarioActualService;
    private final PermisoService permisoService;
    private final HorarioAccesoService horarioAccesoService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PaginaDTO<UsuarioResponseDTO> listar(
            String busqueda,
            Rol rol,
            Boolean activo,
            Boolean debeCambiarPassword,
            List<Rol> roles,
            int pagina,
            int tamano
    ) {
        String termino = busqueda == null || busqueda.isBlank() ? null : busqueda.trim();
        boolean rolesVacios = roles == null || roles.isEmpty();
        List<Rol> rolesParam = rolesVacios ? List.of(Rol.ADMIN) : roles;
        return PaginaDTO.de(usuarioRepository.buscarPaginado(
                termino,
                rol,
                activo,
                debeCambiarPassword,
                rolesVacios,
                rolesParam,
                PaginacionUtil.pageable(pagina, tamano)
        ).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar(String busqueda, Rol rol, Boolean activo) {
        return listar(busqueda, rol, activo, null, null, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtener(Long id) {
        return toDto(buscar(id));
    }

    @Transactional
    public UsuarioResponseDTO crear(UsuarioCreateDTO dto) {
        String username = normalizarUsername(dto.getUsername());
        validarUsername(username);
        validarNombreCompleto(dto.getNombreCompleto());
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new ReglaNegocioException("USUARIO_DUPLICADO", "Ya existe el usuario " + username);
        }
        politicaPassword.validar(dto.getPassword(), username);

        Usuario usuario = Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(dto.getPassword()))
                .nombreCompleto(dto.getNombreCompleto().trim())
                .rol(dto.getRol())
                .activo(true)
                .debeCambiarPassword(true)
                .passwordActualizadaEn(LocalDateTime.now(clock))
                .build();
        return toDto(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDTO actualizar(Long id, UsuarioUpdateDTO dto) {
        Usuario usuario = buscar(id);
        Usuario actual = usuarioActualService.usuario();
        validarNombreCompleto(dto.getNombreCompleto());

        boolean esSesion = usuario.getId().equals(actual.getId());
        if (esSesion && Boolean.FALSE.equals(dto.getActivo())) {
            throw new ReglaNegocioException("AUTO_BLOQUEO", "No puede desactivar su propia cuenta");
        }
        if (esSesion && usuario.getRol() == Rol.ADMIN && dto.getRol() != Rol.ADMIN && esUltimoAdmin(usuario)) {
            throw new ReglaNegocioException("ULTIMO_ADMIN", "No puede quitarse el rol de administrador si es el único activo");
        }
        if (usuario.getRol() == Rol.ADMIN && dto.getRol() != Rol.ADMIN && esUltimoAdmin(usuario)) {
            throw new ReglaNegocioException("ULTIMO_ADMIN", "No se puede quitar el rol al último administrador activo");
        }
        if (usuario.getRol() == Rol.ADMIN && Boolean.FALSE.equals(dto.getActivo()) && esUltimoAdmin(usuario)) {
            throw new ReglaNegocioException("ULTIMO_ADMIN", "No se puede desactivar al último administrador activo");
        }
        if (Boolean.FALSE.equals(dto.getActivo()) && Boolean.TRUE.equals(usuario.getActivo())) {
            validarDesactivacion(usuario);
        }

        usuario.setNombreCompleto(dto.getNombreCompleto().trim());
        usuario.setRol(dto.getRol());
        usuario.setActivo(dto.getActivo());

        if (dto.getHorarioAccesoHabilitado() != null) {
            boolean habilitado = Boolean.TRUE.equals(dto.getHorarioAccesoHabilitado());
            usuario.setHorarioAccesoHabilitado(habilitado);
            if (habilitado) {
                List<HorarioAccesoDTO> horarios = dto.getHorarios();
                if (horarios == null || horarios.isEmpty()) {
                    horarios = horarioAccesoService.asegurarHorariosPorDefecto(usuario.getId());
                }
                horarioAccesoService.guardar(usuario.getId(), true, horarios);
            } else {
                horarioAccesoService.guardar(usuario.getId(), false, List.of());
            }
        } else if (dto.getHorarios() != null && Boolean.TRUE.equals(usuario.getHorarioAccesoHabilitado())) {
            horarioAccesoService.guardar(usuario.getId(), true, dto.getHorarios());
        }

        return toDto(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDTO resetearPassword(Long id, ResetPasswordDTO dto) {
        Usuario usuario = buscar(id);
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ReglaNegocioException("USUARIO_INACTIVO", "No se puede resetear la contraseña de un usuario bloqueado");
        }
        politicaPassword.validar(dto.getPasswordNueva(), usuario.getUsername());
        usuario.setPassword(passwordEncoder.encode(dto.getPasswordNueva()));
        usuario.setDebeCambiarPassword(true);
        usuario.setPasswordActualizadaEn(LocalDateTime.now(clock));
        return toDto(usuarioRepository.save(usuario));
    }

    private void validarDesactivacion(Usuario usuario) {
        if (turnoCajaRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoTurnoCaja.ABIERTO).isPresent()) {
            throw new ReglaNegocioException(
                    "CAJA_ABIERTA",
                    "No se puede bloquear un usuario con turno de caja abierto. Debe cerrar caja primero"
            );
        }
    }

    private String normalizarUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private void validarUsername(String username) {
        if (username.length() < 3 || username.length() > 80 || !username.matches("^[a-z0-9._-]+$")) {
            throw new ReglaNegocioException(
                    "USERNAME_INVALIDO",
                    "El usuario debe tener 3-80 caracteres en minúsculas (letras, números, punto, guion o guion bajo)"
            );
        }
    }

    private void validarNombreCompleto(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.trim().length() < 3) {
            throw new ReglaNegocioException("NOMBRE_INVALIDO", "El nombre completo debe tener al menos 3 caracteres");
        }
    }

    private boolean esUltimoAdmin(Usuario usuario) {
        return Boolean.TRUE.equals(usuario.getActivo())
                && usuario.getRol() == Rol.ADMIN
                && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1;
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }

    private UsuarioResponseDTO toDto(Usuario usuario) {
        Usuario actual = usuarioActualService.usuario();
        boolean esSesion = actual != null && usuario.getId().equals(actual.getId());
        boolean ultimoAdmin = esUltimoAdmin(usuario);
        boolean turnoAbierto = turnoCajaRepository
                .findByUsuarioIdAndEstado(usuario.getId(), EstadoTurnoCaja.ABIERTO)
                .isPresent();
        boolean puedeDesactivar = Boolean.TRUE.equals(usuario.getActivo())
                && !esSesion
                && !ultimoAdmin
                && !turnoAbierto;
        boolean puedeCambiarRol = !esSesion || !ultimoAdmin || usuario.getRol() != Rol.ADMIN;

        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .debeCambiarPassword(usuario.getDebeCambiarPassword())
                .ultimoAcceso(usuario.getUltimoAcceso())
                .passwordActualizadaEn(usuario.getPasswordActualizadaEn())
                .esSesionActual(esSesion)
                .puedeDesactivar(puedeDesactivar)
                .puedeCambiarRol(puedeCambiarRol)
                .turnoCajaAbierto(turnoAbierto)
                .horarioAccesoHabilitado(usuario.getHorarioAccesoHabilitado())
                .horarios(Boolean.TRUE.equals(usuario.getHorarioAccesoHabilitado())
                        ? horarioAccesoService.listar(usuario.getId())
                        : List.of())
                .accesoPermitidoAhora(horarioAccesoService.accesoPermitidoAhora(usuario))
                .permisosRol(permisoService.permisosBasePorRol(usuario.getRol()).stream().sorted().toList())
                .permisosAdicionales(permisoService.permisosAdicionales(usuario.getId()))
                .permisosEfectivos(permisoService.permisosEfectivosOrdenados(usuario))
                .build();
    }
}
