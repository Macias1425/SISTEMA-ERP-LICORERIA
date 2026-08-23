package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.dto.DetalleVentaDTO;
import com.licoreria.pos.dto.DetalleVentaResponseDTO;
import com.licoreria.pos.dto.FacturaDTO;
import com.licoreria.pos.dto.VentaRequestDTO;
import com.licoreria.pos.dto.VentaResponseDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Cliente;
import com.licoreria.pos.model.DetalleVenta;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.FormaPago;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.TipoVerificacionEdad;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VentaService {

    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteService clienteService;
    private final InventarioService inventarioService;
    private final ConversionUnidades conversionUnidades;
    private final MayoriaEdadService mayoriaEdadService;
    private final HorarioVentaService horarioVentaService;
    private final ListaPrecioService listaPrecioService;
    private final FacturaService facturaService;
    private final CajaService cajaService;
    private final AuditoriaService auditoriaService;
    private final AutorizacionService autorizacionService;
    private final PosProperties posProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<VentaResponseDTO> listar() {
        return ventaRepository.findAll().stream()
                .map(venta -> toResponse(venta, facturaService.buscarPorVenta(venta.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public VentaResponseDTO obtenerPorId(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada: " + id));
        return toResponse(venta, facturaService.buscarPorVenta(id).orElse(null));
    }

    @Transactional
    public VentaResponseDTO registrar(VentaRequestDTO request) {
        Usuario cajero = autorizacionService.exigirRol(Rol.CAJERO, Rol.ADMIN);
        var turno = cajaService.exigirTurnoAbierto(cajero.getId());
        FormaPago formaPago = request.getFormaPago() == null ? FormaPago.EFECTIVO : request.getFormaPago();

        Cliente cliente = request.getClienteId() == null ? null : clienteService.buscar(request.getClienteId());
        TipoCliente tipoSolicitado = request.getTipoCliente() != null
                ? request.getTipoCliente()
                : (cliente != null ? cliente.getTipoCliente() : TipoCliente.DETAL);

        List<LineaPreparada> lineas = prepararLineas(request.getDetalles());
        boolean hayAlcohol = lineas.stream().anyMatch(linea -> Boolean.TRUE.equals(linea.producto.getEsAlcoholico()));
        int ummTicket = lineas.stream().mapToInt(linea -> linea.cantidadUmm).sum();

        horarioVentaService.validarVentaLicor(hayAlcohol);
        TipoVerificacionEdad verificacion = mayoriaEdadService.validar(
                hayAlcohol,
                request.getFechaNacimientoCliente(),
                Boolean.TRUE.equals(request.getConfirmacionMayoriaEdad())
        );

        TipoCliente tipoAplicado = listaPrecioService.resolverTipoAplicado(tipoSolicitado, ummTicket);
        String observacion = null;
        if (tipoSolicitado != TipoCliente.DETAL && tipoAplicado == TipoCliente.DETAL) {
            observacion = "Volumen insuficiente para tarifa mayorista; se aplicó precio detal.";
        }

        Long autorizadoPrecioPor = null;
        BigDecimal subtotal = BigDecimal.ZERO;
        List<DetalleVenta> detalles = new ArrayList<>();

        for (int i = 0; i < lineas.size(); i++) {
            LineaPreparada linea = lineas.get(i);
            DetalleVentaDTO dto = request.getDetalles().get(i);
            ListaPrecioService.PrecioResuelto precio = listaPrecioService.resolverPrecio(
                    linea.producto, tipoAplicado, linea.cantidadUmm);

            BigDecimal precioUmmCatalogo = precio.getPrecioUmm().setScale(2, REDONDEO);
            BigDecimal factor = BigDecimal.valueOf(linea.presentacion.getFactorAUnidadMinima());
            BigDecimal precioPresentacion = precioUmmCatalogo.multiply(factor).setScale(2, REDONDEO);

            if (dto.getPrecioUnitario() != null && dto.getPrecioUnitario().compareTo(precioPresentacion) != 0) {
                Usuario supervisor = autorizacionService.exigirCredencialAdmin(
                        request.getAutorizacionSupervisor(),
                        "El cajero no puede alterar el precio del catálogo. Se requiere usuario y clave de un administrador"
                );
                autorizadoPrecioPor = supervisor.getId();
                precioPresentacion = dto.getPrecioUnitario().setScale(2, REDONDEO);
                precioUmmCatalogo = precioPresentacion.divide(factor, 4, REDONDEO);
                observacion = append(observacion, "Precio autorizado por supervisor id " + supervisor.getId());
                auditoriaService.registrar(supervisor, AccionAuditoria.CAMBIO_PRECIO, "Producto", linea.producto.getId(),
                        precioUmmCatalogo.toPlainString(), dto.getPrecioUnitario().toPlainString(),
                        "Override en venta");
            }

            BigDecimal subtotalLinea = precioPresentacion
                    .multiply(BigDecimal.valueOf(linea.cantidadPresentacion))
                    .setScale(2, REDONDEO);
            subtotal = subtotal.add(subtotalLinea);

            detalles.add(DetalleVenta.builder()
                    .productoId(linea.producto.getId())
                    .presentacionId(linea.presentacion.getId())
                    .cantidad(linea.cantidadPresentacion)
                    .cantidadUmm(linea.cantidadUmm)
                    .precioUnitarioUmm(precioUmmCatalogo.setScale(2, REDONDEO))
                    .precioUnitario(precioPresentacion)
                    .subtotal(subtotalLinea)
                    .build());
        }

        BigDecimal tasa = posProperties.getImpuesto().getTasaIsv();
        BigDecimal impuesto = subtotal.multiply(tasa).setScale(2, REDONDEO);
        BigDecimal total = subtotal.add(impuesto);
        LocalDateTime ahora = LocalDateTime.now(clock);

        Venta venta = Venta.builder()
                .numero(siguienteNumero("V-"))
                .clienteId(cliente == null ? null : cliente.getId())
                .usuarioId(cajero.getId())
                .tipoClienteSolicitado(tipoSolicitado)
                .tipoClienteAplicado(tipoAplicado)
                .fecha(ahora)
                .subtotal(subtotal)
                .impuesto(impuesto)
                .total(total)
                .verificacionEdad(verificacion)
                .fechaNacimientoCliente(request.getFechaNacimientoCliente())
                .confirmacionCajero(Boolean.TRUE.equals(request.getConfirmacionMayoriaEdad()))
                .autorizadoPrecioPor(autorizadoPrecioPor)
                .observacion(observacion)
                .turnoCajaId(turno.getId())
                .formaPago(formaPago)
                .estado(EstadoVenta.COMPLETADA)
                .build();

        for (DetalleVenta detalle : detalles) {
            detalle.setVenta(venta);
            venta.getDetalles().add(detalle);
        }

        Venta guardada = ventaRepository.save(venta);

        for (DetalleVenta detalle : guardada.getDetalles()) {
            inventarioService.descontar(
                    detalle.getProductoId(),
                    detalle.getPresentacionId(),
                    detalle.getCantidad(),
                    TipoMovimiento.VENTA,
                    "Venta " + guardada.getNumero(),
                    cajero.getId()
            );
        }

        FacturaDTO factura = facturaService.emitirDesdeVenta(guardada.getId());
        auditoriaService.registrar(cajero, AccionAuditoria.VENTA, "Venta", guardada.getId(),
                null, guardada.getTotal().toPlainString(), "Venta " + guardada.getNumero());
        return toResponse(guardada, factura);
    }

    private List<LineaPreparada> prepararLineas(List<DetalleVentaDTO> detalles) {
        List<LineaPreparada> lineas = new ArrayList<>();
        for (DetalleVentaDTO dto : detalles) {
            Producto producto = productoRepository.findById(dto.getProductoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + dto.getProductoId()));
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new ReglaNegocioException("PRODUCTO_INACTIVO", "El producto no está activo: " + producto.getNombre());
            }
            Presentacion presentacion = inventarioService.obtenerPresentacion(producto.getId(), dto.getPresentacionId());
            int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, dto.getCantidad());
            lineas.add(new LineaPreparada(producto, presentacion, dto.getCantidad(), cantidadUmm));
        }
        return lineas;
    }

    private String siguienteNumero(String prefijo) {
        return prefijo + LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private String append(String actual, String extra) {
        if (actual == null || actual.isBlank()) {
            return extra;
        }
        return actual + " " + extra;
    }

    private VentaResponseDTO toResponse(Venta venta, FacturaDTO factura) {
        return VentaResponseDTO.builder()
                .id(venta.getId())
                .numero(venta.getNumero())
                .clienteId(venta.getClienteId())
                .usuarioId(venta.getUsuarioId())
                .tipoClienteSolicitado(venta.getTipoClienteSolicitado())
                .tipoClienteAplicado(venta.getTipoClienteAplicado())
                .fecha(venta.getFecha())
                .subtotal(venta.getSubtotal())
                .impuesto(venta.getImpuesto())
                .total(venta.getTotal())
                .verificacionEdad(venta.getVerificacionEdad())
                .fechaNacimientoCliente(venta.getFechaNacimientoCliente())
                .confirmacionCajero(venta.getConfirmacionCajero())
                .autorizadoPrecioPor(venta.getAutorizadoPrecioPor())
                .observacion(venta.getObservacion())
                .turnoCajaId(venta.getTurnoCajaId())
                .formaPago(venta.getFormaPago())
                .estado(venta.getEstado())
                .factura(factura)
                .detalles(venta.getDetalles().stream().map(this::toDetalleResponse).toList())
                .build();
    }

    private DetalleVentaResponseDTO toDetalleResponse(DetalleVenta detalle) {
        return DetalleVentaResponseDTO.builder()
                .productoId(detalle.getProductoId())
                .presentacionId(detalle.getPresentacionId())
                .cantidad(detalle.getCantidad())
                .cantidadUmm(detalle.getCantidadUmm())
                .precioUnitarioUmm(detalle.getPrecioUnitarioUmm())
                .precioUnitario(detalle.getPrecioUnitario())
                .subtotal(detalle.getSubtotal())
                .build();
    }

    private record LineaPreparada(Producto producto, Presentacion presentacion, int cantidadPresentacion, int cantidadUmm) {
    }
}
