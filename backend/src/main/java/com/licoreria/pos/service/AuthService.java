package com.licoreria.pos.service;

import com.licoreria.pos.dto.CambioPasswordDTO;
import com.licoreria.pos.dto.LoginRequestDTO;
import com.licoreria.pos.dto.LoginResponseDTO;
import com.licoreria.pos.dto.UsuarioResponseDTO;
import com.licoreria.pos.exception.AutenticacionException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.security.CustomUserDetailsService;
import com.licoreria.pos.security.JwtService;
import com.licoreria.pos.security.LoginAttemptService;
import com.licoreria.pos.security.PoliticaPassword;
import com.licoreria.pos.security.UsuarioActualService;
import com.licoreria.pos.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final LoginAttemptService loginAttemptService;
    private final PoliticaPassword politicaPassword;
    private final UsuarioActualService usuarioActualService;
    private final PermisoService permisoService;
    private final HorarioAccesoService horarioAccesoService;
    private final Clock clock;

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        String username = request.getUsername().trim();
        if (loginAttemptService.estaBloqueado(username)) {
            throw new AutenticacionException(
                    "CUENTA_BLOQUEADA",
                    "Demasiados intentos fallidos. Espere 15 minutos o contacte al administrador"
            );
        }

        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())
                || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            loginAttemptService.registrarFallo(username);
            throw new AutenticacionException("CREDENCIALES_INVALIDAS", "Usuario o contraseña incorrectos");
        }

        loginAttemptService.registrarExito(username);

        if (!horarioAccesoService.accesoPermitidoAhora(usuario)) {
            throw new AutenticacionException(
                    "FUERA_DE_HORARIO_ACCESO",
                    "Su cuenta no puede ingresar fuera del horario de acceso configurado"
            );
        }

        usuario.setUltimoAcceso(LocalDateTime.now(clock));
        usuarioRepository.save(usuario);

        UsuarioPrincipal principal = (UsuarioPrincipal) userDetailsService.loadUserByUsername(usuario.getUsername());
        return LoginResponseDTO.builder()
                .usuarioId(usuario.getId())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .rol(usuario.getRol())
                .token(jwtService.generar(principal))
                .tipoToken("Bearer")
                .debeCambiarPassword(Boolean.TRUE.equals(usuario.getDebeCambiarPassword()))
                .build();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO me() {
        return toDto(usuarioActualService.usuario());
    }

    @Transactional
    public void cambiarPassword(CambioPasswordDTO dto) {
        Usuario usuario = usuarioActualService.usuario();
        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPassword())) {
            throw new AutenticacionException("CREDENCIALES_INVALIDAS", "La contraseña actual no es correcta");
        }
        if (!dto.getPasswordNueva().equals(dto.getConfirmacion())) {
            throw new ReglaNegocioException("PASSWORD_NO_COINCIDE", "La confirmación no coincide con la nueva contraseña");
        }
        if (passwordEncoder.matches(dto.getPasswordNueva(), usuario.getPassword())) {
            throw new ReglaNegocioException("PASSWORD_REPETIDA", "La nueva contraseña debe ser distinta a la actual");
        }
        politicaPassword.validar(dto.getPasswordNueva(), usuario.getUsername());
        usuario.setPassword(passwordEncoder.encode(dto.getPasswordNueva()));
        usuario.setDebeCambiarPassword(false);
        usuario.setPasswordActualizadaEn(LocalDateTime.now(clock));
        usuarioRepository.save(usuario);
    }

    private UsuarioResponseDTO toDto(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .debeCambiarPassword(usuario.getDebeCambiarPassword())
                .ultimoAcceso(usuario.getUltimoAcceso())
                .passwordActualizadaEn(usuario.getPasswordActualizadaEn())
                .horarioAccesoHabilitado(usuario.getHorarioAccesoHabilitado())
                .horarios(Boolean.TRUE.equals(usuario.getHorarioAccesoHabilitado())
                        ? horarioAccesoService.listar(usuario.getId())
                        : java.util.List.of())
                .accesoPermitidoAhora(horarioAccesoService.accesoPermitidoAhora(usuario))
                .permisosRol(permisoService.permisosBasePorRol(usuario.getRol()).stream().sorted().toList())
                .permisosAdicionales(permisoService.permisosAdicionales(usuario.getId()))
                .permisosEfectivos(permisoService.permisosEfectivosOrdenados(usuario))
                .build();
    }
}
