package com.licoreria.pos.controller;

import com.licoreria.pos.dto.CotizacionDTO;
import com.licoreria.pos.dto.CotizacionRequestDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.VentaRequestDTO;
import com.licoreria.pos.dto.VentaResponseDTO;
import com.licoreria.pos.service.CotizacionVentaService;
import com.licoreria.pos.service.VentaService;
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
@RequestMapping("/api/ventas")
@PreAuthorize("@acceso.tiene('VENTAS_CREAR','VENTAS_ANULAR','FACTURAS_VER')")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;
    private final CotizacionVentaService cotizacionVentaService;

    /** Precio y totales calculados por el servidor antes de cobrar: evita descuadres en pantalla. */
    @PostMapping("/cotizar")
    public ResponseEntity<CotizacionDTO> cotizar(@Valid @RequestBody CotizacionRequestDTO request) {
        return ResponseEntity.ok(cotizacionVentaService.cotizar(request));
    }

    @GetMapping
    public ResponseEntity<PaginaDTO<VentaResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(ventaService.listar(pagina, tamano));
    }

    @GetMapping("/turno/{turnoId}")
    public ResponseEntity<PaginaDTO<VentaResponseDTO>> delTurno(
            @PathVariable Long turnoId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano
    ) {
        return ResponseEntity.ok(ventaService.listarPorTurno(turnoId, pagina, tamano));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<VentaResponseDTO> registrar(@Valid @RequestBody VentaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(request));
    }
}
