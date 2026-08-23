package com.licoreria.pos.service;

import com.licoreria.pos.dto.AnulacionFacturaDTO;
import com.licoreria.pos.dto.FacturaDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Cliente;
import com.licoreria.pos.model.DetalleVenta;
import com.licoreria.pos.model.EstadoFactura;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.Factura;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.repository.ClienteRepository;
import com.licoreria.pos.repository.FacturaRepository;
import com.licoreria.pos.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final InventarioService inventarioService;
    private final AutorizacionService autorizacionService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<FacturaDTO> listar() {
        return facturaRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<FacturaDTO> buscarPorVenta(Long ventaId) {
        return facturaRepository.findByVentaId(ventaId).map(this::toDto);
    }

    @Transactional
    public FacturaDTO emitirDesdeVenta(Long ventaId) {
        Optional<Factura> existente = facturaRepository.findByVentaId(ventaId);
        if (existente.isPresent()) {
            return toDto(existente.get());
        }

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada: " + ventaId));
        if (venta.getEstado() != EstadoVenta.COMPLETADA) {
            throw new ReglaNegocioException("VENTA_NO_COMPLETA", "Solo se factura una venta completada");
        }

        String clienteNombre = "Consumidor final";
        String clienteRtn = null;
        if (venta.getClienteId() != null) {
            Cliente cliente = clienteRepository.findById(venta.getClienteId()).orElse(null);
            if (cliente != null) {
                clienteNombre = cliente.getNombre();
                clienteRtn = cliente.getRtn();
            }
        }

        Factura factura = Factura.builder()
                .numero(siguienteNumero())
                .ventaId(venta.getId())
                .fechaEmision(LocalDateTime.now(clock))
                .clienteNombre(clienteNombre)
                .clienteRtn(clienteRtn)
                .subtotal(venta.getSubtotal())
                .impuesto(venta.getImpuesto())
                .total(venta.getTotal())
                .estado(EstadoFactura.EMITIDA)
                .build();

        return toDto(facturaRepository.save(factura));
    }

    @Transactional
    public FacturaDTO anular(Long facturaId, AnulacionFacturaDTO request) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada: " + facturaId));
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new ReglaNegocioException("FACTURA_YA_ANULADA", "La factura ya está anulada");
        }

        Usuario operador = autorizacionService.exigirRol(Rol.CAJERO, Rol.ADMIN);
        Usuario admin = autorizacionService.exigirCredencialAdmin(
                request.getAutorizacion(),
                "Ningún cajero puede anular una factura por su cuenta. Se requiere usuario y clave de un administrador"
        );

        Venta venta = ventaRepository.findById(factura.getVentaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada: " + factura.getVentaId()));
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new ReglaNegocioException("VENTA_YA_ANULADA", "La venta ya está anulada");
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            inventarioService.ingresar(
                    detalle.getProductoId(),
                    detalle.getPresentacionId(),
                    detalle.getCantidad(),
                    TipoMovimiento.ANULACION,
                    "Anulación factura " + factura.getNumero() + ": " + request.getMotivo(),
                    admin.getId()
            );
        }

        venta.setEstado(EstadoVenta.ANULADA);
        ventaRepository.save(venta);

        factura.setEstado(EstadoFactura.ANULADA);
        factura.setMotivoAnulacion(request.getMotivo().trim());
        factura.setAnuladoPor(admin.getId());
        factura.setSolicitadoAnulacionPor(operador.getId());
        factura.setFechaAnulacion(LocalDateTime.now(clock));
        Factura anulada = facturaRepository.save(factura);

        auditoriaService.registrar(admin, AccionAuditoria.ANULACION_FACTURA, "Factura", anulada.getId(),
                EstadoFactura.EMITIDA.name(), EstadoFactura.ANULADA.name(),
                "motivo=" + request.getMotivo() + ", solicitadoPor=" + operador.getId()
                        + ", autorizadoPor=" + admin.getId());
        return toDto(anulada);
    }

    private String siguienteNumero() {
        return "F-" + LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private FacturaDTO toDto(Factura factura) {
        return FacturaDTO.builder()
                .id(factura.getId())
                .numero(factura.getNumero())
                .ventaId(factura.getVentaId())
                .fechaEmision(factura.getFechaEmision())
                .clienteNombre(factura.getClienteNombre())
                .clienteRtn(factura.getClienteRtn())
                .subtotal(factura.getSubtotal())
                .impuesto(factura.getImpuesto())
                .total(factura.getTotal())
                .estado(factura.getEstado())
                .motivoAnulacion(factura.getMotivoAnulacion())
                .anuladoPor(factura.getAnuladoPor())
                .fechaAnulacion(factura.getFechaAnulacion())
                .build();
    }
}
