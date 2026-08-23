package com.licoreria.pos.controller;

import com.licoreria.pos.dto.ListaPrecioDTO;
import com.licoreria.pos.service.ListaPrecioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/precios")
@RequiredArgsConstructor
public class ListaPrecioController {

    private final ListaPrecioService listaPrecioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CAJERO','ADMIN','ALMACENISTA')")
    public ResponseEntity<List<ListaPrecioDTO>> listar(@RequestParam Long productoId) {
        return ResponseEntity.ok(listaPrecioService.listarPorProducto(productoId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ListaPrecioDTO> guardar(@Valid @RequestBody ListaPrecioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listaPrecioService.guardar(dto));
    }
}
