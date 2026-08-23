package com.licoreria.pos.service;

import com.licoreria.pos.dto.ResetPasswordDTO;
import com.licoreria.pos.dto.UsuarioCreateDTO;
import com.licoreria.pos.dto.UsuarioResponseDTO;
import com.licoreria.pos.dto.UsuarioUpdateDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.security.PoliticaPassword;
import com.licoreria.pos.security.UsuarioActualService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaPassword politicaPassword;
    private final UsuarioActualService usuarioActualService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtener(Long id) {
        return toDto(buscar(id));
    }

    @Transactional
    public UsuarioResponseDTO crear(UsuarioCreateDTO dto) {
        String username = dto.getUsername().trim();
        if (usuarioRepository.existsByUsername(username)) {
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

        if (usuario.getId().equals(actual.getId()) && Boolean.FALSE.equals(dto.getActivo())) {
            throw new ReglaNegocioException("AUTO_BLOQUEO", "No puede desactivar su propia cuenta");
        }
        if (usuario.getRol() == Rol.ADMIN && dto.getRol() != Rol.ADMIN && esUltimoAdmin(usuario)) {
            throw new ReglaNegocioException("ULTIMO_ADMIN", "No se puede quitar el rol al último administrador activo");
        }
        if (usuario.getRol() == Rol.ADMIN && Boolean.FALSE.equals(dto.getActivo()) && esUltimoAdmin(usuario)) {
            throw new ReglaNegocioException("ULTIMO_ADMIN", "No se puede desactivar al último administrador activo");
        }

        usuario.setNombreCompleto(dto.getNombreCompleto().trim());
        usuario.setRol(dto.getRol());
        usuario.setActivo(dto.getActivo());
        return toDto(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDTO resetearPassword(Long id, ResetPasswordDTO dto) {
        Usuario usuario = buscar(id);
        politicaPassword.validar(dto.getPasswordNueva(), usuario.getUsername());
        usuario.setPassword(passwordEncoder.encode(dto.getPasswordNueva()));
        usuario.setDebeCambiarPassword(true);
        usuario.setPasswordActualizadaEn(LocalDateTime.now(clock));
        return toDto(usuarioRepository.save(usuario));
    }

    private boolean esUltimoAdmin(Usuario usuario) {
        return Boolean.TRUE.equals(usuario.getActivo())
                && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1;
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
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
                .build();
    }
}
