package com.licoreria.pos.controller;

import com.licoreria.pos.dto.FinanzasPeriodoDTO;
import com.licoreria.pos.dto.MargenRiesgoResumenDTO;
import com.licoreria.pos.service.FinanzasService;
import com.licoreria.pos.service.MargenRiesgoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/finanzas")
@PreAuthorize("@acceso.tiene('FINANZAS_VER')")
@RequiredArgsConstructor
public class FinanzasController {

    private final FinanzasService finanzasService;
    private final MargenRiesgoService margenRiesgoService;

    /** Productos cuyo precio de venta ya no cubre el costo actual del inventario. */
    @GetMapping("/margen-riesgo")
    public ResponseEntity<MargenRiesgoResumenDTO> margenRiesgo(
            @RequestParam(required = false) BigDecimal objetivo,
            @RequestParam(defaultValue = "true") boolean soloRiesgo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(margenRiesgoService.analizar(objetivo, soloRiesgo, pagina, tamano));
    }

    @GetMapping("/periodo")
    public ResponseEntity<FinanzasPeriodoDTO> periodo(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) String formaPago) {
        return ResponseEntity.ok(finanzasService.periodo(desde, hasta, formaPago));
    }
}
