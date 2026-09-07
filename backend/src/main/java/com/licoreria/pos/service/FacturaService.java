package com.licoreria.pos.service;



import com.licoreria.pos.config.PosProperties;

import com.licoreria.pos.dto.AnulacionFacturaDTO;

import com.licoreria.pos.dto.DetalleVentaResponseDTO;

import com.licoreria.pos.dto.FacturaDTO;

import com.licoreria.pos.dto.PaginaDTO;

import com.licoreria.pos.exception.RecursoNoEncontradoException;

import com.licoreria.pos.exception.ReglaNegocioException;

import com.licoreria.pos.model.AccionAuditoria;

import com.licoreria.pos.model.Cliente;

import com.licoreria.pos.model.DetalleVenta;

import com.licoreria.pos.model.EstadoFactura;

import com.licoreria.pos.model.EstadoTurnoCaja;

import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.FormaPago;

import com.licoreria.pos.model.Factura;

import com.licoreria.pos.model.Permiso;

import com.licoreria.pos.model.Producto;

import com.licoreria.pos.model.TipoMovimiento;

import com.licoreria.pos.model.TurnoCaja;

import com.licoreria.pos.model.Usuario;

import com.licoreria.pos.model.Venta;

import com.licoreria.pos.repository.ClienteRepository;

import com.licoreria.pos.repository.FacturaRepository;

import com.licoreria.pos.repository.ProductoRepository;

import com.licoreria.pos.repository.TurnoCajaRepository;

import com.licoreria.pos.repository.UsuarioRepository;

import com.licoreria.pos.repository.VentaRepository;

import com.licoreria.pos.util.PaginacionUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.math.BigDecimal;

import java.time.Clock;

import java.time.LocalDate;

import java.time.LocalDateTime;

import java.time.LocalTime;

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

    private final ProductoRepository productoRepository;

    private final TurnoCajaRepository turnoCajaRepository;

    private final UsuarioRepository usuarioRepository;

    private final InventarioService inventarioService;

    private final ClienteService clienteService;

    private final AutorizacionService autorizacionService;

    private final AccesoService accesoService;

    private final NumeracionFiscalService numeracionFiscalService;

    private final AuditoriaService auditoriaService;

    private final PosProperties posProperties;

    private final Clock clock;



    @Transactional(readOnly = true)

    public PaginaDTO<FacturaDTO> listar(EstadoFactura estado, String busqueda, LocalDate desde, LocalDate hasta,
                                        Long cajeroId, int pagina, int tamano) {
        accesoService.exigirAlguno(Permiso.FACTURAS_VER, Permiso.VENTAS_CREAR);
        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime fin = hasta == null ? null : hasta.atTime(LocalTime.MAX);
        String termino = normalizar(busqueda);
        List<Long> usuarioIdsBusqueda = idsUsuariosPorBusqueda(termino);
        Page<Factura> page = facturaRepository.buscarPaginado(
                estado, termino, inicio, fin, cajeroId, usuarioIdsBusqueda,
                PaginacionUtil.pageable(pagina, tamano)
        );
        return PaginaDTO.de(page.map(factura -> toDto(factura, false)));
    }

    @Transactional(readOnly = true)

    public List<FacturaDTO> listar(EstadoFactura estado, String busqueda, LocalDate desde, LocalDate hasta, Long cajeroId) {
        return listar(estado, busqueda, desde, hasta, cajeroId, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }



    @Transactional(readOnly = true)

    public FacturaDTO obtener(Long id) {
        accesoService.exigirAlguno(Permiso.FACTURAS_VER, Permiso.VENTAS_CREAR);

        Factura factura = facturaRepository.findById(id)

                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada: " + id));

        return toDto(factura, true);

    }



    @Transactional(readOnly = true)

    public FacturaDTO obtenerPorVenta(Long ventaId) {

        accesoService.exigirAlguno(Permiso.FACTURAS_VER, Permiso.VENTAS_CREAR);

        return facturaRepository.findByVentaId(ventaId)

                .map(factura -> toDto(factura, true))

                .orElseThrow(() -> new RecursoNoEncontradoException("No hay factura para la venta " + ventaId));

    }



    @Transactional(readOnly = true)

    public Optional<FacturaDTO> buscarPorVenta(Long ventaId) {

        return facturaRepository.findByVentaId(ventaId).map(factura -> toDto(factura, false));

    }



    @Transactional

    public FacturaDTO emitirDesdeVenta(Long ventaId) {

        Optional<Factura> existente = facturaRepository.findByVentaId(ventaId);

        if (existente.isPresent()) {

            return toDto(existente.get(), true);

        }



        Venta venta = ventaRepository.findById(ventaId)

                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada: " + ventaId));

        validarVentaFacturable(venta);



        String clienteNombre = "Consumidor final";

        String clienteRuc = null;

        if (venta.getClienteId() != null) {

            Cliente cliente = clienteRepository.findById(venta.getClienteId()).orElse(null);

            if (cliente != null) {

                clienteNombre = cliente.getNombre();

                clienteRuc = cliente.getRuc();

            }

        }



        NumeracionFiscalService.DocumentoFiscal fiscal = numeracionFiscalService.asignarSiguiente();

        Factura factura = Factura.builder()

                .numero(siguienteNumero())

                .numeroFiscal(fiscal == null ? null : fiscal.numeroFiscal())

                .autorizacionDgi(fiscal == null ? null : fiscal.autorizacionDgi())

                .rangoAutorizado(fiscal == null ? null : fiscal.rangoAutorizado())

                .fechaLimiteEmision(fiscal == null ? null : fiscal.fechaLimite())

                .ventaId(venta.getId())

                .fechaEmision(LocalDateTime.now(clock))

                .clienteNombre(clienteNombre)

                .clienteRuc(clienteRuc)

                .subtotal(venta.getSubtotal())

                .impuesto(venta.getImpuesto())

                .total(venta.getTotal())

                .estado(EstadoFactura.EMITIDA)

                .build();



        Factura guardada = facturaRepository.save(factura);

        return toDto(guardada, true);

    }



    @Transactional

    public FacturaDTO anular(Long facturaId, AnulacionFacturaDTO request) {

        Factura factura = facturaRepository.findById(facturaId)

                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada: " + facturaId));

        if (factura.getEstado() == EstadoFactura.ANULADA) {

            throw new ReglaNegocioException("FACTURA_YA_ANULADA", "La factura ya está anulada");

        }



        Usuario operador = accesoService.exigirPermiso(Permiso.VENTAS_ANULAR);

        Usuario admin = autorizacionService.exigirCredencialAdmin(

                request.getAutorizacion(),

                "Ningún cajero puede anular una factura por su cuenta. Se requiere usuario y clave de un administrador"

        );



        Venta venta = ventaRepository.findById(factura.getVentaId())

                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada: " + factura.getVentaId()));

        if (venta.getEstado() == EstadoVenta.ANULADA) {

            throw new ReglaNegocioException("VENTA_YA_ANULADA", "La venta ya está anulada");

        }

        exigirTurnoAbiertoParaAnular(venta);



        for (DetalleVenta detalle : venta.getDetalles()) {

            inventarioService.ingresar(
                    detalle.getProductoId(),
                    detalle.getPresentacionId(),
                    detalle.getCantidad(),
                    TipoMovimiento.ANULACION,
                    "Anulación factura " + factura.getNumero() + ": " + request.getMotivo().trim(),
                    admin.getId(),
                    null,
                    venta.getId(),
                    null
            );

        }



        if (venta.getFormaPago() == FormaPago.CREDITO && venta.getClienteId() != null) {
            clienteService.liberarCredito(venta.getClienteId(), venta.getTotal());
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

                "motivo=" + request.getMotivo().trim() + ", solicitadoPor=" + operador.getId()

                        + ", autorizadoPor=" + admin.getId());

        return toDto(anulada, true);

    }



    private void validarVentaFacturable(Venta venta) {

        if (venta.getEstado() != EstadoVenta.COMPLETADA) {

            throw new ReglaNegocioException("VENTA_NO_COMPLETA", "Solo se factura una venta completada");

        }

        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {

            throw new ReglaNegocioException("VENTA_SIN_DETALLE", "La venta no tiene productos para facturar");

        }

        if (venta.getTotal() == null || venta.getTotal().compareTo(BigDecimal.ZERO) <= 0) {

            throw new ReglaNegocioException("TOTAL_INVALIDO", "El total de la venta debe ser mayor a cero");

        }

    }



    private void exigirTurnoAbiertoParaAnular(Venta venta) {

        if (!posProperties.getFactura().isRequiereTurnoAbiertoParaAnular()) {

            return;

        }

        if (venta.getTurnoCajaId() == null) {

            throw new ReglaNegocioException(

                    "TURNO_NO_IDENTIFICADO",

                    "No se puede anular: la venta no está ligada a un turno de caja"

            );

        }

        TurnoCaja turno = turnoCajaRepository.findById(venta.getTurnoCajaId())

                .orElseThrow(() -> new RecursoNoEncontradoException("Turno de caja no encontrado: " + venta.getTurnoCajaId()));

        if (turno.getEstado() != EstadoTurnoCaja.ABIERTO) {

            throw new ReglaNegocioException(

                    "TURNO_CERRADO_ANULACION",

                    "No se puede anular: el turno de caja #" + turno.getId() + " ya está cerrado"

            );

        }

    }



    private String normalizar(String busqueda) {

        if (busqueda == null) {

            return null;

        }

        String limpio = busqueda.trim();

        return limpio.isEmpty() ? null : limpio;

    }



    private String siguienteNumero() {

        return "F-" + LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

                + "-" + ThreadLocalRandom.current().nextInt(100, 999);

    }



    private FacturaDTO toDto(Factura factura, boolean conLineas) {

        Venta venta = factura.getVentaId() == null

                ? null

                : ventaRepository.findById(factura.getVentaId()).orElse(null);

        String motivoNoAnulable = motivoNoAnulable(factura, venta);

        return FacturaDTO.builder()

                .id(factura.getId())

                .numero(factura.getNumero())

                .ventaId(factura.getVentaId())

                .ventaNumero(venta == null ? null : venta.getNumero())

                .turnoCajaId(venta == null ? null : venta.getTurnoCajaId())

                .fechaEmision(factura.getFechaEmision())

                .clienteNombre(factura.getClienteNombre())

                .clienteRuc(factura.getClienteRuc())

                .numeroFiscal(factura.getNumeroFiscal())

                .autorizacionDgi(factura.getAutorizacionDgi())

                .rangoAutorizado(factura.getRangoAutorizado())

                .fechaLimiteEmision(factura.getFechaLimiteEmision())

                .subtotal(factura.getSubtotal())

                .impuesto(factura.getImpuesto())

                .total(factura.getTotal())

                .estado(factura.getEstado())

                .formaPago(venta == null ? null : venta.getFormaPago())

                .montoRecibido(venta == null ? null : venta.getMontoRecibido())

                .vuelto(venta == null ? null : venta.getVuelto())

                .cajeroNombre(venta == null ? null : nombreUsuario(venta.getUsuarioId()))
                .cajeroId(venta == null ? null : venta.getUsuarioId())
                .motivoAnulacion(factura.getMotivoAnulacion())

                .anuladoPor(factura.getAnuladoPor())

                .anuladoPorNombre(nombreUsuario(factura.getAnuladoPor()))

                .solicitadoAnulacionPor(factura.getSolicitadoAnulacionPor())

                .solicitadoAnulacionPorNombre(nombreUsuario(factura.getSolicitadoAnulacionPor()))

                .fechaAnulacion(factura.getFechaAnulacion())

                .anulable(motivoNoAnulable == null)

                .motivoNoAnulable(motivoNoAnulable)

                .lineas(conLineas ? lineasDe(factura.getVentaId()) : List.of())

                .build();

    }



    private String motivoNoAnulable(Factura factura, Venta venta) {

        if (factura.getEstado() == EstadoFactura.ANULADA) {

            return "La factura ya está anulada";

        }

        if (venta == null) {

            return "Venta asociada no encontrada";

        }

        if (venta.getEstado() == EstadoVenta.ANULADA) {

            return "La venta ya está anulada";

        }

        if (posProperties.getFactura().isRequiereTurnoAbiertoParaAnular()) {

            if (venta.getTurnoCajaId() == null) {

                return "La venta no tiene turno de caja";

            }

            TurnoCaja turno = turnoCajaRepository.findById(venta.getTurnoCajaId()).orElse(null);

            if (turno == null) {

                return "Turno de caja no encontrado";

            }

            if (turno.getEstado() != EstadoTurnoCaja.ABIERTO) {

                return "El turno de caja ya está cerrado";

            }

        }

        return null;

    }



    private String nombreUsuario(Long usuarioId) {

        if (usuarioId == null) {

            return null;

        }

        return usuarioRepository.findById(usuarioId)

                .map(Usuario::getNombreCompleto)

                .orElse(null);

    }

    private List<Long> idsUsuariosPorBusqueda(String termino) {
        if (termino == null || termino.isBlank()) {
            return List.of(-1L);
        }
        List<Long> ids = usuarioRepository.buscar(termino, null, null).stream()
                .map(Usuario::getId)
                .toList();
        return ids.isEmpty() ? List.of(-1L) : ids;
    }



    private List<DetalleVentaResponseDTO> lineasDe(Long ventaId) {

        if (ventaId == null) {

            return List.of();

        }

        return ventaRepository.findById(ventaId)

                .map(venta -> venta.getDetalles().stream().map(this::toLinea).toList())

                .orElse(List.of());

    }



    private DetalleVentaResponseDTO toLinea(DetalleVenta detalle) {

        String productoNombre = productoRepository.findById(detalle.getProductoId())

                .map(Producto::getNombre)

                .orElse("Producto " + detalle.getProductoId());

        String presentacionNombre = null;

        try {

            presentacionNombre = inventarioService

                    .obtenerPresentacion(detalle.getProductoId(), detalle.getPresentacionId())

                    .getNombre();

        } catch (RuntimeException ignored) {

            presentacionNombre = null;

        }

        return DetalleVentaResponseDTO.builder()

                .productoId(detalle.getProductoId())

                .productoNombre(productoNombre)

                .presentacionId(detalle.getPresentacionId())

                .presentacionNombre(presentacionNombre)

                .cantidad(detalle.getCantidad())

                .cantidadUmm(detalle.getCantidadUmm())

                .precioUnitarioUmm(detalle.getPrecioUnitarioUmm())

                .precioUnitario(detalle.getPrecioUnitario())

                .subtotal(detalle.getSubtotal())

                .build();

    }

}

