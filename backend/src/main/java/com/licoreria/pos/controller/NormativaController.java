package com.licoreria.pos.controller;

import com.licoreria.pos.dto.ControlVentasReglasDTO;
import com.licoreria.pos.dto.EstadoNormativaDTO;
import com.licoreria.pos.service.ControlVentasService;
import com.licoreria.pos.service.HorarioVentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/normativa")
@RequiredArgsConstructor
public class NormativaController {

    private final HorarioVentaService horarioVentaService;
    private final ControlVentasService controlVentasService;

    @GetMapping("/estado")
    public ResponseEntity<EstadoNormativaDTO> estado() {
        EstadoNormativaDTO base = horarioVentaService.estadoActual();
        ControlVentasReglasDTO reglas = controlVentasService.reglasOperativas();
        base.setMontoSupervisorRequerido(reglas.getMontoSupervisorRequerido());
        base.setMaxVentasPorTurno(reglas.getMaxVentasPorTurno());
        return ResponseEntity.ok(base);
    }
}
