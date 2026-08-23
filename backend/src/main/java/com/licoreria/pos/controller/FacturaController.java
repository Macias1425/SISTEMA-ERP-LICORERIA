package com.licoreria.pos.controller;

import com.licoreria.pos.dto.AnulacionFacturaDTO;
import com.licoreria.pos.dto.FacturaDTO;
import com.licoreria.pos.service.FacturaService;
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
@RequestMapping("/api/facturas")
@PreAuthorize("hasAnyRole('CAJERO','ADMIN')")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;

    @GetMapping
    public ResponseEntity<List<FacturaDTO>> listar() {
        return ResponseEntity.ok(facturaService.listar());
    }

    @PostMapping("/venta/{ventaId}")
    public ResponseEntity<FacturaDTO> emitir(@PathVariable Long ventaId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaService.emitirDesdeVenta(ventaId));
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<FacturaDTO> anular(@PathVariable Long id, @Valid @RequestBody AnulacionFacturaDTO request) {
        return ResponseEntity.ok(facturaService.anular(id, request));
    }
}
