package com.licoreria.pos.controller;

import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.PresentacionDTO;
import com.licoreria.pos.dto.ProductoDTO;
import com.licoreria.pos.model.FiltroAlertaStock;
import com.licoreria.pos.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La autorización fina vive en {@link ProductoService} (permisos efectivos por usuario).
 * Aquí solo se exige sesión: si el controlador filtrara por rol, los permisos
 * adicionales que un administrador concede a un usuario nunca surtirían efecto.
 */
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginaDTO<ProductoDTO>> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean esAlcoholico,
            @RequestParam(required = false) FiltroAlertaStock nivelAlerta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(productoService.listar(
                busqueda, categoriaId, activo, esAlcoholico, nivelAlerta, pagina, tamano));
    }

    @GetMapping("/pos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginaDTO<ProductoDTO>> listarPos(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return ResponseEntity.ok(productoService.listarParaPos(busqueda, categoriaId, pagina, tamano));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductoDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductoDTO> crear(@Valid @RequestBody ProductoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductoDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoDTO dto) {
        return ResponseEntity.ok(productoService.actualizar(id, dto));
    }

    @PostMapping("/{id}/presentaciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresentacionDTO> agregarPresentacion(@PathVariable Long id,
                                                               @Valid @RequestBody PresentacionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.agregarPresentacion(id, dto));
    }

    @PutMapping("/{id}/presentaciones/{presentacionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresentacionDTO> actualizarPresentacion(@PathVariable Long id,
                                                                 @PathVariable Long presentacionId,
                                                                 @Valid @RequestBody PresentacionDTO dto) {
        return ResponseEntity.ok(productoService.actualizarPresentacion(id, presentacionId, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
