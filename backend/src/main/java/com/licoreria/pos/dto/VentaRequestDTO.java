package com.licoreria.pos.dto;

import com.licoreria.pos.model.FormaPago;
import com.licoreria.pos.model.TipoCliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaRequestDTO {

    private Long clienteId;

    @Builder.Default
    private TipoCliente tipoCliente = TipoCliente.DETAL;

    private LocalDate fechaNacimientoCliente;

    @Builder.Default
    private Boolean confirmacionMayoriaEdad = false;

    /** Usuario y clave de un ADMIN cuando el precio sale del catálogo. */
    @Valid
    private AutorizacionDTO autorizacionSupervisor;

    @Builder.Default
    private FormaPago formaPago = FormaPago.EFECTIVO;

    @Valid
    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Builder.Default
    private List<DetalleVentaDTO> detalles = new ArrayList<>();
}
