package com.licoreria.pos.dto;

import com.licoreria.pos.model.TipoCliente;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionDTO {

    private TipoCliente tipoClienteSolicitado;
    private TipoCliente tipoClienteAplicado;
    private Integer cantidadUmmTicket;
    private Integer volumenMinimoMayorista;

    private BigDecimal subtotal;
    private BigDecimal tasaIva;
    private BigDecimal impuesto;
    private BigDecimal total;

    private Boolean hayAlcohol;
    private Boolean ventaLicorPermitidaAhora;
    private Boolean requiereAutorizacionSupervisor;
    private BigDecimal montoSupervisorRequerido;
    private Boolean cobrable;

    @Builder.Default
    private List<CotizacionLineaDTO> lineas = new ArrayList<>();

    @Builder.Default
    private List<String> avisos = new ArrayList<>();
}
