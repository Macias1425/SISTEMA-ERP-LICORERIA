package com.licoreria.pos.service;

import com.licoreria.pos.dto.AlertaSeccionDTO;
import com.licoreria.pos.dto.AlertasResumenDTO;
import com.licoreria.pos.dto.EstadoFiscalDTO;
import com.licoreria.pos.model.EstadoMerma;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.NivelAlerta;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.LoteInventarioRepository;
import com.licoreria.pos.repository.MermaRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Centro único de alertas operativas. Solo entrega las secciones que el usuario
 * puede ver, para que el badge del menú no filtre información de otros módulos.
 */
@Service
@RequiredArgsConstructor
public class AlertasService {

    private static final String CRITICA = "CRITICA";
    private static final String AVISO = "AVISO";

    private final ProductoRepository productoRepository;
    private final LoteInventarioRepository loteRepository;
    private final MermaRepository mermaRepository;
    private final VentaRepository ventaRepository;
    private final InventarioService inventarioService;
    private final NumeracionFiscalService numeracionFiscalService;
    private final ControlVentasService controlVentasService;
    private final AutorizacionService autorizacionService;
    private final AccesoService accesoService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AlertasResumenDTO resumen() {
        Usuario operador = autorizacionService.operadorActual();
        List<AlertaSeccionDTO> secciones = new ArrayList<>();

        if (accesoService.tienePermiso(operador, Permiso.INVENTARIO_VER)) {
            agregar(secciones, stockCritico());
            agregar(secciones, lotesPorVencer());
        }
        if (accesoService.tienePermiso(operador, Permiso.MERMA_APROBAR)) {
            agregar(secciones, mermasPendientes());
        }
        if (accesoService.tienePermiso(operador, Permiso.CONTROL_VENTAS_VER)) {
            agregar(secciones, ventasConAlertaHoy());
        }
        if (accesoService.tienePermiso(operador, Permiso.CONFIG_GESTIONAR)) {
            agregar(secciones, rangoFiscal());
        }

        int total = secciones.stream().mapToInt(AlertaSeccionDTO::getCantidad).sum();
        int criticas = secciones.stream()
                .filter(seccion -> CRITICA.equals(seccion.getNivel()))
                .mapToInt(AlertaSeccionDTO::getCantidad)
                .sum();

        return AlertasResumenDTO.builder()
                .total(total)
                .criticas(criticas)
                .generadoEn(LocalDateTime.now(clock))
                .secciones(secciones)
                .build();
    }

    private AlertaSeccionDTO stockCritico() {
        List<Producto> activos = productoRepository.findByActivoTrue();
        long criticos = activos.stream()
                .filter(producto -> inventarioService.calcularNivelAlerta(producto) == NivelAlerta.CRITICO)
                .count();
        long minimos = activos.stream()
                .filter(producto -> inventarioService.calcularNivelAlerta(producto) == NivelAlerta.MINIMO)
                .count();
        if (criticos + minimos == 0) {
            return null;
        }
        return AlertaSeccionDTO.builder()
                .clave("stock")
                .etiqueta("Stock por reponer")
                .ruta("/inventario")
                .cantidad((int) (criticos + minimos))
                .nivel(criticos > 0 ? CRITICA : AVISO)
                .detalle(criticos + " en crítico y " + minimos + " en mínimo")
                .build();
    }

    private AlertaSeccionDTO lotesPorVencer() {
        LocalDate hoy = LocalDate.now(clock);
        var lotes = loteRepository.porVencer(hoy.plusDays(LoteService.DIAS_POR_VENCER));
        if (lotes.isEmpty()) {
            return null;
        }
        long vencidos = lotes.stream()
                .filter(lote -> lote.getFechaVencimiento().isBefore(hoy))
                .count();
        return AlertaSeccionDTO.builder()
                .clave("lotes")
                .etiqueta("Lotes por vencer")
                .ruta("/inventario")
                .cantidad(lotes.size())
                .nivel(vencidos > 0 ? CRITICA : AVISO)
                .detalle(vencidos > 0
                        ? vencidos + " ya vencidos en bodega"
                        : "Vencen en los próximos " + LoteService.DIAS_POR_VENCER + " días")
                .build();
    }

    private AlertaSeccionDTO mermasPendientes() {
        int pendientes = mermaRepository.findByEstadoOrderByFechaSolicitudAsc(EstadoMerma.PENDIENTE).size();
        if (pendientes == 0) {
            return null;
        }
        return AlertaSeccionDTO.builder()
                .clave("mermas")
                .etiqueta("Mermas por aprobar")
                .ruta("/inventario")
                .cantidad(pendientes)
                .nivel(AVISO)
                .detalle("Esperan autorización de un administrador")
                .build();
    }

    private AlertaSeccionDTO ventasConAlertaHoy() {
        LocalDate hoy = LocalDate.now(clock);
        var reglas = controlVentasService.reglasOperativas();
        var ventas = ventaRepository.findByFechaBetween(hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX)).stream()
                .filter(venta -> venta.getEstado() == EstadoVenta.COMPLETADA)
                .toList();
        long overrides = ventas.stream().filter(venta -> venta.getAutorizadoPrecioPor() != null).count();
        long altoMonto = reglas.getMontoAlertaVenta() == null
                ? 0
                : ventas.stream()
                .filter(venta -> venta.getTotal() != null
                        && venta.getTotal().compareTo(reglas.getMontoAlertaVenta()) >= 0)
                .count();
        long total = overrides + altoMonto;
        if (total == 0) {
            return null;
        }
        return AlertaSeccionDTO.builder()
                .clave("control-ventas")
                .etiqueta("Ventas a revisar hoy")
                .ruta("/control-ventas")
                .cantidad((int) total)
                .nivel(overrides > 0 ? CRITICA : AVISO)
                .detalle(overrides + " con cambio de precio y " + altoMonto + " de alto monto")
                .build();
    }

    private AlertaSeccionDTO rangoFiscal() {
        EstadoFiscalDTO fiscal = numeracionFiscalService.estado();
        if (!Boolean.TRUE.equals(fiscal.getHabilitada()) || "OK".equals(fiscal.getEstado())) {
            return null;
        }
        boolean bloqueante = List.of("AGOTADO", "VENCIDO", "NO_CONFIGURADO").contains(fiscal.getEstado());
        return AlertaSeccionDTO.builder()
                .clave("fiscal")
                .etiqueta("Rango fiscal")
                .ruta("/configuracion")
                .cantidad(1)
                .nivel(bloqueante ? CRITICA : AVISO)
                .detalle(fiscal.getMensaje())
                .build();
    }

    private void agregar(List<AlertaSeccionDTO> destino, AlertaSeccionDTO seccion) {
        if (seccion != null) {
            destino.add(seccion);
        }
    }
}
