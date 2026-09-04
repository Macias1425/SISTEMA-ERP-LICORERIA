package com.licoreria.pos.controller;

import com.licoreria.pos.dto.ControlVentaEventoDTO;
import com.licoreria.pos.dto.ControlVentasReglasDTO;
import com.licoreria.pos.dto.ControlVentasResumenDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.model.TipoEventoControlVenta;
import com.licoreria.pos.service.ControlVentasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/control-ventas")
@RequiredArgsConstructor
public class ControlVentasController {

    private final ControlVentasService controlVentasService;

    @GetMapping("/resumen")
    public ResponseEntity<ControlVentasResumenDTO> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return ResponseEntity.ok(controlVentasService.resumen(desde, hasta));
    }

    @GetMapping("/eventos")
    public ResponseEntity<PaginaDTO<ControlVentaEventoDTO>> eventos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoEventoControlVenta tipo,
            @RequestParam(required = false) Long cajeroId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(controlVentasService.eventos(desde, hasta, tipo, cajeroId, busqueda, pagina, tamano));
    }

    @GetMapping("/ventas/{id}")
    public ResponseEntity<com.licoreria.pos.dto.VentaResponseDTO> venta(@PathVariable Long id) {
        return ResponseEntity.ok(controlVentasService.detalleVenta(id));
    }

    @GetMapping("/reglas")
    public ResponseEntity<ControlVentasReglasDTO> reglas() {
        return ResponseEntity.ok(controlVentasService.reglas());
    }

    @PutMapping("/reglas")
    public ResponseEntity<ControlVentasReglasDTO> guardarReglas(@Valid @RequestBody ControlVentasReglasDTO dto) {
        return ResponseEntity.ok(controlVentasService.guardarReglas(dto));
    }
}
