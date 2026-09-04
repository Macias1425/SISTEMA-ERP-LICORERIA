package com.licoreria.pos.controller;

import com.licoreria.pos.dto.ConfiguracionDTO;
import com.licoreria.pos.dto.DatosNegocioDTO;
import com.licoreria.pos.service.ConfiguracionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    @GetMapping
    @PreAuthorize("@acceso.tiene('CONFIG_GESTIONAR')")
    public ResponseEntity<ConfiguracionDTO> obtener() {
        return ResponseEntity.ok(configuracionService.obtener());
    }

    /** Encabezado del negocio para imprimir comprobantes: lo necesita cualquier operador. */
    @GetMapping("/negocio")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DatosNegocioDTO> negocio() {
        return ResponseEntity.ok(configuracionService.datosNegocio());
    }

    @PutMapping
    @PreAuthorize("@acceso.tiene('CONFIG_GESTIONAR')")
    public ResponseEntity<ConfiguracionDTO> guardar(@Valid @RequestBody ConfiguracionDTO request) {
        return ResponseEntity.ok(configuracionService.guardar(request));
    }
}
