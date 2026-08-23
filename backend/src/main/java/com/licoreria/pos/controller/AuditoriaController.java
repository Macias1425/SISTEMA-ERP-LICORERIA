package com.licoreria.pos.controller;

import com.licoreria.pos.dto.AuditoriaDTO;
import com.licoreria.pos.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    public ResponseEntity<List<AuditoriaDTO>> listar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId) {
        if (entidad != null && entidadId != null) {
            return ResponseEntity.ok(auditoriaService.listarPorEntidad(entidad, entidadId));
        }
        return ResponseEntity.ok(auditoriaService.listar());
    }
}
