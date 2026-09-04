package com.licoreria.pos.dto;

import com.licoreria.pos.model.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioUpdateDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 120, message = "El nombre completo debe tener entre 3 y 120 caracteres")
    private String nombreCompleto;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;

    @NotNull(message = "El estado es obligatorio")
    private Boolean activo;

    private Boolean horarioAccesoHabilitado;

    private List<HorarioAccesoDTO> horarios;
}
