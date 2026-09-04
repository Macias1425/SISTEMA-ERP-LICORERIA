package com.licoreria.pos.controller;

import com.licoreria.pos.dto.ActualizarPermisosUsuarioDTO;
import com.licoreria.pos.dto.PermisoItemDTO;
import com.licoreria.pos.dto.PermisosRolDTO;
import com.licoreria.pos.dto.UsuarioPermisosDTO;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.service.PermisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permisos")
@PreAuthorize("@acceso.tiene('PERMISOS_GESTIONAR')")
@RequiredArgsConstructor
public class PermisoController {

    private final PermisoService permisoService;

    @GetMapping("/catalogo")
    public ResponseEntity<List<PermisoItemDTO>> catalogo() {
        return ResponseEntity.ok(permisoService.catalogo());
    }

    @GetMapping("/roles")
    public ResponseEntity<List<PermisosRolDTO>> roles() {
        return ResponseEntity.ok(permisoService.permisosPorRoles());
    }

    @GetMapping("/roles/{rol}")
    public ResponseEntity<PermisosRolDTO> rol(@PathVariable Rol rol) {
        return ResponseEntity.ok(permisoService.permisosPorRol(rol));
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioPermisosDTO> usuario(@PathVariable Long id) {
        return ResponseEntity.ok(permisoService.permisosUsuario(id));
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioPermisosDTO> actualizarUsuario(@PathVariable Long id,
                                                                @Valid @RequestBody ActualizarPermisosUsuarioDTO dto) {
        return ResponseEntity.ok(permisoService.actualizarAdicionales(id, dto));
    }
}
