package com.licoreria.pos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class ProveedorDTO {

    private Long id;

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 120)
    private String nombre;

    @Size(max = 60)
    private String documento;

    @Size(max = 80)
    private String contactoNombre;

    @Size(max = 30)
    private String telefono;

    @Email(message = "El correo no es válido")
    @Size(max = 120)
    private String email;

    @Builder.Default
    private Boolean activo = true;

    private Integer cantidadPrecios;
    private Boolean desactivable;
    private String motivoNoDesactivable;
}
