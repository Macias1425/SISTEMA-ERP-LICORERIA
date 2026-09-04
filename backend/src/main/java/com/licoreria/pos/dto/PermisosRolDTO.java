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
public class PermisosRolDTO {

    private Rol rol;
    @Builder.Default
    private List<Permiso> permisos = new ArrayList<>();
}
