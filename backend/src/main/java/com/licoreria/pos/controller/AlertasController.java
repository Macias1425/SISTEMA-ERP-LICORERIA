package com.licoreria.pos.controller;

import com.licoreria.pos.dto.AlertasResumenDTO;
import com.licoreria.pos.service.AlertasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alertas")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AlertasController {

    private final AlertasService alertasService;

    @GetMapping("/resumen")
    public ResponseEntity<AlertasResumenDTO> resumen() {
        return ResponseEntity.ok(alertasService.resumen());
    }
}
