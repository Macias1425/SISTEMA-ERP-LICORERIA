package com.licoreria.pos.service;

import com.licoreria.pos.dto.CerrarCompraDTO;
import com.licoreria.pos.dto.CompraAnulacionDTO;
import com.licoreria.pos.dto.CompraDTO;
import com.licoreria.pos.dto.CompraRequestDTO;
import com.licoreria.pos.dto.DetalleCompraDTO;
import com.licoreria.pos.dto.DetalleCompraResponseDTO;
import com.licoreria.pos.dto.ImpactoRecepcionDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.RecepcionCompraRequestDTO;
import com.licoreria.pos.dto.RecepcionLineaDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Compra;
import com.licoreria.pos.model.DetalleCompra;
import com.licoreria.pos.model.EstadoCompra;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.PrecioProveedor;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Proveedor;
import com.licoreria.pos.model.TipoMovimiento;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.CompraRepository;
import com.licoreria.pos.repository.PrecioProveedorRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CompraService {

    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final List<EstadoCompra> ESTADOS_DOC_ACTIVOS = List.of(
            EstadoCompra.PENDIENTE, EstadoCompra.PARCIAL, EstadoCompra.RECIBIDA);

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final PrecioProveedorRepository precioProveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;
    private final ProveedorService proveedorService;
    private final ConversionUnidades conversionUnidades;
    private final AutorizacionService autorizacionService;
    private final AccesoService accesoService;
    private final AuditoriaService auditoriaService;
    private final PoliticaPrecioService politicaPrecioService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PaginaDTO<CompraDTO> listar(String busqueda, LocalDate desde, LocalDate hasta, EstadoCompra estado,
                                       int pagina, int tamano) {
        accesoService.exigirPermiso(Permiso.COMPRAS_GESTIONAR);
        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime fin = hasta == null ? null : hasta.atTime(LocalTime.MAX);
        String termino = busqueda == null || busqueda.isBlank() ? null : busqueda.trim();
        Page<Compra> page = compraRepository.buscarPaginado(termino, inicio, fin, estado,
                PaginacionUtil.pageable(pagina, tamano));
        return PaginaDTO.de(page.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public List<CompraDTO> listar(String busqueda, LocalDate desde, LocalDate hasta, EstadoCompra estado) {
        return listar(busqueda, desde, hasta, estado, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public CompraDTO obtener(Long id) {
        accesoService.exigirPermiso(Permiso.COMPRAS_GESTIONAR);
        return toDto(compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + id)));
    }

    @Transactional
    public CompraDTO crearOrden(CompraRequestDTO request) {
        Usuario operador = accesoService.exigirPermiso(Permiso.COMPRAS_GESTIONAR);
        Compra compra = construirCompra(request, operador, EstadoCompra.PENDIENTE);
        Compra guardada = compraRepository.save(compra);
        auditoriaService.registrar(operador, AccionAuditoria.COMPRA, "Compra", guardada.getId(),
                null, guardada.getTotal().toPlainString(),
                "orden pendiente, proveedor=" + guardada.getProveedorNombre());
        return toDto(guardada);
    }

    @Transactional
    public CompraDTO recibirOrden(Long id) {
        return recibirOrden(id, RecepcionCompraRequestDTO.builder().build());
    }

    @Transactional
    public CompraDTO recibirOrden(Long id, RecepcionCompraRequestDTO request) {
        Usuario operador = accesoService.exigirPermiso(Permiso.COMPRAS_GESTIONAR);
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + id));
        if (compra.getEstado() != EstadoCompra.PENDIENTE && compra.getEstado() != EstadoCompra.PARCIAL) {
            throw new ReglaNegocioException("ESTADO_INVALIDO",
                    "Solo se pueden recibir órdenes PENDIENTE o PARCIAL");
        }
        EstadoCompra estadoAnterior = compra.getEstado();
        boolean actualizarCostos = request == null || !Boolean.FALSE.equals(request.getActualizarCostos());
        List<ImpactoRecepcionDTO> impactos = aplicarRecepcionParcial(compra, operador, actualizarCostos, request);
        compra.setEstado(calcularEstadoTrasRecepcion(compra));
        compra.setFecha(LocalDateTime.now(clock));
        Compra guardada = compraRepository.save(compra);
        auditoriaService.registrar(operador, AccionAuditoria.COMPRA, "Compra", guardada.getId(),
                estadoAnterior.name(), guardada.getEstado().name(),
                "recepción " + guardada.getEstado().name().toLowerCase()
                        + ", proveedor=" + guardada.getProveedorNombre());
        CompraDTO dto = toDto(guardada);
        dto.setImpactosRecepcion(impactos);
        return dto;
    }

    @Transactional
    public CompraDTO recibir(CompraRequestDTO request) {
        Usuario operador = accesoService.exigirPermiso(Permiso.COMPRAS_GESTIONAR);
        Compra compra = construirCompra(request, operador, EstadoCompra.RECIBIDA);
        Compra guardada = compraRepository.save(compra);
        inicializarLineasRecibidas(guardada);
        List<ImpactoRecepcionDTO> impactos = aplicarRecepcionDirecta(guardada, operador,
                !Boolean.FALSE.equals(request.getActualizarCostos()));
        guardada = compraRepository.save(guardada);
        auditoriaService.registrar(operador, AccionAuditoria.COMPRA, "Compra", guardada.getId(),
                null, guardada.getTotal().toPlainString(),
                "recepción directa, proveedor=" + guardada.getProveedorNombre());
        CompraDTO dto = toDto(guardada);
        dto.setImpactosRecepcion(impactos);
        return dto;
    }

    @Transactional
    public CompraDTO cerrarOrden(Long id, CerrarCompraDTO cierre) {
        Usuario operador = accesoService.exigirPermiso(Permiso.COMPRAS_GESTIONAR);
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + id));
        if (compra.getEstado() != EstadoCompra.PARCIAL) {
            throw new ReglaNegocioException("ESTADO_INVALIDO",
                    "Solo órdenes PARCIAL pueden cerrarse como incompletas");
        }
        if (cierre == null || cierre.getMotivo() == null || cierre.getMotivo().isBlank()) {
            throw new ReglaNegocioException("MOTIVO_OBLIGATORIO", "El cierre requiere motivo");
        }
        EstadoCompra anterior = compra.getEstado();
        compra.setEstado(EstadoCompra.CERRADA);
        compra.setObservacion(unirObservacion(compra.getObservacion(),
                "CERRADA: " + cierre.getMotivo().trim()));
        Compra guardada = compraRepository.save(compra);
        auditoriaService.registrar(operador, AccionAuditoria.COMPRA, "Compra", guardada.getId(),
                anterior.name(), EstadoCompra.CERRADA.name(), cierre.getMotivo().trim());
        return toDto(guardada);
    }

    @Transactional
    public CompraDTO anular(Long id, CompraAnulacionDTO anulacion) {
        Usuario operador = accesoService.exigirPermiso(Permiso.COMPRAS_GESTIONAR);
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + id));
        if (compra.getEstado() == EstadoCompra.ANULADA) {
            throw new ReglaNegocioException("YA_ANULADA", "La compra ya está anulada");
        }
        if (anulacion == null || anulacion.getMotivo() == null || anulacion.getMotivo().isBlank()) {
            throw new ReglaNegocioException("MOTIVO_OBLIGATORIO", "La anulación requiere motivo");
        }
        if (compra.getEstado() == EstadoCompra.RECIBIDA
                || compra.getEstado() == EstadoCompra.PARCIAL
                || compra.getEstado() == EstadoCompra.CERRADA) {
            autorizacionService.exigirCredencialAdmin(anulacion.getAutorizacion(),
                    "Anular una compra con recepciones requiere credenciales de administrador");
            revertirRecepcion(compra, operador, anulacion.getMotivo().trim());
        }
        EstadoCompra anterior = compra.getEstado();
        compra.setEstado(EstadoCompra.ANULADA);
        compra.setObservacion(unirObservacion(compra.getObservacion(), "ANULADA: " + anulacion.getMotivo().trim()));
        Compra guardada = compraRepository.save(compra);
        auditoriaService.registrar(operador, AccionAuditoria.COMPRA, "Compra", guardada.getId(),
                anterior.name(), EstadoCompra.ANULADA.name(), anulacion.getMotivo().trim());
        return toDto(guardada);
    }

    private Compra construirCompra(CompraRequestDTO request, Usuario operador, EstadoCompra estado) {
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("DETALLE_VACIO", "La compra debe incluir al menos una línea de producto");
        }
        LocalDate hoy = LocalDate.now(clock);
        Proveedor proveedor = proveedorService.exigirActivo(request.getProveedorId());
        String documento = texto(request.getDocumentoProveedor());
        if (documento == null) {
            documento = proveedor.getDocumento();
        }
        exigirDocumentoUnico(proveedor.getId(), documento);

        Compra compra = Compra.builder()
                .numero(siguienteNumero())
                .proveedorId(proveedor.getId())
                .proveedorNombre(proveedor.getNombre())
                .documentoProveedor(documento)
                .fecha(LocalDateTime.now(clock))
                .estado(estado)
                .usuarioId(operador.getId())
                .observacion(texto(request.getObservacion()))
                .build();

        BigDecimal total = BigDecimal.ZERO.setScale(2, REDONDEO);
        for (DetalleCompraDTO linea : request.getDetalles()) {
            LineaConstruida construida = validarYConstruirLinea(linea, proveedor.getId(), hoy);
            construida.detalle().setCompra(compra);
            total = total.add(construida.subtotal());
            compra.getDetalles().add(construida.detalle());
        }
        compra.setTotal(total);
        return compra;
    }

    private void inicializarLineasRecibidas(Compra compra) {
        for (DetalleCompra detalle : compra.getDetalles()) {
            if (detalle.getCantidadOrdenada() == null) {
                detalle.setCantidadOrdenada(detalle.getCantidad());
            }
            detalle.setCantidadRecibida(detalle.getCantidadOrdenada());
            detalle.setCantidadRechazada(0);
        }
    }

    private List<ImpactoRecepcionDTO> aplicarRecepcionDirecta(Compra compra, Usuario operador, boolean actualizarCostos) {
        List<ImpactoRecepcionDTO> impactos = new ArrayList<>();
        for (DetalleCompra detalle : compra.getDetalles()) {
            impactos.add(procesarLineaRecepcion(compra, detalle, detalle.getCantidadRecibida(), 0,
                    detalle.getNotasQc(), null, operador, actualizarCostos, false));
        }
        return impactos;
    }

    private List<ImpactoRecepcionDTO> aplicarRecepcionParcial(Compra compra, Usuario operador,
                                                                boolean actualizarCostos,
                                                                RecepcionCompraRequestDTO request) {
        Map<Long, RecepcionLineaDTO> porDetalle = indexarLineasRecepcion(request);
        List<ImpactoRecepcionDTO> impactos = new ArrayList<>();
        for (DetalleCompra detalle : compra.getDetalles()) {
            int pendiente = cantidadPendiente(detalle);
            if (pendiente <= 0) {
                continue;
            }
            RecepcionLineaDTO lineaReq = resolverLineaRecepcion(detalle, porDetalle);
            int recibir = lineaReq == null ? pendiente : valor(lineaReq.getCantidadRecibida());
            int rechazar = lineaReq == null ? 0 : valor(lineaReq.getCantidadRechazada());
            if (recibir + rechazar <= 0) {
                continue;
            }
            if (recibir + rechazar > pendiente) {
                throw new ReglaNegocioException("CANTIDAD_EXCEDIDA",
                        "La suma recibida + rechazada excede el saldo pendiente de la línea");
            }
            String notasQc = lineaReq == null ? null : lineaReq.getNotasQc();
            BigDecimal ventaExplicita = lineaReq == null ? null : lineaReq.getPrecioVentaExplicito();
            impactos.add(procesarLineaRecepcion(compra, detalle, recibir, rechazar, notasQc, ventaExplicita,
                    operador, actualizarCostos, true));
        }
        if (impactos.isEmpty()) {
            throw new ReglaNegocioException("SIN_RECEPCION", "No hay saldo pendiente por recibir en esta orden");
        }
        return impactos;
    }

    private ImpactoRecepcionDTO procesarLineaRecepcion(Compra compra, DetalleCompra detalle,
                                                       int cantidadRecibir, int cantidadRechazar,
                                                       String notasQc, BigDecimal precioVentaExplicito,
                                                       Usuario operador, boolean actualizarCostos,
                                                       boolean acumular) {
        if (cantidadRecibir <= 0 && cantidadRechazar <= 0) {
            return null;
        }
        Producto producto = productoRepository.findById(detalle.getProductoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + detalle.getProductoId()));
        Presentacion presentacion = inventarioService.obtenerPresentacion(detalle.getProductoId(), detalle.getPresentacionId());
        int stockAntes = producto.getStockActual() == null ? 0 : producto.getStockActual();
        BigDecimal costoPromedioAnterior = producto.getPrecioCompra();
        BigDecimal costoUltimoAnterior = producto.getUltimoCostoCompra() != null
                ? producto.getUltimoCostoCompra()
                : costoPromedioAnterior;
        BigDecimal ventaAnterior = producto.getPrecioVenta();

        if (cantidadRecibir > 0) {
            String motivoBase = "Compra " + compra.getNumero() + " · " + compra.getProveedorNombre();
            inventarioService.ingresar(
                    detalle.getProductoId(),
                    detalle.getPresentacionId(),
                    cantidadRecibir,
                    TipoMovimiento.COMPRA,
                    motivoBase,
                    operador.getId(),
                    compra.getId(),
                    null,
                    null,
                    datosLote(compra, detalle)
            );
        }

        if (acumular) {
            detalle.setCantidadRecibida(valor(detalle.getCantidadRecibida()) + cantidadRecibir);
            detalle.setCantidadRechazada(valor(detalle.getCantidadRechazada()) + cantidadRechazar);
        } else {
            detalle.setCantidadRecibida(cantidadRecibir);
            detalle.setCantidadRechazada(cantidadRechazar);
        }
        if (notasQc != null && !notasQc.isBlank()) {
            detalle.setNotasQc(unirObservacion(detalle.getNotasQc(), notasQc.trim()));
        }

        ResultadoPoliticaPrecio resultadoPolitica = null;
        if (actualizarCostos && cantidadRecibir > 0) {
            resultadoPolitica = actualizarCostoProducto(compra, detalle, cantidadRecibir, operador, precioVentaExplicito);
        } else if (cantidadRecibir > 0) {
            actualizarVencimiento(detalle);
        }

        if (cantidadRecibir > 0) {
            sincronizarPrecioCatalogo(
                    compra.getProveedorId(),
                    detalle.getProductoId(),
                    detalle.getPresentacionId(),
                    detalle.getCostoUnitario());
        }

        producto = productoRepository.findById(detalle.getProductoId()).orElse(producto);
        int cantidadUmmRecibida = conversionUnidades.aUnidadMinima(presentacion, cantidadRecibir);
        BigDecimal margenMin = politicaPrecioService.margenEfectivo(producto);
        BigDecimal ventaFinal = producto.getPrecioVenta();
        BigDecimal costoFinal = producto.getPrecioCompra();
        BigDecimal markup = politicaPrecioService.calcularMarkupPct(costoFinal, ventaFinal);
        BigDecimal margen = politicaPrecioService.calcularMargenPct(costoFinal, ventaFinal);
        boolean alertaMargen = margen != null && margen.compareTo(margenMin) < 0;

        if (resultadoPolitica == null && precioVentaExplicito != null) {
            resultadoPolitica = politicaPrecioService.aplicarTrasCompra(
                    producto, costoPromedioAnterior, costoFinal, operador, compra.getNumero(), precioVentaExplicito);
            productoRepository.save(producto);
            ventaFinal = producto.getPrecioVenta();
        }

        return ImpactoRecepcionDTO.builder()
                .productoId(producto.getId())
                .productoNombre(producto.getNombre())
                .costoUltimoAnterior(costoUltimoAnterior)
                .costoUltimoNuevo(producto.getUltimoCostoCompra())
                .costoPromedioAnterior(costoPromedioAnterior)
                .costoPromedioNuevo(costoFinal)
                .cantidadAntes(stockAntes)
                .cantidadRecibida(cantidadUmmRecibida)
                .cantidadDespues(producto.getStockActual())
                .ventaAnterior(ventaAnterior)
                .ventaNueva(ventaFinal)
                .ventaSugerida(resultadoPolitica == null
                        ? politicaPrecioService.precioVentaDesdeMargen(costoFinal, margenMin)
                        : resultadoPolitica.ventaSugerida())
                .markupActualPct(markup)
                .margenActualPct(margen)
                .margenMinimoPct(margenMin)
                .politicaPrecio(producto.getPoliticaPrecio())
                .decisionPrecio(resultadoPolitica == null ? null : resultadoPolitica.decision())
                .mensajeDecision(resultadoPolitica == null ? null : resultadoPolitica.mensaje())
                .ventaAplicada(resultadoPolitica != null && resultadoPolitica.ventaAplicada())
                .alertaMargen(alertaMargen)
                .build();
    }

    private ResultadoPoliticaPrecio actualizarCostoProducto(Compra compra, DetalleCompra detalle,
                                                            int cantidadRecibirEmpaque, Usuario operador,
                                                            BigDecimal precioVentaExplicito) {
        Producto persistido = productoRepository.findById(detalle.getProductoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + detalle.getProductoId()));
        Presentacion presentacion = inventarioService.obtenerPresentacion(detalle.getProductoId(), detalle.getPresentacionId());
        BigDecimal factor = BigDecimal.valueOf(presentacion.getFactorAUnidadMinima());
        BigDecimal costoEntranteUmm = detalle.getCostoUnitario().divide(factor, 4, REDONDEO);
        BigDecimal anterior = persistido.getPrecioCompra() == null
                ? BigDecimal.ZERO.setScale(2, REDONDEO)
                : persistido.getPrecioCompra();
        int stockActual = persistido.getStockActual() == null ? 0 : persistido.getStockActual();
        int cantidadEntrante = conversionUnidades.aUnidadMinima(presentacion, cantidadRecibirEmpaque);
        int stockAnterior = Math.max(0, stockActual - cantidadEntrante);

        BigDecimal costoUmm;
        String detalleAuditoria;
        if (stockAnterior <= 0 || cantidadEntrante <= 0) {
            costoUmm = costoEntranteUmm.setScale(2, REDONDEO);
            detalleAuditoria = "Costo actualizado por compra " + compra.getNumero();
        } else {
            BigDecimal stockPrev = BigDecimal.valueOf(stockAnterior);
            BigDecimal cantNueva = BigDecimal.valueOf(cantidadEntrante);
            BigDecimal valorPrevio = anterior.multiply(stockPrev);
            BigDecimal valorEntrante = costoEntranteUmm.multiply(cantNueva);
            BigDecimal stockTotal = stockPrev.add(cantNueva);
            costoUmm = valorPrevio.add(valorEntrante).divide(stockTotal, 2, REDONDEO);
            detalleAuditoria = "Costo ponderado por compra " + compra.getNumero()
                    + " (stock previo " + stockAnterior + " UMM)";
        }

        BigDecimal costoUltimoAnteriorAudit = persistido.getUltimoCostoCompra() != null
                ? persistido.getUltimoCostoCompra()
                : anterior;
        persistido.setUltimoCostoCompra(costoEntranteUmm.setScale(4, REDONDEO));
        persistido.setPrecioCompra(costoUmm);
        aplicarFechaVencimiento(persistido, detalle.getFechaVencimiento());
        auditoriaService.registrar(operador, AccionAuditoria.CAMBIO_PRECIO, "Producto", persistido.getId(),
                "cpp=" + anterior.toPlainString() + ", ultimo=" + costoUltimoAnteriorAudit.toPlainString(),
                "cpp=" + costoUmm.toPlainString() + ", ultimo=" + persistido.getUltimoCostoCompra().toPlainString(),
                detalleAuditoria);
        ResultadoPoliticaPrecio resultado = politicaPrecioService.aplicarTrasCompra(
                persistido, anterior, costoUmm, operador, compra.getNumero(), precioVentaExplicito);
        productoRepository.save(persistido);
        return resultado;
    }

    private EntradaLote datosLote(Compra compra, DetalleCompra detalle) {
        Presentacion presentacion = inventarioService.obtenerPresentacion(
                detalle.getProductoId(), detalle.getPresentacionId());
        BigDecimal factor = BigDecimal.valueOf(presentacion.getFactorAUnidadMinima());
        BigDecimal costoUmm = detalle.getCostoUnitario().divide(factor, 4, REDONDEO);
        return EntradaLote.deCompra(costoUmm, detalle.getFechaVencimiento(), compra.getId(), compra.getProveedorNombre());
    }

    private void revertirRecepcion(Compra compra, Usuario operador, String motivo) {
        String motivoBase = "Anulación compra " + compra.getNumero() + ": " + motivo;
        for (DetalleCompra detalle : compra.getDetalles()) {
            int recibida = valor(detalle.getCantidadRecibida());
            if (recibida <= 0) {
                continue;
            }
            inventarioService.descontar(
                    detalle.getProductoId(),
                    detalle.getPresentacionId(),
                    recibida,
                    TipoMovimiento.AJUSTE,
                    motivoBase,
                    operador.getId(),
                    compra.getId(),
                    null,
                    null
            );
        }
    }

    private void actualizarVencimiento(DetalleCompra detalle) {
        if (detalle.getFechaVencimiento() == null) {
            return;
        }
        productoRepository.findById(detalle.getProductoId()).ifPresent(producto -> {
            aplicarFechaVencimiento(producto, detalle.getFechaVencimiento());
            productoRepository.save(producto);
        });
    }

    private LineaConstruida validarYConstruirLinea(DetalleCompraDTO linea, Long proveedorId, LocalDate hoy) {
        Producto producto = productoRepository.findById(linea.getProductoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + linea.getProductoId()));
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ReglaNegocioException("PRODUCTO_INACTIVO", "El producto no está activo: " + producto.getNombre());
        }
        if (linea.getFechaVencimiento() != null && linea.getFechaVencimiento().isBefore(hoy)) {
            throw new ReglaNegocioException("VENCIMIENTO_PASADO",
                    "La fecha de vencimiento no puede ser anterior a hoy para " + producto.getNombre());
        }
        Presentacion presentacion = inventarioService.obtenerPresentacion(linea.getProductoId(), linea.getPresentacionId());
        int cantidadUmm = conversionUnidades.aUnidadMinima(presentacion, linea.getCantidad());
        BigDecimal costo = linea.getCostoUnitario().setScale(2, REDONDEO);
        BigDecimal subtotal = costo.multiply(BigDecimal.valueOf(linea.getCantidad())).setScale(2, REDONDEO);
        DetalleCompra detalle = DetalleCompra.builder()
                .productoId(producto.getId())
                .presentacionId(presentacion.getId())
                .cantidad(linea.getCantidad())
                .cantidadOrdenada(linea.getCantidad())
                .cantidadRecibida(0)
                .cantidadRechazada(0)
                .cantidadUmm(cantidadUmm)
                .costoUnitario(costo)
                .subtotal(subtotal)
                .fechaVencimiento(linea.getFechaVencimiento())
                .build();
        return new LineaConstruida(detalle, subtotal);
    }

    private EstadoCompra calcularEstadoTrasRecepcion(Compra compra) {
        boolean completa = compra.getDetalles().stream()
                .allMatch(det -> cantidadPendiente(det) <= 0);
        return completa ? EstadoCompra.RECIBIDA : EstadoCompra.PARCIAL;
    }

    private int cantidadPendiente(DetalleCompra detalle) {
        normalizarDetalleLegacy(detalle);
        int ordenada = detalle.getCantidadOrdenada() == null ? valor(detalle.getCantidad()) : detalle.getCantidadOrdenada();
        return Math.max(0, ordenada - valor(detalle.getCantidadRecibida()) - valor(detalle.getCantidadRechazada()));
    }

    private void normalizarDetalleLegacy(DetalleCompra detalle) {
        if (detalle.getCantidadOrdenada() == null) {
            detalle.setCantidadOrdenada(detalle.getCantidad());
        }
        Compra compra = detalle.getCompra();
        if (compra != null
                && (compra.getEstado() == EstadoCompra.RECIBIDA || compra.getEstado() == EstadoCompra.CERRADA)
                && valor(detalle.getCantidadRecibida()) == 0
                && valor(detalle.getCantidadRechazada()) == 0) {
            detalle.setCantidadRecibida(detalle.getCantidadOrdenada());
        }
    }

    private Map<Long, RecepcionLineaDTO> indexarLineasRecepcion(RecepcionCompraRequestDTO request) {
        Map<Long, RecepcionLineaDTO> mapa = new HashMap<>();
        if (request == null || request.getLineas() == null) {
            return mapa;
        }
        for (RecepcionLineaDTO linea : request.getLineas()) {
            if (linea.getDetalleId() != null) {
                mapa.put(linea.getDetalleId(), linea);
            }
        }
        return mapa;
    }

    private RecepcionLineaDTO resolverLineaRecepcion(DetalleCompra detalle, Map<Long, RecepcionLineaDTO> porDetalle) {
        if (detalle.getId() != null && porDetalle.containsKey(detalle.getId())) {
            return porDetalle.get(detalle.getId());
        }
        if (porDetalle.isEmpty()) {
            return null;
        }
        return porDetalle.values().stream()
                .filter(linea -> linea.getDetalleId() == null
                        && detalle.getProductoId().equals(linea.getProductoId())
                        && detalle.getPresentacionId().equals(linea.getPresentacionId()))
                .findFirst()
                .orElse(null);
    }

    private void exigirDocumentoUnico(Long proveedorId, String documento) {
        if (documento == null || documento.isBlank()) {
            return;
        }
        if (compraRepository.existsByProveedorIdAndDocumentoProveedorIgnoreCaseAndEstadoIn(
                proveedorId, documento.trim(), ESTADOS_DOC_ACTIVOS)) {
            throw new ReglaNegocioException("DOCUMENTO_DUPLICADO",
                    "Ya existe una compra activa con el documento " + documento.trim() + " para este proveedor");
        }
    }

    private String siguienteNumero() {
        return "C-" + LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private void aplicarFechaVencimiento(Producto producto, LocalDate nueva) {
        if (nueva == null) {
            return;
        }
        LocalDate hoy = LocalDate.now(clock);
        LocalDate actual = producto.getFechaVencimiento();
        if (actual == null || !actual.isAfter(hoy) || !nueva.isAfter(actual)) {
            producto.setFechaVencimiento(nueva);
        }
    }

    private void sincronizarPrecioCatalogo(Long proveedorId, Long productoId, Long presentacionId, BigDecimal costo) {
        if (proveedorId == null || productoId == null || presentacionId == null || costo == null) {
            return;
        }
        BigDecimal precio = costo.setScale(2, REDONDEO);
        precioProveedorRepository
                .findByProveedorIdAndProductoIdAndPresentacionId(proveedorId, productoId, presentacionId)
                .ifPresentOrElse(catalogo -> {
                    catalogo.setPrecioUnitario(precio);
                    catalogo.setActivo(true);
                    precioProveedorRepository.save(catalogo);
                }, () -> precioProveedorRepository.save(PrecioProveedor.builder()
                        .proveedorId(proveedorId)
                        .productoId(productoId)
                        .presentacionId(presentacionId)
                        .precioUnitario(precio)
                        .activo(true)
                        .build()));
    }

    private CompraDTO toDto(Compra compra) {
        boolean recibible = compra.getEstado() == EstadoCompra.PENDIENTE || compra.getEstado() == EstadoCompra.PARCIAL;
        boolean anulable = compra.getEstado() != EstadoCompra.ANULADA;
        boolean cerrable = compra.getEstado() == EstadoCompra.PARCIAL;
        return CompraDTO.builder()
                .id(compra.getId())
                .numero(compra.getNumero())
                .proveedorId(compra.getProveedorId())
                .proveedorNombre(compra.getProveedorNombre())
                .documentoProveedor(compra.getDocumentoProveedor())
                .fecha(compra.getFecha())
                .total(compra.getTotal())
                .estado(compra.getEstado())
                .usuarioId(compra.getUsuarioId())
                .usuarioNombre(nombreUsuario(compra.getUsuarioId()))
                .observacion(compra.getObservacion())
                .detalles(compra.getDetalles().stream().map(this::toLinea).toList())
                .recibible(recibible)
                .motivoNoRecibible(recibible ? null : "La orden no admite más recepciones")
                .anulable(anulable)
                .motivoNoAnulable(anulable ? null : "La compra ya está anulada")
                .cerrable(cerrable)
                .motivoNoCerrable(cerrable ? null : "Solo órdenes parciales pueden cerrarse")
                .build();
    }

    private DetalleCompraResponseDTO toLinea(DetalleCompra detalle) {
        normalizarDetalleLegacy(detalle);
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
        int ordenada = detalle.getCantidadOrdenada() == null ? valor(detalle.getCantidad()) : detalle.getCantidadOrdenada();
        return DetalleCompraResponseDTO.builder()
                .id(detalle.getId())
                .productoId(detalle.getProductoId())
                .productoNombre(productoNombre)
                .presentacionId(detalle.getPresentacionId())
                .presentacionNombre(presentacionNombre)
                .cantidad(detalle.getCantidad())
                .cantidadOrdenada(ordenada)
                .cantidadRecibida(valor(detalle.getCantidadRecibida()))
                .cantidadRechazada(valor(detalle.getCantidadRechazada()))
                .cantidadPendiente(cantidadPendiente(detalle))
                .notasQc(detalle.getNotasQc())
                .cantidadUmm(detalle.getCantidadUmm())
                .costoUnitario(detalle.getCostoUnitario())
                .subtotal(detalle.getSubtotal())
                .fechaVencimiento(detalle.getFechaVencimiento())
                .build();
    }

    private int valor(Integer numero) {
        return numero == null ? 0 : numero;
    }

    private String nombreUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return usuarioRepository.findById(usuarioId)
                .map(Usuario::getNombreCompleto)
                .orElse(null);
    }

    private String texto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private String unirObservacion(String actual, String extra) {
        if (actual == null || actual.isBlank()) {
            return extra;
        }
        return actual + " | " + extra;
    }

    private record LineaConstruida(DetalleCompra detalle, BigDecimal subtotal) {
    }
}
