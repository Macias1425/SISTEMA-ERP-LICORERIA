package com.licoreria.pos.controller;

import com.licoreria.pos.dto.CategoriaDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginaDTO<CategoriaDTO>> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(categoriaService.listar(busqueda, activo, pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoriaDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("@acceso.tiene('CATEGORIAS_GESTIONAR')")
    public ResponseEntity<CategoriaDTO> crear(@Valid @RequestBody CategoriaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@acceso.tiene('CATEGORIAS_GESTIONAR')")
    public ResponseEntity<CategoriaDTO> actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaDTO dto) {
        return ResponseEntity.ok(categoriaService.actualizar(id, dto));
    }
}
