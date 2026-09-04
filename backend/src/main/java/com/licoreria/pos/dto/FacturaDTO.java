package com.licoreria.pos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.licoreria.pos.model.EstadoFactura;
import com.licoreria.pos.model.FormaPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaDTO {

    private Long id;
    private String numero;
    private Long ventaId;
    private String ventaNumero;
    private Long turnoCajaId;
    private LocalDateTime fechaEmision;
    private String clienteNombre;
    @JsonAlias("clienteRtn")
    private String clienteRuc;
    private String numeroFiscal;
    @JsonAlias("cai")
    private String autorizacionDgi;
    private String rangoAutorizado;
    private LocalDate fechaLimiteEmision;
    private BigDecimal subtotal;
    private BigDecimal impuesto;
    private BigDecimal total;
    private EstadoFactura estado;
    private FormaPago formaPago;
    private BigDecimal montoRecibido;
    private BigDecimal vuelto;
    private String cajeroNombre;
    private Long cajeroId;
    private String motivoAnulacion;
    private Long anuladoPor;
    private String anuladoPorNombre;
    private Long solicitadoAnulacionPor;
    private String solicitadoAnulacionPorNombre;
    private LocalDateTime fechaAnulacion;
    private Boolean anulable;
    private String motivoNoAnulable;

    @Builder.Default
    private List<DetalleVentaResponseDTO> lineas = new ArrayList<>();

    @JsonGetter("clienteRtn")
    public String getClienteRtn() {
        return clienteRuc;
    }

    @JsonGetter("cai")
    public String getCai() {
        return autorizacionDgi;
    }
}
