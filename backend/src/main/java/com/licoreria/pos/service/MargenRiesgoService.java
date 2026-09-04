package com.licoreria.pos.service;

import com.licoreria.pos.dto.MargenRiesgoItemDTO;
import com.licoreria.pos.dto.MargenRiesgoResumenDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.model.ListaPrecio;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.repository.ListaPrecioRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Compara costo real contra precio de venta para detectar productos que se están
 * vendiendo con margen insuficiente o directamente a pérdida.
 * En licorería el costo se mueve con cada compra, así que el precio queda desfasado si nadie revisa.
 */
@Service
@RequiredArgsConstructor
public class MargenRiesgoService {

    private static final BigDecimal MARGEN_OBJETIVO_DEFECTO = new BigDecimal("20");
    private static final BigDecimal MARGEN_CRITICO = new BigDecimal("5");
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final ProductoRepository productoRepository;
    private final ListaPrecioRepository listaPrecioRepository;
    private final LoteService loteService;
    private final AccesoService accesoService;

    @Transactional(readOnly = true)
    public MargenRiesgoResumenDTO analizar(BigDecimal margenObjetivoPorcentaje, boolean soloRiesgo) {
        return analizar(margenObjetivoPorcentaje, soloRiesgo, 0, PaginacionUtil.TAMANO_MAX);
    }

    @Transactional(readOnly = true)
    public MargenRiesgoResumenDTO analizar(
            BigDecimal margenObjetivoPorcentaje, boolean soloRiesgo, int pagina, int tamano) {
        accesoService.exigirPermiso(Permiso.FINANZAS_VER);
        BigDecimal objetivo = objetivo(margenObjetivoPorcentaje);

        List<MargenRiesgoItemDTO> items = new ArrayList<>();
        BigDecimal capitalRiesgo = BigDecimal.ZERO;
        BigDecimal valorInventario = BigDecimal.ZERO;
        BigDecimal margenPonderadoNumerador = BigDecimal.ZERO;
        BigDecimal unidadesConCosto = BigDecimal.ZERO;

        for (Producto producto : productoRepository.findByActivoTrue()) {
            MargenRiesgoItemDTO item = evaluar(producto, objetivo);
            items.add(item);

            BigDecimal stock = BigDecimal.valueOf(item.getStockActual() == null ? 0 : item.getStockActual());
            if (item.getCostoUmm() != null) {
                valorInventario = valorInventario.add(item.getCostoUmm().multiply(stock));
            }
            if (item.getMargenDetalPorcentaje() != null && stock.signum() > 0) {
                margenPonderadoNumerador = margenPonderadoNumerador
                        .add(item.getMargenDetalPorcentaje().multiply(stock));
                unidadesConCosto = unidadesConCosto.add(stock);
            }
            if (item.getCapitalEnRiesgo() != null) {
                capitalRiesgo = capitalRiesgo.add(item.getCapitalEnRiesgo());
            }
        }

        List<MargenRiesgoItemDTO> visibles = items.stream()
                .filter(item -> !soloRiesgo || !"SANO".equals(item.getNivel()))
                .sorted(Comparator
                        .comparingInt((MargenRiesgoItemDTO item) -> prioridad(item.getNivel()))
                        .thenComparing(MargenRiesgoItemDTO::getMargenDetalPorcentaje,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

        PaginaDTO<MargenRiesgoItemDTO> paginaItems = PaginacionUtil.deLista(visibles, pagina, tamano);
        return MargenRiesgoResumenDTO.builder()
                .margenObjetivoPorcentaje(objetivo)
                .productosEvaluados(items.size())
                .productosEnPerdida(contar(items, "PERDIDA"))
                .productosCriticos(contar(items, "CRITICO"))
                .productosBajos(contar(items, "BAJO"))
                .productosSanos(contar(items, "SANO"))
                .margenPromedioPonderado(unidadesConCosto.signum() == 0
                        ? null
                        : margenPonderadoNumerador.divide(unidadesConCosto, 2, REDONDEO))
                .capitalEnRiesgo(capitalRiesgo.setScale(2, REDONDEO))
                .valorInventarioCosto(valorInventario.setScale(2, REDONDEO))
                .items(paginaItems.getContenido())
                .pagina(paginaItems.getPagina())
                .tamano(paginaItems.getTamano())
                .totalElementos(paginaItems.getTotalElementos())
                .totalPaginas(paginaItems.getTotalPaginas())
                .build();
    }

    private MargenRiesgoItemDTO evaluar(Producto producto, BigDecimal objetivo) {
        BigDecimal costoCatalogo = producto.getPrecioCompra();
        BigDecimal costoLotes = loteService.costoPromedioLotes(producto.getId());
        BigDecimal costoReferencia = costoLotes != null ? costoLotes : costoCatalogo;

        BigDecimal precioDetal = precioLista(producto, TipoCliente.DETAL, producto.getPrecioVenta());
        BigDecimal precioMayorista = precioLista(producto, TipoCliente.MAYORISTA, null);

        BigDecimal margenDetal = precioDetal == null || costoReferencia == null
                ? null
                : precioDetal.subtract(costoReferencia).setScale(2, REDONDEO);
        BigDecimal margenDetalPct = porcentaje(margenDetal, precioDetal);
        BigDecimal margenMayoristaPct = precioMayorista == null || costoReferencia == null
                ? null
                : porcentaje(precioMayorista.subtract(costoReferencia), precioMayorista);

        String nivel = clasificar(margenDetalPct, objetivo, costoReferencia, precioDetal);
        int stock = producto.getStockActual() == null ? 0 : producto.getStockActual();

        return MargenRiesgoItemDTO.builder()
                .productoId(producto.getId())
                .codigo(producto.getCodigo())
                .nombre(producto.getNombre())
                .marca(producto.getMarca())
                .stockActual(stock)
                .costoUmm(costoCatalogo)
                .costoLotesUmm(costoLotes)
                .precioDetalUmm(precioDetal)
                .precioMayoristaUmm(precioMayorista)
                .margenDetal(margenDetal)
                .margenDetalPorcentaje(margenDetalPct)
                .margenMayoristaPorcentaje(margenMayoristaPct)
                .nivel(nivel)
                .motivo(motivo(nivel, objetivo, costoLotes, costoCatalogo))
                .precioSugeridoUmm(precioSugerido(costoReferencia, objetivo))
                .capitalEnRiesgo(capitalEnRiesgo(nivel, costoReferencia, stock))
                .build();
    }

    /**
     * Margen sobre precio de venta (no sobre costo): es el criterio con el que
     * se mide la rentabilidad del ticket en retail.
     */
    private BigDecimal porcentaje(BigDecimal margen, BigDecimal precio) {
        if (margen == null || precio == null || precio.signum() == 0) {
            return null;
        }
        return margen.multiply(CIEN).divide(precio, 2, REDONDEO);
    }

    private String clasificar(BigDecimal margenPct, BigDecimal objetivo, BigDecimal costo, BigDecimal precio) {
        if (costo == null || costo.signum() == 0 || precio == null) {
            return "CRITICO";
        }
        if (margenPct == null) {
            return "CRITICO";
        }
        if (margenPct.signum() <= 0) {
            return "PERDIDA";
        }
        if (margenPct.compareTo(MARGEN_CRITICO) < 0) {
            return "CRITICO";
        }
        return margenPct.compareTo(objetivo) < 0 ? "BAJO" : "SANO";
    }

    private String motivo(String nivel, BigDecimal objetivo, BigDecimal costoLotes, BigDecimal costoCatalogo) {
        boolean costoDesfasado = costoLotes != null && costoCatalogo != null
                && costoLotes.compareTo(costoCatalogo) != 0;
        return switch (nivel) {
            case "PERDIDA" -> "Se vende por debajo del costo: corrija el precio antes de seguir vendiendo";
            case "CRITICO" -> costoDesfasado
                    ? "Margen casi nulo y el costo de los lotes difiere del catálogo"
                    : "Margen casi nulo: revise costo y precio";
            case "BAJO" -> "Margen bajo el objetivo de " + objetivo.stripTrailingZeros().toPlainString() + "%";
            default -> costoDesfasado ? "Margen sano, pero el costo del catálogo está desfasado" : null;
        };
    }

    private BigDecimal precioSugerido(BigDecimal costo, BigDecimal objetivo) {
        if (costo == null || costo.signum() == 0) {
            return null;
        }
        BigDecimal divisor = CIEN.subtract(objetivo);
        if (divisor.signum() <= 0) {
            return null;
        }
        return costo.multiply(CIEN).divide(divisor, 2, RoundingMode.CEILING);
    }

    /** Costo inmovilizado en productos que hoy no dejan margen. */
    private BigDecimal capitalEnRiesgo(String nivel, BigDecimal costo, int stock) {
        if (costo == null || stock <= 0 || "SANO".equals(nivel) || "BAJO".equals(nivel)) {
            return null;
        }
        return costo.multiply(BigDecimal.valueOf(stock)).setScale(2, REDONDEO);
    }

    private BigDecimal precioLista(Producto producto, TipoCliente tipo, BigDecimal porDefecto) {
        return listaPrecioRepository.findByProductoIdAndTipoCliente(producto.getId(), tipo)
                .map(ListaPrecio::getPrecioUmm)
                .orElse(porDefecto);
    }

    private BigDecimal objetivo(BigDecimal solicitado) {
        if (solicitado == null || solicitado.signum() <= 0 || solicitado.compareTo(CIEN) >= 0) {
            return MARGEN_OBJETIVO_DEFECTO;
        }
        return solicitado.setScale(2, REDONDEO);
    }

    private int prioridad(String nivel) {
        return switch (nivel) {
            case "PERDIDA" -> 0;
            case "CRITICO" -> 1;
            case "BAJO" -> 2;
            default -> 3;
        };
    }

    private int contar(List<MargenRiesgoItemDTO> items, String nivel) {
        return (int) items.stream().filter(item -> nivel.equals(item.getNivel())).count();
    }
}
