package com.licoreria.pos.controller;

import com.licoreria.pos.dto.AlertaStockDTO;
import com.licoreria.pos.dto.AutorizacionDTO;
import com.licoreria.pos.dto.MermaRequestDTO;
import com.licoreria.pos.dto.MermaResponseDTO;
import com.licoreria.pos.dto.MovimientoInventarioDTO;
import com.licoreria.pos.model.EstadoMerma;
import com.licoreria.pos.model.MovimientoInventario;
import com.licoreria.pos.service.InventarioService;
import com.licoreria.pos.service.MermaService;
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

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;
    private final MermaService mermaService;

    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('ALMACENISTA','ADMIN','CAJERO')")
    public ResponseEntity<List<AlertaStockDTO>> alertas() {
        return ResponseEntity.ok(inventarioService.listarAlertas());
    }

    @GetMapping("/movimientos/{productoId}")
    @PreAuthorize("hasAnyRole('ALMACENISTA','ADMIN')")
    public ResponseEntity<List<MovimientoInventario>> listarMovimientos(@PathVariable Long productoId) {
        return ResponseEntity.ok(inventarioService.listarMovimientos(productoId));
    }

    @PostMapping("/movimientos")
    @PreAuthorize("hasAnyRole('ALMACENISTA','ADMIN')")
    public ResponseEntity<MovimientoInventario> registrar(@Valid @RequestBody MovimientoInventarioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.registrarMovimiento(dto));
    }

    @GetMapping("/mermas")
    @PreAuthorize("hasAnyRole('ALMACENISTA','ADMIN','CAJERO')")
    public ResponseEntity<List<MermaResponseDTO>> listarMermas(@RequestParam(required = false) EstadoMerma estado) {
        return ResponseEntity.ok(mermaService.listar(estado));
    }

    @PostMapping("/mermas")
    @PreAuthorize("hasAnyRole('ALMACENISTA','ADMIN','CAJERO')")
    public ResponseEntity<MermaResponseDTO> solicitarMerma(@Valid @RequestBody MermaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mermaService.solicitar(dto));
    }

    @PostMapping("/mermas/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ALMACENISTA','ADMIN','CAJERO')")
    public ResponseEntity<MermaResponseDTO> aprobarMerma(@PathVariable Long id,
                                                         @Valid @RequestBody AutorizacionDTO autorizacion) {
        return ResponseEntity.ok(mermaService.aprobar(id, autorizacion));
    }

    @PostMapping("/mermas/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ALMACENISTA','ADMIN','CAJERO')")
    public ResponseEntity<MermaResponseDTO> rechazarMerma(@PathVariable Long id,
                                                          @Valid @RequestBody AutorizacionDTO autorizacion) {
        return ResponseEntity.ok(mermaService.rechazar(id, autorizacion));
    }
}
