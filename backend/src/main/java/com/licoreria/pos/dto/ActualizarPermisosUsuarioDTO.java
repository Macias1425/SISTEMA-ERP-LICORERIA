package com.licoreria.pos.dto;

import com.licoreria.pos.model.Permiso;
import jakarta.validation.constraints.NotNull;
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
public class ActualizarPermisosUsuarioDTO {

    @NotNull
    @Builder.Default
    private List<Permiso> permisosAdicionales = new ArrayList<>();
}
