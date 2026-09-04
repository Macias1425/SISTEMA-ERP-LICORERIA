package com.licoreria.pos.dto;

import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.FormaPago;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.model.TipoVerificacionEdad;
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
public class VentaResponseDTO {

    private Long id;
    private String numero;
    private Long clienteId;
    private String clienteNombre;
    private Long usuarioId;
    private String cajeroNombre;
    private TipoCliente tipoClienteSolicitado;
    private TipoCliente tipoClienteAplicado;
    private LocalDateTime fecha;
    private BigDecimal subtotal;
    private BigDecimal impuesto;
    private BigDecimal total;
    private TipoVerificacionEdad verificacionEdad;
    private LocalDate fechaNacimientoCliente;
    private Boolean confirmacionCajero;
    private Long autorizadoPrecioPor;
    private String observacion;
    private Long turnoCajaId;
    private FormaPago formaPago;
    private BigDecimal montoRecibido;
    private BigDecimal vuelto;
    private EstadoVenta estado;
    private FacturaDTO factura;

    @Builder.Default
    private List<DetalleVentaResponseDTO> detalles = new ArrayList<>();
}
