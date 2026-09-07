package com.licoreria.pos.controller;

import com.licoreria.pos.dto.CerrarCompraDTO;
import com.licoreria.pos.dto.CompraAnulacionDTO;
import com.licoreria.pos.dto.CompraDTO;
import com.licoreria.pos.dto.CompraRequestDTO;
import com.licoreria.pos.dto.CompraSugerenciaItemDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.RecepcionCompraRequestDTO;
import com.licoreria.pos.model.EstadoCompra;
import com.licoreria.pos.service.CompraService;
import com.licoreria.pos.service.CompraSugerenciaService;
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
@RequestMapping("/api/compras")
@PreAuthorize("@acceso.tiene('COMPRAS_GESTIONAR')")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;
    private final CompraSugerenciaService compraSugerenciaService;

    @GetMapping
    public ResponseEntity<PaginaDTO<CompraDTO>> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) EstadoCompra estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(compraService.listar(busqueda, desde, hasta, estado, pagina, tamano));
    }

    @GetMapping("/sugerencias")
    @PreAuthorize("@acceso.tiene('COMPRAS_GESTIONAR', 'INVENTARIO_VER')")
    public ResponseEntity<List<CompraSugerenciaItemDTO>> sugerencias(
            @RequestParam(defaultValue = "30") int diasHistorial,
            @RequestParam(defaultValue = "14") int diasCobertura
    ) {
        return ResponseEntity.ok(compraSugerenciaService.sugerir(diasHistorial, diasCobertura));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtener(id));
    }

    @PostMapping("/orden")
    public ResponseEntity<CompraDTO> crearOrden(@Valid @RequestBody CompraRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.crearOrden(request));
    }

    @PostMapping
    public ResponseEntity<CompraDTO> recibir(@Valid @RequestBody CompraRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.recibir(request));
    }

    @PostMapping("/{id}/recibir")
    public ResponseEntity<CompraDTO> recibirOrden(@PathVariable Long id,
                                                  @RequestBody(required = false) RecepcionCompraRequestDTO request) {
        return ResponseEntity.ok(compraService.recibirOrden(id, request));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<CompraDTO> cerrarOrden(@PathVariable Long id,
                                                 @Valid @RequestBody CerrarCompraDTO cierre) {
        return ResponseEntity.ok(compraService.cerrarOrden(id, cierre));
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<CompraDTO> anular(@PathVariable Long id, @Valid @RequestBody CompraAnulacionDTO anulacion) {
        return ResponseEntity.ok(compraService.anular(id, anulacion));
    }
}
