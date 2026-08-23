package com.licoreria.pos.controller;

import com.licoreria.pos.dto.CambioPasswordDTO;
import com.licoreria.pos.dto.LoginRequestDTO;
import com.licoreria.pos.dto.LoginResponseDTO;
import com.licoreria.pos.dto.UsuarioResponseDTO;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me() {
        return ResponseEntity.ok(authService.me());
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambioPasswordDTO dto) {
        authService.cambiarPassword(dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Rol>> roles() {
        return ResponseEntity.ok(Arrays.asList(Rol.values()));
    }
}
