package com.licoreria.pos.controller;

import com.licoreria.pos.dto.AuditoriaDTO;
import com.licoreria.pos.dto.AuditoriaResumenDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.NivelRiesgoAuditoria;
import com.licoreria.pos.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("@acceso.tiene('AUDITORIA_VER')")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    public ResponseEntity<PaginaDTO<AuditoriaDTO>> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) AccionAuditoria accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) NivelRiesgoAuditoria nivelRiesgo,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(auditoriaService.buscar(
                busqueda, desde, hasta, accion, entidad, entidadId, usuarioId, nivelRiesgo, categoria,
                pagina, tamano));
    }

    @GetMapping("/resumen")
    public ResponseEntity<AuditoriaResumenDTO> resumen() {
        return ResponseEntity.ok(auditoriaService.resumen());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(auditoriaService.obtener(id));
    }
}
