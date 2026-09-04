package com.licoreria.pos.dto;

import com.licoreria.pos.model.TipoCliente;
import jakarta.validation.constraints.NotBlank;
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
public class ClienteDTO {

    private Long id;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombre;

    private String ruc;
    private String telefono;
    private String direccion;

    @Builder.Default
    private TipoCliente tipoCliente = TipoCliente.DETAL;

    @Builder.Default
    private Boolean activo = true;
}
