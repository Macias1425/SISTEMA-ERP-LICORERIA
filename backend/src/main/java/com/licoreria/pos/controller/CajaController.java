package com.licoreria.pos.controller;

import com.licoreria.pos.dto.AperturaCajaDTO;
import com.licoreria.pos.dto.CierreCajaDTO;
import com.licoreria.pos.dto.TurnoCajaDTO;
import com.licoreria.pos.service.CajaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/caja")
@PreAuthorize("hasAnyRole('CAJERO','ADMIN')")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService cajaService;

    @PostMapping("/abrir")
    public ResponseEntity<TurnoCajaDTO> abrir(@Valid @RequestBody AperturaCajaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.abrir(dto));
    }

    @PostMapping("/{turnoId}/cerrar")
    public ResponseEntity<TurnoCajaDTO> cerrar(@PathVariable Long turnoId, @Valid @RequestBody CierreCajaDTO dto) {
        return ResponseEntity.ok(cajaService.cerrar(turnoId, dto));
    }

    @GetMapping("/abierta")
    public ResponseEntity<TurnoCajaDTO> abierta() {
        return ResponseEntity.ok(cajaService.abierta());
    }

    @GetMapping("/historial")
    public ResponseEntity<List<TurnoCajaDTO>> historial() {
        return ResponseEntity.ok(cajaService.historial());
    }

    @GetMapping("/historial/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TurnoCajaDTO>> historialDe(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(cajaService.historialDe(usuarioId));
    }
}
