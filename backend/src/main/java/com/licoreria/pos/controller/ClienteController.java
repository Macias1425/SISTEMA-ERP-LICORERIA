package com.licoreria.pos.controller;



import com.licoreria.pos.dto.ClienteDTO;

import com.licoreria.pos.dto.PaginaDTO;

import com.licoreria.pos.service.ClienteService;

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



@RestController

@RequestMapping("/api/clientes")

@RequiredArgsConstructor

public class ClienteController {



    private final ClienteService clienteService;



    @GetMapping

    @PreAuthorize("@acceso.tiene('CLIENTES_GESTIONAR', 'VENTAS_CREAR')")

    public ResponseEntity<PaginaDTO<ClienteDTO>> listar(

            @RequestParam(required = false) String busqueda,

            @RequestParam(required = false) Boolean activo,

            @RequestParam(defaultValue = "0") int pagina,

            @RequestParam(defaultValue = "20") int tamano

    ) {

        return ResponseEntity.ok(clienteService.listar(busqueda, activo, pagina, tamano));

    }



    @GetMapping("/{id}")

    @PreAuthorize("@acceso.tiene('CLIENTES_GESTIONAR', 'VENTAS_CREAR')")

    public ResponseEntity<ClienteDTO> obtener(@PathVariable Long id) {

        return ResponseEntity.ok(clienteService.obtener(id));

    }



    @PostMapping

    @PreAuthorize("@acceso.tiene('CLIENTES_GESTIONAR')")

    public ResponseEntity<ClienteDTO> crear(@Valid @RequestBody ClienteDTO dto) {

        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(dto));

    }



    @PutMapping("/{id}")

    @PreAuthorize("@acceso.tiene('CLIENTES_GESTIONAR')")

    public ResponseEntity<ClienteDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteDTO dto) {

        return ResponseEntity.ok(clienteService.actualizar(id, dto));

    }

}

