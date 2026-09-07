package com.licoreria.pos.service;

import com.licoreria.pos.dto.CompraSugerenciaItemDTO;
import com.licoreria.pos.model.DetalleVenta;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CompraSugerenciaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final AccesoService accesoService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<CompraSugerenciaItemDTO> sugerir(int diasHistorial, int diasCobertura) {
        accesoService.exigirAlguno(Permiso.COMPRAS_GESTIONAR, Permiso.INVENTARIO_VER);
        int hist = Math.max(7, Math.min(diasHistorial, 90));
        int cobertura = Math.max(3, Math.min(diasCobertura, 60));

        LocalDate hoy = LocalDate.now(clock);
        LocalDateTime inicio = hoy.minusDays(hist - 1L).atStartOfDay();
        LocalDateTime fin = hoy.atTime(LocalTime.MAX);

        Map<Long, Integer> vendidoUmm = new HashMap<>();
        for (Venta venta : ventaRepository.findByFechaBetween(inicio, fin)) {
            if (venta.getEstado() != EstadoVenta.COMPLETADA) {
                continue;
            }
            for (DetalleVenta d : venta.getDetalles()) {
                vendidoUmm.merge(d.getProductoId(), d.getCantidadUmm() == null ? 0 : d.getCantidadUmm(), Integer::sum);
            }
        }

        List<CompraSugerenciaItemDTO> out = new ArrayList<>();
        for (Producto producto : productoRepository.findAll()) {
            if (Boolean.FALSE.equals(producto.getActivo())) {
                continue;
            }
            int stock = producto.getStockActual() == null ? 0 : producto.getStockActual();
            int vendido = vendidoUmm.getOrDefault(producto.getId(), 0);
            BigDecimal promedio = BigDecimal.valueOf(vendido)
                    .divide(BigDecimal.valueOf(hist), 4, RoundingMode.HALF_UP);
            int objetivo = promedio.multiply(BigDecimal.valueOf(cobertura))
                    .setScale(0, RoundingMode.CEILING)
                    .intValue();
            int sugerido = Math.max(0, objetivo - stock);

            String motivo = null;
            if (sugerido > 0 && vendido > 0) {
                motivo = "Ritmo de venta: ~" + promedio.setScale(1, RoundingMode.HALF_UP)
                        + " UMM/día · cobertura " + cobertura + " días";
            } else if (stock <= 0 && producto.getStockMinimo() != null) {
                sugerido = Math.max(sugerido, producto.getStockMinimo());
                motivo = "Sin stock; reponer al mínimo";
            } else if (producto.getStockMinimo() != null && stock < producto.getStockMinimo()) {
                sugerido = Math.max(sugerido, producto.getStockMinimo() - stock);
                motivo = "Bajo el stock mínimo (" + producto.getStockMinimo() + ")";
            }

            if (sugerido <= 0) {
                continue;
            }

            Presentacion umm = presentacionUmm(producto);
            out.add(CompraSugerenciaItemDTO.builder()
                    .productoId(producto.getId())
                    .codigo(producto.getCodigo())
                    .nombre(producto.getNombre())
                    .presentacionId(umm == null ? null : umm.getId())
                    .presentacionNombre(umm == null ? "UMM" : umm.getNombre())
                    .stockActual(stock)
                    .vendidoPeriodoUmm(vendido)
                    .promedioDiarioUmm(promedio.setScale(2, RoundingMode.HALF_UP))
                    .sugeridoUmm(sugerido)
                    .diasCobertura(cobertura)
                    .motivo(motivo)
                    .build());
        }

        out.sort(Comparator
                .comparing(CompraSugerenciaItemDTO::getSugeridoUmm, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CompraSugerenciaItemDTO::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)));
        return out;
    }

    private static Presentacion presentacionUmm(Producto producto) {
        if (producto.getPresentaciones() == null) {
            return null;
        }
        return producto.getPresentaciones().stream()
                .filter(p -> p.getActivo() == null || Boolean.TRUE.equals(p.getActivo()))
                .filter(p -> p.getFactorAUnidadMinima() != null && p.getFactorAUnidadMinima() == 1)
                .findFirst()
                .orElseGet(() -> producto.getPresentaciones().stream()
                        .filter(p -> p.getActivo() == null || Boolean.TRUE.equals(p.getActivo()))
                        .findFirst()
                        .orElse(null));
    }
}
