package com.licoreria.pos.service;

import com.licoreria.pos.dto.HistorialPrecioDTO;
import com.licoreria.pos.dto.MarcaResumenDTO;
import com.licoreria.pos.dto.MarcasPreciosResumenDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.ProductoPrecioCatalogoDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Auditoria;
import com.licoreria.pos.model.ListaPrecio;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.AuditoriaRepository;
import com.licoreria.pos.repository.ListaPrecioRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarcasPreciosService {

    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final Pattern PARES_PRECIO = Pattern.compile("([A-Za-z_]+)=([^,]+)");

    private final ProductoRepository productoRepository;
    private final ListaPrecioRepository listaPrecioRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaService categoriaService;
    private final AccesoService accesoService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MarcasPreciosResumenDTO resumen() {
        List<Producto> productos = productoRepository.findAll();
        long conMarca = productos.stream().filter(p -> tieneMarca(p.getMarca())).count();
        Map<String, Long> marcas = productos.stream()
                .filter(p -> tieneMarca(p.getMarca()))
                .collect(Collectors.groupingBy(p -> normalizarMarca(p.getMarca()), Collectors.counting()));

        LocalDateTime inicioMes = LocalDate.now(clock).withDayOfMonth(1).atStartOfDay();
        long cambiosMes = auditoriaRepository.findHistorialPrecios(
                AccionAuditoria.CAMBIO_PRECIO, inicioMes, null).size();

        return MarcasPreciosResumenDTO.builder()
                .marcasRegistradas(marcas.size())
                .productosConMarca((int) conMarca)
                .productosSinMarca(productos.size() - (int) conMarca)
                .cambiosPrecioMes(cambiosMes)
                .cambiosPrecioTotal(auditoriaRepository.countByAccion(AccionAuditoria.CAMBIO_PRECIO))
                .build();
    }

    @Transactional(readOnly = true)
    public List<MarcaResumenDTO> listarMarcas(String busqueda) {
        String termino = busqueda == null || busqueda.isBlank() ? null : busqueda.trim().toLowerCase(Locale.ROOT);
        Map<String, List<Producto>> porMarca = productoRepository.findAll().stream()
                .filter(p -> tieneMarca(p.getMarca()))
                .collect(Collectors.groupingBy(p -> normalizarMarca(p.getMarca())));

        List<MarcaResumenDTO> marcas = new ArrayList<>();
        porMarca.forEach((nombre, items) -> {
            if (termino != null && !nombre.toLowerCase(Locale.ROOT).contains(termino)) {
                return;
            }
            int activos = (int) items.stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).count();
            BigDecimal compraProm = promedio(items.stream().map(Producto::getPrecioCompra).toList());
            BigDecimal ventaProm = promedio(items.stream().map(Producto::getPrecioVenta).toList());
            marcas.add(MarcaResumenDTO.builder()
                    .nombre(nombre)
                    .productosTotal(items.size())
                    .productosActivos(activos)
                    .precioCompraPromedio(compraProm)
                    .precioVentaPromedio(ventaProm)
                    .margenPromedioPct(porcentaje(ventaProm.subtract(compraProm), ventaProm))
                    .build());
        });

        marcas.sort(Comparator.comparing(MarcaResumenDTO::getProductosTotal, Comparator.reverseOrder())
                .thenComparing(MarcaResumenDTO::getNombre, String.CASE_INSENSITIVE_ORDER));
        return marcas;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<MarcaResumenDTO> listarMarcas(String busqueda, int pagina, int tamano) {
        return PaginacionUtil.deLista(listarMarcas(busqueda), pagina, tamano);
    }

    @Transactional(readOnly = true)
    public List<HistorialPrecioDTO> historial(
            LocalDate desde,
            LocalDate hasta,
            String busqueda,
            String marca,
            Long productoId,
            String tipoCambio) {
        RangoFechas rango = validarRango(desde, hasta);
        List<Auditoria> eventos = auditoriaRepository.findHistorialPrecios(
                AccionAuditoria.CAMBIO_PRECIO, rango.desde(), rango.hasta());

        Map<Long, Producto> productos = productoRepository.findAll().stream()
                .collect(Collectors.toMap(Producto::getId, p -> p, (a, b) -> a));
        Map<Long, String> usuarios = cargarUsuarios(eventos);
        Map<Long, LocalDateTime> ultimoCambio = new HashMap<>();

        String marcaFiltro = marca == null || marca.isBlank() ? null : normalizarMarca(marca);
        String tipoFiltro = tipoCambio == null || tipoCambio.isBlank() ? null : tipoCambio.trim().toUpperCase(Locale.ROOT);
        String termino = busqueda == null || busqueda.isBlank() ? null : busqueda.trim().toLowerCase(Locale.ROOT);

        return eventos.stream()
                .map(evento -> mapearHistorial(evento, productos, usuarios))
                .filter(item -> productoId == null || Objects.equals(productoId, item.getProductoId()))
                .filter(item -> marcaFiltro == null || marcaFiltro.equalsIgnoreCase(normalizarMarca(item.getMarca())))
                .filter(item -> tipoFiltro == null || tipoFiltro.equals(item.getTipoCambio()))
                .filter(item -> coincideBusqueda(termino, item))
                .peek(item -> {
                    if (item.getProductoId() != null && item.getFechaHora() != null) {
                        ultimoCambio.merge(item.getProductoId(), item.getFechaHora(),
                                (a, b) -> a.isAfter(b) ? a : b);
                    }
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<HistorialPrecioDTO> historial(
            LocalDate desde,
            LocalDate hasta,
            String busqueda,
            String marca,
            Long productoId,
            String tipoCambio,
            int pagina,
            int tamano) {
        return PaginacionUtil.deLista(
                historial(desde, hasta, busqueda, marca, productoId, tipoCambio), pagina, tamano);
    }

    @Transactional(readOnly = true)
    public List<ProductoPrecioCatalogoDTO> catalogoPrecios(String busqueda, String marca, Boolean activo) {
        String termino = busqueda == null || busqueda.isBlank() ? null : busqueda.trim();
        String marcaFiltro = marca == null || marca.isBlank() ? null : normalizarMarca(marca);

        Map<Long, LocalDateTime> ultimos = ultimosCambiosPorProducto();
        Map<Long, BigDecimal> mayoristas = listaPrecioRepository.findAll().stream()
                .filter(lp -> lp.getTipoCliente() == TipoCliente.MAYORISTA)
                .collect(Collectors.toMap(ListaPrecio::getProductoId, ListaPrecio::getPrecioUmm, (a, b) -> b));

        return productoRepository.buscar(termino, null, activo, null).stream()
                .filter(p -> marcaFiltro == null || marcaFiltro.equalsIgnoreCase(normalizarMarca(p.getMarca())))
                .map(p -> {
                    BigDecimal compra = nvl(p.getPrecioCompra());
                    BigDecimal venta = nvl(p.getPrecioVenta());
                    return ProductoPrecioCatalogoDTO.builder()
                            .id(p.getId())
                            .codigo(p.getCodigo())
                            .nombre(p.getNombre())
                            .marca(normalizarMarca(p.getMarca()))
                            .categoriaNombre(categoriaService.nombrePorId(p.getCategoriaId()))
                            .precioCompra(compra)
                            .precioVenta(venta)
                            .precioMayorista(mayoristas.get(p.getId()))
                            .margenPct(porcentaje(venta.subtract(compra), venta))
                            .activo(p.getActivo())
                            .ultimoCambioPrecio(ultimos.get(p.getId()))
                            .build();
                })
                .sorted(Comparator.comparing(ProductoPrecioCatalogoDTO::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ProductoPrecioCatalogoDTO> catalogoPrecios(
            String busqueda, String marca, Boolean activo, int pagina, int tamano) {
        return PaginacionUtil.deLista(catalogoPrecios(busqueda, marca, activo), pagina, tamano);
    }

    @Transactional
    public int renombrarMarca(String marcaActual, String marcaNueva) {
        accesoService.exigirPermiso(Permiso.MARCAS_PRECIOS_GESTIONAR);
        if (marcaActual == null || marcaActual.isBlank()) {
            throw new ReglaNegocioException("MARCA_INVALIDA", "Indique la marca actual");
        }
        if (marcaNueva == null || marcaNueva.isBlank()) {
            throw new ReglaNegocioException("MARCA_INVALIDA", "Indique la marca nueva");
        }
        String actual = normalizarMarca(marcaActual);
        String nueva = normalizarMarca(marcaNueva);
        if (actual.equalsIgnoreCase(nueva)) {
            return 0;
        }

        int actualizados = 0;
        for (Producto producto : productoRepository.findAll()) {
            if (actual.equalsIgnoreCase(normalizarMarca(producto.getMarca()))) {
                producto.setMarca(nueva);
                productoRepository.save(producto);
                actualizados++;
            }
        }
        if (actualizados == 0) {
            throw new ReglaNegocioException("MARCA_NO_ENCONTRADA", "No hay productos con la marca indicada");
        }
        return actualizados;
    }

    static HistorialPrecioDTO mapearHistorial(Auditoria evento, Map<Long, Producto> productos, Map<Long, String> usuarios) {
        Long productoId = resolverProductoId(evento);
        Producto producto = productoId == null ? null : productos.get(productoId);
        TipoHistorial tipo = clasificar(evento);

        return HistorialPrecioDTO.builder()
                .id(evento.getId())
                .fechaHora(evento.getFechaHora())
                .usuarioId(evento.getUsuarioId())
                .usuarioNombre(evento.getUsuarioId() == null ? null : usuarios.get(evento.getUsuarioId()))
                .productoId(productoId)
                .productoCodigo(producto == null ? null : producto.getCodigo())
                .productoNombre(producto == null ? detalleProducto(evento) : producto.getNombre())
                .marca(producto == null ? null : normalizarMarca(producto.getMarca()))
                .tipoCambio(tipo.tipo())
                .origen(tipo.origen())
                .valorAnterior(evento.getValorAnterior())
                .valorNuevo(evento.getValorNuevo())
                .variacionPct(calcularVariacion(evento, tipo))
                .detalle(evento.getDetalle())
                .build();
    }

    private static Long resolverProductoId(Auditoria evento) {
        if ("Producto".equalsIgnoreCase(evento.getEntidad())) {
            return evento.getEntidadId();
        }
        return null;
    }

    private static TipoHistorial clasificar(Auditoria evento) {
        String detalle = evento.getDetalle() == null ? "" : evento.getDetalle().toLowerCase(Locale.ROOT);
        String nuevo = evento.getValorNuevo() == null ? "" : evento.getValorNuevo();

        if (detalle.contains("markup automático")) {
            return new TipoHistorial("AUTO_MARKUP_COMPRA", "Compras");
        }
        if (detalle.contains("precio sugerido")) {
            return new TipoHistorial("SUGERIDO_COMPRA", "Compras");
        }
        if (detalle.contains("lista de precios")) {
            if (nuevo.toUpperCase(Locale.ROOT).startsWith("MAYORISTA")) {
                return new TipoHistorial("LISTA_MAYORISTA", "Catálogo");
            }
            if (nuevo.toUpperCase(Locale.ROOT).startsWith("DETAL")) {
                return new TipoHistorial("LISTA_DETAL", "Catálogo");
            }
            return new TipoHistorial("LISTA_PRECIO", "Catálogo");
        }
        if (detalle.contains("compra")) {
            return new TipoHistorial("COSTO_COMPRA", "Compras");
        }
        if (detalle.contains("override") || detalle.contains("venta")) {
            return new TipoHistorial("OVERRIDE_VENTA", "POS");
        }
        if (contieneClave(evento.getValorAnterior(), "compra") || contieneClave(evento.getValorNuevo(), "compra")) {
            if (contieneClave(evento.getValorAnterior(), "venta") || contieneClave(evento.getValorNuevo(), "venta")) {
                return new TipoHistorial("COMPRA_VENTA", "Catálogo");
            }
            return new TipoHistorial("COSTO", "Catálogo");
        }
        if (contieneClave(evento.getValorAnterior(), "venta") || contieneClave(evento.getValorNuevo(), "venta")) {
            return new TipoHistorial("VENTA", "Catálogo");
        }
        return new TipoHistorial("PRECIO", "Sistema");
    }

    private static BigDecimal calcularVariacion(Auditoria evento, TipoHistorial tipo) {
        BigDecimal anterior = extraerMonto(evento.getValorAnterior(), tipo);
        BigDecimal nuevo = extraerMonto(evento.getValorNuevo(), tipo);
        if (anterior == null || nuevo == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return nuevo.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior.abs(), 2, REDONDEO);
    }

    private static BigDecimal extraerMonto(String valor, TipoHistorial tipo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        Map<String, String> pares = parsePares(valor);
        if ("COSTO_COMPRA".equals(tipo.tipo()) || "COSTO".equals(tipo.tipo())) {
            if (pares.containsKey("compra")) {
                return parseDecimal(pares.get("compra"));
            }
            return parseDecimal(valor);
        }
        if ("VENTA".equals(tipo.tipo())) {
            return parseDecimal(pares.getOrDefault("venta", valor));
        }
        if (tipo.tipo().startsWith("LISTA")) {
            int idx = valor.indexOf('=');
            if (idx >= 0) {
                return parseDecimal(valor.substring(idx + 1));
            }
        }
        if ("OVERRIDE_VENTA".equals(tipo.tipo())) {
            return parseDecimal(valor);
        }
        if (pares.containsKey("venta")) {
            return parseDecimal(pares.get("venta"));
        }
        if (pares.containsKey("compra")) {
            return parseDecimal(pares.get("compra"));
        }
        return parseDecimal(valor);
    }

    static Map<String, String> parsePares(String valor) {
        Map<String, String> mapa = new HashMap<>();
        if (valor == null) {
            return mapa;
        }
        Matcher matcher = PARES_PRECIO.matcher(valor);
        while (matcher.find()) {
            mapa.put(matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2).trim());
        }
        return mapa;
    }

    private static boolean contieneClave(String valor, String clave) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(clave + "=");
    }

    private static BigDecimal parseDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(valor.trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<Long, LocalDateTime> ultimosCambiosPorProducto() {
        Map<Long, LocalDateTime> mapa = new HashMap<>();
        for (Auditoria evento : auditoriaRepository.findByAccionOrderByFechaHoraDesc(AccionAuditoria.CAMBIO_PRECIO)) {
            Long productoId = resolverProductoId(evento);
            if (productoId != null && evento.getFechaHora() != null && !mapa.containsKey(productoId)) {
                mapa.put(productoId, evento.getFechaHora());
            }
        }
        return mapa;
    }

    private Map<Long, String> cargarUsuarios(List<Auditoria> eventos) {
        List<Long> ids = eventos.stream()
                .map(Auditoria::getUsuarioId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombreCompleto));
    }

    private boolean coincideBusqueda(String termino, HistorialPrecioDTO item) {
        if (termino == null) {
            return true;
        }
        return contiene(item.getProductoCodigo(), termino)
                || contiene(item.getProductoNombre(), termino)
                || contiene(item.getMarca(), termino)
                || contiene(item.getDetalle(), termino)
                || contiene(item.getUsuarioNombre(), termino);
    }

    private static boolean contiene(String valor, String termino) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(termino);
    }

    private static String detalleProducto(Auditoria evento) {
        if (evento.getDetalle() == null) {
            return "Producto";
        }
        return evento.getDetalle();
    }

    private RangoFechas validarRango(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate inicio = desde == null ? hoy.minusDays(30) : desde;
        LocalDate fin = hasta == null ? hoy : hasta;
        if (fin.isBefore(inicio)) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "La fecha final no puede ser anterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > 366) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "El rango no puede superar 366 días");
        }
        return new RangoFechas(inicio.atStartOfDay(), fin.atTime(LocalTime.MAX));
    }

    static boolean tieneMarca(String marca) {
        return marca != null && !marca.isBlank();
    }

    static String normalizarMarca(String marca) {
        if (marca == null || marca.isBlank()) {
            return "Sin marca";
        }
        return marca.trim();
    }

    static BigDecimal promedio(List<BigDecimal> valores) {
        List<BigDecimal> validos = valores.stream().filter(Objects::nonNull).toList();
        if (validos.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal suma = validos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(validos.size()), 2, REDONDEO);
    }

    static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return nvl(parte).multiply(BigDecimal.valueOf(100)).divide(total, 2, REDONDEO);
    }

    static BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private record RangoFechas(LocalDateTime desde, LocalDateTime hasta) {
    }

    private record TipoHistorial(String tipo, String origen) {
    }
}
