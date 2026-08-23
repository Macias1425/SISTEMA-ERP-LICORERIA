package com.licoreria.pos.controller;

import com.licoreria.pos.dto.EstadoNormativaDTO;
import com.licoreria.pos.service.HorarioVentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/normativa")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NormativaController {

    private final HorarioVentaService horarioVentaService;

    @GetMapping("/estado")
    public ResponseEntity<EstadoNormativaDTO> estado() {
        return ResponseEntity.ok(horarioVentaService.estadoActual());
    }
}
