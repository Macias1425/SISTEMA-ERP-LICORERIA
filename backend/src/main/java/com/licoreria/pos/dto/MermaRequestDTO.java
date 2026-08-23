package com.licoreria.pos.dto;

import com.licoreria.pos.model.TipoMerma;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class MermaRequestDTO {

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La presentación es obligatoria")
    private Long presentacionId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @NotNull(message = "El tipo de merma es obligatorio")
    private TipoMerma tipo;

    @NotBlank(message = "La justificación de la merma es obligatoria")
    @Size(min = 5, message = "La justificación debe tener al menos 5 caracteres")
    private String motivo;
}
