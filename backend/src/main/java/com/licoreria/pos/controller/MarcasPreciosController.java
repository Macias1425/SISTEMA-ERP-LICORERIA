package com.licoreria.pos.controller;

import com.licoreria.pos.dto.HistorialPrecioDTO;
import com.licoreria.pos.dto.MarcaResumenDTO;
import com.licoreria.pos.dto.MarcasPreciosResumenDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.ProductoPrecioCatalogoDTO;
import com.licoreria.pos.dto.RenombrarMarcaDTO;
import com.licoreria.pos.service.MarcasPreciosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marcas-precios")
@PreAuthorize("@acceso.tiene('MARCAS_PRECIOS_GESTIONAR')")
@RequiredArgsConstructor
public class MarcasPreciosController {

    private final MarcasPreciosService marcasPreciosService;

    @GetMapping("/resumen")
    public ResponseEntity<MarcasPreciosResumenDTO> resumen() {
        return ResponseEntity.ok(marcasPreciosService.resumen());
    }

    @GetMapping("/marcas")
    public ResponseEntity<PaginaDTO<MarcaResumenDTO>> marcas(
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(marcasPreciosService.listarMarcas(busqueda, pagina, tamano));
    }

    @GetMapping("/historial")
    public ResponseEntity<PaginaDTO<HistorialPrecioDTO>> historial(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String tipoCambio,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(marcasPreciosService.historial(
                desde, hasta, busqueda, marca, productoId, tipoCambio, pagina, tamano));
    }

    @GetMapping("/catalogo")
    public ResponseEntity<PaginaDTO<ProductoPrecioCatalogoDTO>> catalogo(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(marcasPreciosService.catalogoPrecios(busqueda, marca, activo, pagina, tamano));
    }

    @PostMapping("/marcas/renombrar")
    public ResponseEntity<Map<String, Object>> renombrar(@RequestBody RenombrarMarcaDTO dto) {
        int actualizados = marcasPreciosService.renombrarMarca(dto.getMarcaActual(), dto.getMarcaNueva());
        return ResponseEntity.ok(Map.of("actualizados", actualizados));
    }
}
