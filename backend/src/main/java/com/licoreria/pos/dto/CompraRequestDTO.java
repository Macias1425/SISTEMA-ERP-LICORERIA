package com.licoreria.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CompraRequestDTO {

    @NotNull(message = "El proveedor es obligatorio")
    private Long proveedorId;

    /** Documento de la factura del proveedor (opcional). */
    @Size(max = 60)
    private String documentoProveedor;

    @Size(max = 255)
    private String observacion;

    @Builder.Default
    private Boolean actualizarCostos = Boolean.TRUE;

    @Valid
    @NotEmpty(message = "La compra debe tener al menos una línea")
    @Builder.Default
    private List<DetalleCompraDTO> detalles = new ArrayList<>();
}
