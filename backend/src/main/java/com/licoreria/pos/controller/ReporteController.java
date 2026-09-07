package com.licoreria.pos.controller;

import com.licoreria.pos.dto.AlertaVencimientoDTO;
import com.licoreria.pos.dto.CorteDiaDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.ReportePeriodoDTO;
import com.licoreria.pos.dto.ResumenOperativoDTO;
import com.licoreria.pos.service.CorteDiaService;
import com.licoreria.pos.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("@acceso.tiene('REPORTES_VER')")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final CorteDiaService corteDiaService;

    @GetMapping("/resumen")
    public ResponseEntity<ResumenOperativoDTO> resumen() {
        return ResponseEntity.ok(reporteService.resumenDelDia());
    }

    @GetMapping("/periodo")
    public ResponseEntity<ReportePeriodoDTO> periodo(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String formaPago,
            @RequestParam(name = "estadoFactura", required = false) String estadoFactura,
            @RequestParam(required = false) String tabla,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(reporteService.periodo(
                desde, hasta, busqueda, formaPago, estadoFactura, tabla, pagina, tamano));
    }

    @GetMapping("/vencimientos")
    public ResponseEntity<PaginaDTO<AlertaVencimientoDTO>> vencimientos(
            @RequestParam(name = "dias", defaultValue = "30") int dias,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(reporteService.vencimientos(dias, busqueda, estado, pagina, tamano));
    }

    @GetMapping("/corte")
    public ResponseEntity<CorteDiaDTO> corte(@RequestParam(required = false) LocalDate fecha) {
        return ResponseEntity.ok(corteDiaService.corte(fecha));
    }

    @GetMapping("/corte.pdf")
    public ResponseEntity<byte[]> cortePdf(@RequestParam(required = false) LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : java.time.LocalDate.now();
        byte[] pdf = corteDiaService.pdf(fecha);
        String nombre = "corte-dia-" + dia + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
