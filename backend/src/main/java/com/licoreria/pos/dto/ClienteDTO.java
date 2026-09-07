package com.licoreria.pos.dto;

import com.licoreria.pos.model.TipoCliente;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

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

    @DecimalMin(value = "0.0", message = "El límite de crédito no puede ser negativo")
    @Builder.Default
    private BigDecimal limiteCredito = BigDecimal.ZERO;

    /** Solo lectura en respuestas; los abonos van por endpoint de crédito. */
    @Builder.Default
    private BigDecimal saldoCredito = BigDecimal.ZERO;
}
