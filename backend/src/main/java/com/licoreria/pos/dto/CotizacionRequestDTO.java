package com.licoreria.pos.dto;

import com.licoreria.pos.model.TipoCliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class CotizacionRequestDTO {

    private TipoCliente tipoCliente;

    @Valid
    @NotEmpty(message = "La cotización requiere al menos una línea")
    @Builder.Default
    private List<DetalleVentaDTO> detalles = new ArrayList<>();
}
