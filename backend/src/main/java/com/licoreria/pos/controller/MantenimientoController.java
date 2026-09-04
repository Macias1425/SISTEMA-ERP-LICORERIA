package com.licoreria.pos.controller;

import com.licoreria.pos.dto.MantenimientoResumenDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.RespaldoDTO;
import com.licoreria.pos.dto.ResultadoTareaDTO;
import com.licoreria.pos.service.MantenimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/mantenimiento")
@PreAuthorize("@acceso.tiene('MANTENIMIENTO_GESTIONAR')")
@RequiredArgsConstructor
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;

    @GetMapping("/resumen")
    public ResponseEntity<MantenimientoResumenDTO> resumen() {
        return ResponseEntity.ok(mantenimientoService.resumen());
    }

    @GetMapping("/respaldos")
    public ResponseEntity<PaginaDTO<RespaldoDTO>> listarRespaldos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(mantenimientoService.listarRespaldos(pagina, tamano));
    }

    @PostMapping("/respaldos")
    public ResponseEntity<RespaldoDTO> crearRespaldo() {
        return ResponseEntity.ok(mantenimientoService.crearRespaldo());
    }

    @GetMapping("/respaldos/{nombre:.+}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable String nombre) throws IOException {
        Resource archivo = mantenimientoService.descargarRespaldo(nombre);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentLength(archivo.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(archivo);
    }

    @DeleteMapping("/respaldos/{nombre:.+}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
        mantenimientoService.eliminarRespaldo(nombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/respaldos/{nombre:.+}/restaurar")
    public ResponseEntity<ResultadoTareaDTO> restaurarExistente(
            @PathVariable String nombre,
            @RequestParam("confirmacion") String confirmacion) {
        return ResponseEntity.ok(mantenimientoService.restaurarExistente(nombre, confirmacion));
    }

    @PostMapping(value = "/restaurar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoTareaDTO> restaurar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("confirmacion") String confirmacion) {
        return ResponseEntity.ok(mantenimientoService.restaurar(archivo, confirmacion));
    }

    @PostMapping("/optimizar")
    public ResponseEntity<ResultadoTareaDTO> optimizar(
            @RequestParam(defaultValue = "false") boolean soloFragmentadas) {
        return ResponseEntity.ok(mantenimientoService.optimizarTablas(soloFragmentadas));
    }

    @PostMapping("/verificar")
    public ResponseEntity<ResultadoTareaDTO> verificar() {
        return ResponseEntity.ok(mantenimientoService.verificarTablas());
    }
}
