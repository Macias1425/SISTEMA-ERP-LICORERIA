package com.licoreria.pos.controller;

import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.ResetPasswordDTO;
import com.licoreria.pos.dto.UsuarioCreateDTO;
import com.licoreria.pos.dto.UsuarioResponseDTO;
import com.licoreria.pos.dto.UsuarioUpdateDTO;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("@acceso.tiene('USUARIOS_VER','USUARIOS_GESTIONAR')")
    public ResponseEntity<PaginaDTO<UsuarioResponseDTO>> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean debeCambiarPassword,
            @RequestParam(required = false) List<Rol> roles,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(usuarioService.listar(
                busqueda, rol, activo, debeCambiarPassword, roles, pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@acceso.tiene('USUARIOS_VER','USUARIOS_GESTIONAR')")
    public ResponseEntity<UsuarioResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("@acceso.tiene('USUARIOS_GESTIONAR')")
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@acceso.tiene('USUARIOS_GESTIONAR')")
    public ResponseEntity<UsuarioResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizar(id, dto));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("@acceso.tiene('USUARIOS_GESTIONAR')")
    public ResponseEntity<UsuarioResponseDTO> resetearPassword(@PathVariable Long id,
                                                               @Valid @RequestBody ResetPasswordDTO dto) {
        return ResponseEntity.ok(usuarioService.resetearPassword(id, dto));
    }
}
