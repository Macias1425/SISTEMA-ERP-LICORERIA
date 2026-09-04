package com.licoreria.pos.controller;

import com.licoreria.pos.dto.AperturaCajaDTO;
import com.licoreria.pos.dto.CierreCajaDTO;
import com.licoreria.pos.dto.EstadoCajaDTO;
import com.licoreria.pos.dto.PaginaDTO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/caja")
@PreAuthorize("@acceso.tiene('CAJA_OPERAR')")
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

    @GetMapping("/estado")
    public ResponseEntity<EstadoCajaDTO> estado() {
        return ResponseEntity.ok(cajaService.estado());
    }

    @GetMapping("/abierta")
    public ResponseEntity<TurnoCajaDTO> abierta() {
        return ResponseEntity.ok(cajaService.abierta());
    }

    @GetMapping("/historial")
    public ResponseEntity<PaginaDTO<TurnoCajaDTO>> historial(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(cajaService.historial(pagina, tamano));
    }

    @GetMapping("/historial/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginaDTO<TurnoCajaDTO>> historialDe(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(cajaService.historialDe(usuarioId, pagina, tamano));
    }
}
