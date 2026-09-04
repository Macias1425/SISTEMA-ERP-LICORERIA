package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.dto.DetalleVentaDTO;
import com.licoreria.pos.dto.DetalleVentaResponseDTO;
import com.licoreria.pos.dto.FacturaDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.VentaRequestDTO;
import com.licoreria.pos.dto.VentaResponseDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Cliente;
import com.licoreria.pos.model.DetalleVenta;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.FormaPago;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.TipoVerificacionEdad;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.repository.VentaRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VentaService {

    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
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
    private final AccesoService accesoService;
    private final PosProperties posProperties;
    private final Clock clock;

    @Lazy
    @Autowired
    private ControlVentasService controlVentasService;

    @Transactional(readOnly = true)
    public PaginaDTO<VentaResponseDTO> listar(int pagina, int tamano) {
        return PaginaDTO.de(ventaRepository.findAllByOrderByFechaDesc(PaginacionUtil.pageable(pagina, tamano))
                .map(venta -> toResponse(venta, facturaService.buscarPorVenta(venta.getId()).orElse(null))));
    }

    @Transactional(readOnly = true)
    public List<VentaResponseDTO> listar() {
        return listar(0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<VentaResponseDTO> listarPorTurno(Long turnoId, int pagina, int tamano) {
        accesoService.exigirAlguno(Permiso.FACTURAS_VER, Permiso.VENTAS_CREAR);
        return PaginaDTO.de(ventaRepository.findByTurnoCajaIdOrderByFechaDesc(
                turnoId, PaginacionUtil.pageable(pagina, tamano)
        ).map(venta -> toResponse(venta, facturaService.buscarPorVenta(venta.getId()).orElse(null))));
    }

    @Transactional(readOnly = true)
    public List<VentaResponseDTO> listarPorTurno(Long turnoId) {
        return listarPorTurno(turnoId, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public VentaResponseDTO obtenerPorId(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada: " + id));
        return toResponse(venta, facturaService.buscarPorVenta(id).orElse(null));
    }

    @Transactional
    public VentaResponseDTO registrar(VentaRequestDTO request) {
        Usuario cajero = accesoService.exigirPermiso(Permiso.VENTAS_CREAR);
        var turno = cajaService.exigirTurnoAbierto(cajero.getId());
        controlVentasService.validarLimiteTurno(turno.getId());
        FormaPago formaPago = request.getFormaPago() == null ? FormaPago.EFECTIVO : request.getFormaPago();
        String claveIdempotencia = normalizarClave(request.getClaveIdempotencia());
        VentaResponseDTO repetida = ventaIdempotente(cajero, claveIdempotencia);
        if (repetida != null) {
            return repetida;
        }

        Cliente cliente = request.getClienteId() == null ? null : clienteService.buscar(request.getClienteId());
        TipoCliente tipoSolicitado = request.getTipoCliente() != null
                ? request.getTipoCliente()
                : (cliente != null ? cliente.getTipoCliente() : TipoCliente.DETAL);

        List<LineaPreparada> lineas = prepararLineas(request.getDetalles());
        exigirStockTicket(lineas);
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

            BigDecimal precioUmmCatalogo = TarifaVenta.dinero(precio.getPrecioUmm());
            int factor = linea.presentacion.getFactorAUnidadMinima();
            BigDecimal precioPresentacion = TarifaVenta.precioPresentacion(precioUmmCatalogo, factor);

            if (dto.getPrecioUnitario() != null && dto.getPrecioUnitario().compareTo(precioPresentacion) != 0) {
                // RN-POS-05: cambio de precio en venta exige credencial de administrador con permiso explícito.
                Usuario supervisor = autorizacionService.exigirCredencialAdmin(
                        request.getAutorizacionSupervisor(),
                        "El cajero no puede alterar el precio del catálogo. Se requiere usuario y clave de un administrador"
                );
                accesoService.exigirPermisoDe(supervisor, Permiso.VENTAS_OVERRIDE_PRECIO,
                        "El administrador autorizante no tiene permiso para cambiar precios en venta");
                autorizadoPrecioPor = supervisor.getId();
                precioPresentacion = TarifaVenta.dinero(dto.getPrecioUnitario());
                precioUmmCatalogo = TarifaVenta.precioUmmDesdePresentacion(precioPresentacion, factor);
                observacion = append(observacion, "Precio autorizado por supervisor id " + supervisor.getId());
                auditoriaService.registrar(supervisor, AccionAuditoria.CAMBIO_PRECIO, "Producto", linea.producto.getId(),
                        precioUmmCatalogo.toPlainString(), dto.getPrecioUnitario().toPlainString(),
                        "Override en venta");
            }

            BigDecimal subtotalLinea = TarifaVenta.subtotalLinea(precioPresentacion, linea.cantidadPresentacion);
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

        BigDecimal tasa = posProperties.getImpuesto().getTasaIva();
        BigDecimal impuesto = TarifaVenta.impuesto(subtotal, tasa);
        BigDecimal total = TarifaVenta.dinero(subtotal.add(impuesto));

        var reglasControl = controlVentasService.reglasOperativas();
        if (reglasControl.getMontoSupervisorRequerido() != null
                && total.compareTo(reglasControl.getMontoSupervisorRequerido()) >= 0
                && cajero.getRol() != Rol.ADMIN) {
            autorizacionService.exigirCredencialAdmin(
                    request.getAutorizacionSupervisor(),
                    "Las ventas iguales o superiores a C$ "
                            + reglasControl.getMontoSupervisorRequerido()
                            + " requieren autorización de un administrador"
            );
            observacion = append(observacion, "Venta alto monto autorizada por supervisor");
        }

        BigDecimal vuelto = CobroPos.vuelto(formaPago, request.getMontoRecibido(), total);
        BigDecimal montoRecibido = CobroPos.montoRegistrado(formaPago, request.getMontoRecibido(), total);
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
                .montoRecibido(montoRecibido)
                .vuelto(vuelto)
                .claveIdempotencia(claveIdempotencia)
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
                    cajero.getId(),
                    null,
                    guardada.getId(),
                    null
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
            LocalDate hoy = LocalDate.now(clock);
            if (producto.getFechaVencimiento() != null && producto.getFechaVencimiento().isBefore(hoy)) {
                throw new ReglaNegocioException(
                        "PRODUCTO_VENCIDO",
                        "No se puede vender '" + producto.getNombre() + "': venció el " + producto.getFechaVencimiento()
                );
            }
            Presentacion presentacion = inventarioService.obtenerPresentacion(producto.getId(), dto.getPresentacionId());
            int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, dto.getCantidad());
            lineas.add(new LineaPreparada(producto, presentacion, dto.getCantidad(), cantidadUmm));
        }
        return lineas;
    }

    private void exigirStockTicket(List<LineaPreparada> lineas) {
        Map<Long, Integer> demanda = new LinkedHashMap<>();
        for (LineaPreparada linea : lineas) {
            demanda.merge(linea.producto.getId(), linea.cantidadUmm, Integer::sum);
        }
        for (Map.Entry<Long, Integer> entrada : demanda.entrySet()) {
            inventarioService.exigirStockDisponible(entrada.getKey(), entrada.getValue());
        }
    }

    private VentaResponseDTO ventaIdempotente(Usuario cajero, String clave) {
        if (clave == null) {
            return null;
        }
        return ventaRepository.findByClaveIdempotencia(clave)
                .map(venta -> {
                    if (!cajero.getId().equals(venta.getUsuarioId())) {
                        throw new ReglaNegocioException(
                                "IDEMPOTENCIA_AJENA",
                                "Esa clave de cobro ya fue usada por otro cajero"
                        );
                    }
                    return toResponse(venta, facturaService.buscarPorVenta(venta.getId()).orElse(null));
                })
                .orElse(null);
    }

    private String normalizarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            return null;
        }
        return clave.trim();
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
                .clienteNombre(nombreCliente(venta.getClienteId()))
                .usuarioId(venta.getUsuarioId())
                .cajeroNombre(nombreUsuario(venta.getUsuarioId()))
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
                .montoRecibido(venta.getMontoRecibido())
                .vuelto(venta.getVuelto())
                .estado(venta.getEstado())
                .factura(factura)
                .detalles(venta.getDetalles().stream().map(this::toDetalleResponse).toList())
                .build();
    }

    private DetalleVentaResponseDTO toDetalleResponse(DetalleVenta detalle) {
        return DetalleVentaResponseDTO.builder()
                .productoId(detalle.getProductoId())
                .productoNombre(nombreProducto(detalle.getProductoId()))
                .presentacionId(detalle.getPresentacionId())
                .presentacionNombre(nombrePresentacion(detalle.getProductoId(), detalle.getPresentacionId()))
                .cantidad(detalle.getCantidad())
                .cantidadUmm(detalle.getCantidadUmm())
                .precioUnitarioUmm(detalle.getPrecioUnitarioUmm())
                .precioUnitario(detalle.getPrecioUnitario())
                .subtotal(detalle.getSubtotal())
                .build();
    }

    private String nombreProducto(Long productoId) {
        return productoRepository.findById(productoId).map(Producto::getNombre).orElse(null);
    }

    private String nombrePresentacion(Long productoId, Long presentacionId) {
        try {
            return inventarioService.obtenerPresentacion(productoId, presentacionId).getNombre();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String nombreCliente(Long clienteId) {
        if (clienteId == null) {
            return "Consumidor final";
        }
        try {
            return clienteService.buscar(clienteId).getNombre();
        } catch (RecursoNoEncontradoException ignored) {
            return null;
        }
    }

    private String nombreUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return usuarioRepository.findById(usuarioId)
                .map(Usuario::getNombreCompleto)
                .orElse(null);
    }

    private record LineaPreparada(Producto producto, Presentacion presentacion, int cantidadPresentacion, int cantidadUmm) {
    }
}
