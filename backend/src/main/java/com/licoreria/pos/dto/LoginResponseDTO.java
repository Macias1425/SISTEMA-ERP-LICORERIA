package com.licoreria.pos.dto;

import com.licoreria.pos.model.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private Long usuarioId;
    private String username;
    private String nombreCompleto;
    private Rol rol;
    private String token;
    private String tipoToken;
    private boolean debeCambiarPassword;
}
