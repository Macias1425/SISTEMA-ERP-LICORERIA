package com.licoreria.pos.dto;

import com.licoreria.pos.model.EstadoCompra;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraDTO {

    private Long id;
    private String numero;
    private Long proveedorId;
    private String proveedorNombre;
    private String documentoProveedor;
    private LocalDateTime fecha;
    private BigDecimal total;
    private EstadoCompra estado;
    private Long usuarioId;
    private String usuarioNombre;
    private String observacion;
    @Builder.Default
    private List<DetalleCompraResponseDTO> detalles = new ArrayList<>();
    private Boolean recibible;
    private String motivoNoRecibible;
    private Boolean anulable;
    private String motivoNoAnulable;
    private Boolean cerrable;
    private String motivoNoCerrable;
    @Builder.Default
    private List<ImpactoRecepcionDTO> impactosRecepcion = new ArrayList<>();
}
