package com.licoreria.pos.dto;

import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioPermisosDTO {

    private Long usuarioId;
    private String username;
    private String nombreCompleto;
    private Rol rol;
    @Builder.Default
    private List<Permiso> permisosRol = new ArrayList<>();
    @Builder.Default
    private List<Permiso> permisosAdicionales = new ArrayList<>();
    @Builder.Default
    private List<Permiso> permisosEfectivos = new ArrayList<>();
}
