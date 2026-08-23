package com.licoreria.pos.dto;

import com.licoreria.pos.model.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {

    private Long id;
    private String username;
    private String nombreCompleto;
    private Rol rol;
    private Boolean activo;
    private Boolean debeCambiarPassword;
    private LocalDateTime ultimoAcceso;
    private LocalDateTime passwordActualizadaEn;
}
