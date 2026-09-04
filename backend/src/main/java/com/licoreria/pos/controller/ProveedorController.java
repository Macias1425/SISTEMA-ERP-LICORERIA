package com.licoreria.pos.controller;

import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.PrecioProveedorDTO;
import com.licoreria.pos.dto.ProveedorDTO;
import com.licoreria.pos.service.ProveedorService;
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
@RequestMapping("/api/proveedores")
@PreAuthorize("@acceso.tiene('PROVEEDORES_GESTIONAR')")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public ResponseEntity<PaginaDTO<ProveedorDTO>> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(proveedorService.listar(busqueda, activo, pagina, tamano));
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ProveedorDTO>> listarActivos() {
        return ResponseEntity.ok(proveedorService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<ProveedorDTO> crear(@Valid @RequestBody ProveedorDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorDTO dto) {
        return ResponseEntity.ok(proveedorService.actualizar(id, dto));
    }

    @GetMapping("/{id}/precios")
    public ResponseEntity<PaginaDTO<PrecioProveedorDTO>> listarPrecios(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(proveedorService.listarPrecios(id, soloActivos, pagina, tamano));
    }

    @GetMapping("/{id}/precios/consulta")
    public ResponseEntity<PrecioProveedorDTO> consultarPrecio(
            @PathVariable Long id,
            @RequestParam Long productoId,
            @RequestParam Long presentacionId
    ) {
        PrecioProveedorDTO precio = proveedorService.consultarPrecio(id, productoId, presentacionId);
        if (precio == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(precio);
    }

    @PostMapping("/{id}/precios")
    public ResponseEntity<PrecioProveedorDTO> guardarPrecio(
            @PathVariable Long id,
            @Valid @RequestBody PrecioProveedorDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.guardarPrecio(id, dto));
    }
}
