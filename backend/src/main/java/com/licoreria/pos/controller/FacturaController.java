package com.licoreria.pos.controller;



import com.licoreria.pos.dto.AnulacionFacturaDTO;

import com.licoreria.pos.dto.FacturaDTO;

import com.licoreria.pos.dto.PaginaDTO;

import com.licoreria.pos.model.EstadoFactura;

import com.licoreria.pos.service.FacturaService;

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

@RequestMapping("/api/facturas")

@PreAuthorize("@acceso.tiene('FACTURAS_VER','VENTAS_CREAR','VENTAS_ANULAR')")

@RequiredArgsConstructor

public class FacturaController {



    private final FacturaService facturaService;



    @GetMapping

    public ResponseEntity<PaginaDTO<FacturaDTO>> listar(

            @RequestParam(required = false) EstadoFactura estado,

            @RequestParam(required = false) String busqueda,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long cajeroId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(facturaService.listar(estado, busqueda, desde, hasta, cajeroId, pagina, tamano));

    }



    @GetMapping("/{id}")

    public ResponseEntity<FacturaDTO> obtener(@PathVariable Long id) {

        return ResponseEntity.ok(facturaService.obtener(id));

    }



    @GetMapping("/venta/{ventaId}")

    public ResponseEntity<FacturaDTO> porVenta(@PathVariable Long ventaId) {

        return ResponseEntity.ok(facturaService.obtenerPorVenta(ventaId));

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

