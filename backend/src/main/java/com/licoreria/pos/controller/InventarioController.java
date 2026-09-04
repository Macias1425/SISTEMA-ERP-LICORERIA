package com.licoreria.pos.controller;

import com.licoreria.pos.dto.AlertaStockDTO;
import com.licoreria.pos.dto.AutorizacionDTO;
import com.licoreria.pos.dto.LoteDTO;
import com.licoreria.pos.dto.MermaRequestDTO;
import com.licoreria.pos.dto.MermaResponseDTO;
import com.licoreria.pos.dto.MovimientoInventarioDTO;
import com.licoreria.pos.dto.MovimientoInventarioResponseDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.model.EstadoMerma;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.service.InventarioService;
import com.licoreria.pos.service.LoteService;
import com.licoreria.pos.service.MermaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@PreAuthorize("@acceso.tiene('INVENTARIO_VER')")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;
    private final MermaService mermaService;
    private final LoteService loteService;

    @GetMapping("/alertas")
    public ResponseEntity<PaginaDTO<AlertaStockDTO>> alertas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(inventarioService.listarAlertas(pagina, tamano));
    }

    @GetMapping("/lotes/{productoId}")
    public ResponseEntity<PaginaDTO<LoteDTO>> lotesDeProducto(
            @PathVariable Long productoId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(loteService.listarPorProducto(productoId, pagina, tamano));
    }

    /** Orden sugerido de salida (FEFO) para el producto. */
    @GetMapping("/lotes/{productoId}/fefo")
    public ResponseEntity<PaginaDTO<LoteDTO>> fefo(
            @PathVariable Long productoId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(loteService.sugerenciaFefo(productoId, pagina, tamano));
    }

    @GetMapping("/lotes/por-vencer")
    public ResponseEntity<PaginaDTO<LoteDTO>> lotesPorVencer(
            @RequestParam(required = false) Integer dias,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(loteService.listarPorVencer(dias, pagina, tamano));
    }

    @GetMapping("/movimientos")
    public ResponseEntity<PaginaDTO<MovimientoInventarioResponseDTO>> listarMovimientos(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "30") int tamano
    ) {
        return ResponseEntity.ok(inventarioService.listarMovimientos(productoId, tipo, desde, hasta, pagina, tamano));
    }

    @GetMapping("/movimientos/{productoId}")
    public ResponseEntity<PaginaDTO<MovimientoInventarioResponseDTO>> listarMovimientosProducto(
            @PathVariable Long productoId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "30") int tamano
    ) {
        return ResponseEntity.ok(inventarioService.listarMovimientos(productoId, tipo, desde, hasta, pagina, tamano));
    }

    @PostMapping("/movimientos")
    @PreAuthorize("@acceso.tiene('INVENTARIO_AJUSTAR')")
    public ResponseEntity<MovimientoInventarioResponseDTO> registrar(@Valid @RequestBody MovimientoInventarioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.registrarMovimiento(dto));
    }

    @GetMapping("/mermas")
    public ResponseEntity<PaginaDTO<MermaResponseDTO>> listarMermas(
            @RequestParam(required = false) EstadoMerma estado,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(mermaService.listar(estado, productoId, desde, hasta, pagina, tamano));
    }

    @GetMapping("/mermas/{id}")
    public ResponseEntity<MermaResponseDTO> obtenerMerma(@PathVariable Long id) {
        return ResponseEntity.ok(mermaService.obtener(id));
    }

    @PostMapping("/mermas")
    @PreAuthorize("@acceso.tiene('MERMA_SOLICITAR')")
    public ResponseEntity<MermaResponseDTO> solicitarMerma(@Valid @RequestBody MermaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mermaService.solicitar(dto));
    }

    @PostMapping("/mermas/{id}/aprobar")
    @PreAuthorize("@acceso.tiene('MERMA_APROBAR')")
    public ResponseEntity<MermaResponseDTO> aprobarMerma(@PathVariable Long id,
                                                         @Valid @RequestBody AutorizacionDTO autorizacion) {
        return ResponseEntity.ok(mermaService.aprobar(id, autorizacion));
    }

    @PostMapping("/mermas/{id}/rechazar")
    @PreAuthorize("@acceso.tiene('MERMA_APROBAR')")
    public ResponseEntity<MermaResponseDTO> rechazarMerma(@PathVariable Long id,
                                                          @Valid @RequestBody AutorizacionDTO autorizacion) {
        return ResponseEntity.ok(mermaService.rechazar(id, autorizacion));
    }
}
