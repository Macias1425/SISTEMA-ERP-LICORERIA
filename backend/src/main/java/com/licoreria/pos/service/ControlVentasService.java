package com.licoreria.pos.service;

import com.licoreria.pos.dto.ControlVentaEventoDTO;
import com.licoreria.pos.dto.ControlVentasCajeroDTO;
import com.licoreria.pos.dto.ControlVentasDiaDTO;
import com.licoreria.pos.dto.ControlVentasReglasDTO;
import com.licoreria.pos.dto.ControlVentasResumenDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.VentaResponseDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.Auditoria;
import com.licoreria.pos.model.ConfiguracionPos;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.TipoEventoControlVenta;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.repository.AuditoriaRepository;
import com.licoreria.pos.repository.ConfiguracionPosRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.repository.VentaRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ControlVentasService {

    private static final Long CONFIG_ID = 1L;
    private static final int MAX_DIAS_RANGO = 93;
    private static final int MAX_EVENTOS_RECIENTES = 8;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final ConfiguracionPosRepository configuracionRepository;
    private final VentaRepository ventaRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacionService;
    private final PermisoService permisoService;
    private final AuditoriaService auditoriaService;
    private final @Lazy VentaService ventaService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ControlVentasResumenDTO resumen(LocalDate desde, LocalDate hasta) {
        exigirVerControl();
        Rango rango = validarRango(desde, hasta);
        ControlVentasReglasDTO reglas = reglasInternas();
        List<Venta> ventas = ventasCompletadas(rango);
        List<Auditoria> anulacionesLog = auditoriaRepository.findByAccionAndFechaHoraBetweenOrderByFechaHoraDesc(
                AccionAuditoria.ANULACION_FACTURA, rango.inicio(), rango.fin());

        BigDecimal montoTotal = BigDecimal.ZERO;
        long altoMonto = 0;
        BigDecimal montoAlto = BigDecimal.ZERO;
        long overrides = 0;
        long supervisor = 0;
        Map<Long, CajeroAcumulador> cajeros = new HashMap<>();
        Map<LocalDate, DiaAcumulador> dias = new LinkedHashMap<>();

        long ventasConAlerta = 0;

        for (Venta venta : ventas) {
            BigDecimal total = venta.getTotal() == null ? BigDecimal.ZERO : venta.getTotal();
            montoTotal = montoTotal.add(total);

            boolean esAlto = esAltoMonto(venta, reglas);
            boolean esOverride = venta.getAutorizadoPrecioPor() != null;
            boolean esSupervisor = esSupervisorAutorizado(venta, reglas);
            boolean esAlerta = esAlto || esOverride || esSupervisor;

            if (esAlto) {
                altoMonto++;
                montoAlto = montoAlto.add(total);
            }
            if (esOverride) {
                overrides++;
            }
            if (esSupervisor) {
                supervisor++;
            }
            if (esAlerta) {
                ventasConAlerta++;
            }

            LocalDate dia = venta.getFecha().toLocalDate();
            DiaAcumulador diaAcum = dias.computeIfAbsent(dia, ignored -> new DiaAcumulador());
            diaAcum.ventas++;
            diaAcum.monto = diaAcum.monto.add(total);
            if (esAlto || esOverride || esSupervisor) {
                diaAcum.alertas++;
            }

            CajeroAcumulador cajero = cajeros.computeIfAbsent(
                    venta.getUsuarioId(),
                    id -> new CajeroAcumulador(id, nombreUsuario(id))
            );
            cajero.totalVentas++;
            cajero.montoTotal = cajero.montoTotal.add(total);
            if (esAlto || esOverride || esSupervisor) {
                cajero.alertas++;
            }
            if (esOverride) {
                cajero.overrides++;
            }
        }

        for (Auditoria anulacion : anulacionesLog) {
            if (anulacion.getUsuarioId() == null) {
                continue;
            }
            CajeroAcumulador cajero = cajeros.computeIfAbsent(
                    anulacion.getUsuarioId(),
                    id -> new CajeroAcumulador(id, nombreUsuario(id))
            );
            cajero.anulaciones++;
        }

        long alertasTotales = ventasConAlerta + anulacionesLog.size();
        BigDecimal ticketPromedio = ventas.isEmpty()
                ? BigDecimal.ZERO
                : montoTotal.divide(BigDecimal.valueOf(ventas.size()), 2, REDONDEO);
        BigDecimal tasaAlertas = ventas.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(ventasConAlerta)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(ventas.size()), 1, REDONDEO);

        List<ControlVentasDiaDTO> serieDiaria = dias.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> ControlVentasDiaDTO.builder()
                        .fecha(entry.getKey())
                        .ventas(entry.getValue().ventas)
                        .monto(entry.getValue().monto)
                        .alertas(entry.getValue().alertas)
                        .build())
                .toList();

        List<ControlVentasCajeroDTO> porCajero = cajeros.values().stream()
                .map(item -> ControlVentasCajeroDTO.builder()
                        .cajeroId(item.cajeroId)
                        .cajeroNombre(item.cajeroNombre)
                        .totalVentas(item.totalVentas)
                        .montoTotal(item.montoTotal)
                        .alertas(item.alertas + item.anulaciones)
                        .overrides(item.overrides)
                        .anulaciones(item.anulaciones)
                        .ticketPromedio(item.totalVentas == 0
                                ? BigDecimal.ZERO
                                : item.montoTotal.divide(BigDecimal.valueOf(item.totalVentas), 2, REDONDEO))
                        .build())
                .sorted(Comparator.comparingLong(ControlVentasCajeroDTO::getAlertas).reversed()
                        .thenComparing(ControlVentasCajeroDTO::getMontoTotal, Comparator.reverseOrder()))
                .toList();

        List<ControlVentaEventoDTO> eventosRecientes = construirEventos(
                rango, reglas, anulacionesLog, ventas, null, null
        ).stream().limit(MAX_EVENTOS_RECIENTES).toList();

        return ControlVentasResumenDTO.builder()
                .desde(rango.desde())
                .hasta(rango.hasta())
                .totalVentas(ventas.size())
                .montoTotal(montoTotal)
                .ticketPromedio(ticketPromedio)
                .ventasAltoMonto(altoMonto)
                .montoAltoMonto(montoAlto)
                .overridesPrecio(overrides)
                .ventasSupervisor(supervisor)
                .anulaciones(anulacionesLog.size())
                .alertasTotales(alertasTotales)
                .tasaAlertasPct(tasaAlertas)
                .serieDiaria(serieDiaria)
                .porCajero(porCajero)
                .eventosRecientes(eventosRecientes)
                .reglas(reglas)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ControlVentaEventoDTO> eventos(
            LocalDate desde,
            LocalDate hasta,
            TipoEventoControlVenta tipo,
            Long cajeroId,
            String busqueda
    ) {
        exigirVerControl();
        Rango rango = validarRango(desde, hasta);
        ControlVentasReglasDTO reglas = reglasInternas();
        List<Venta> ventas = ventasCompletadas(rango);
        List<Auditoria> anulacionesLog = auditoriaRepository.findByAccionAndFechaHoraBetweenOrderByFechaHoraDesc(
                AccionAuditoria.ANULACION_FACTURA, rango.inicio(), rango.fin());
        return construirEventos(rango, reglas, anulacionesLog, ventas, tipo, cajeroId).stream()
                .filter(evento -> coincideBusqueda(evento, busqueda))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ControlVentaEventoDTO> eventos(
            LocalDate desde,
            LocalDate hasta,
            TipoEventoControlVenta tipo,
            Long cajeroId,
            String busqueda,
            int pagina,
            int tamano
    ) {
        return PaginacionUtil.deLista(eventos(desde, hasta, tipo, cajeroId, busqueda), pagina, tamano);
    }

    @Transactional(readOnly = true)
    public VentaResponseDTO detalleVenta(Long ventaId) {
        exigirVerControl();
        return ventaService.obtenerPorId(ventaId);
    }

    @Transactional(readOnly = true)
    public ControlVentasReglasDTO reglas() {
        exigirConfigVentas();
        return reglasInternas();
    }

    @Transactional
    public ControlVentasReglasDTO guardarReglas(ControlVentasReglasDTO request) {
        Usuario operador = autorizacionService.operadorActual();
        exigirEditarReglasVentas(operador);
        validarReglas(request);
        ConfiguracionPos config = asegurarConfig();
        String antes = resumenReglas(config);

        config.setMontoAlertaVenta(request.getMontoAlertaVenta().setScale(2, REDONDEO));
        config.setMontoSupervisorRequerido(request.getMontoSupervisorRequerido().setScale(2, REDONDEO));
        config.setMaxVentasPorTurno(request.getMaxVentasPorTurno());
        config.setAlertarOverridePrecio(Boolean.TRUE.equals(request.getAlertarOverridePrecio()));
        config.setActualizadoEn(LocalDateTime.now(clock));
        configuracionRepository.save(config);

        auditoriaService.registrar(operador, AccionAuditoria.CONFIGURACION, "ControlVentas", CONFIG_ID,
                antes, resumenReglas(config), "Actualización de reglas de control de ventas");
        return toReglas(config);
    }

    @Transactional(readOnly = true)
    public ControlVentasReglasDTO reglasOperativas() {
        return reglasInternas();
    }

    @Transactional(readOnly = true)
    public void validarLimiteTurno(Long turnoId) {
        ControlVentasReglasDTO reglas = reglasInternas();
        int max = reglas.getMaxVentasPorTurno() == null ? 0 : reglas.getMaxVentasPorTurno();
        if (max <= 0 || turnoId == null) {
            return;
        }
        long actuales = ventaRepository.countByTurnoCajaIdAndEstado(turnoId, EstadoVenta.COMPLETADA);
        if (actuales >= max) {
            throw new ReglaNegocioException(
                    "LIMITE_VENTAS_TURNO",
                    "Se alcanzó el límite de " + max + " ventas por turno. Cierre caja o contacte al administrador"
            );
        }
    }

    private List<ControlVentaEventoDTO> construirEventos(
            Rango rango,
            ControlVentasReglasDTO reglas,
            List<Auditoria> anulacionesLog,
            List<Venta> ventas,
            TipoEventoControlVenta tipo,
            Long cajeroId
    ) {
        List<ControlVentaEventoDTO> eventos = new ArrayList<>();

        if (tipo == null || tipo == TipoEventoControlVenta.ALTO_MONTO) {
            ventas.stream()
                    .filter(v -> esAltoMonto(v, reglas))
                    .map(v -> eventoVenta(TipoEventoControlVenta.ALTO_MONTO, v,
                            "Venta " + v.getNumero() + " supera umbral de "
                                    + reglas.getMontoAlertaVenta().toPlainString(),
                            "MEDIO"))
                    .forEach(eventos::add);
        }

        if (tipo == null || tipo == TipoEventoControlVenta.OVERRIDE_PRECIO) {
            ventas.stream()
                    .filter(v -> v.getAutorizadoPrecioPor() != null)
                    .filter(v -> Boolean.TRUE.equals(reglas.getAlertarOverridePrecio()))
                    .map(v -> eventoVenta(TipoEventoControlVenta.OVERRIDE_PRECIO, v,
                            v.getObservacion() != null ? v.getObservacion() : "Precio autorizado por supervisor",
                            "ALTO"))
                    .forEach(eventos::add);
        }

        if (tipo == null || tipo == TipoEventoControlVenta.SUPERVISOR_AUTORIZADO) {
            ventas.stream()
                    .filter(v -> esSupervisorAutorizado(v, reglas))
                    .map(v -> eventoVenta(TipoEventoControlVenta.SUPERVISOR_AUTORIZADO, v,
                            v.getObservacion() != null && !v.getObservacion().isBlank()
                                    ? v.getObservacion()
                                    : "Venta requiere autorización de supervisor",
                            "ALTO"))
                    .forEach(eventos::add);
        }

        if (tipo == null || tipo == TipoEventoControlVenta.ANULACION) {
            anulacionesLog.stream()
                    .map(this::eventoAnulacion)
                    .forEach(eventos::add);
        }

        return eventos.stream()
                .filter(evento -> cajeroId == null || cajeroId.equals(evento.getCajeroId()))
                .sorted(Comparator.comparing(ControlVentaEventoDTO::getFecha).reversed())
                .toList();
    }

    private List<Venta> ventasCompletadas(Rango rango) {
        return ventaRepository.findByFechaBetween(rango.inicio(), rango.fin()).stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .toList();
    }

    private boolean coincideBusqueda(ControlVentaEventoDTO evento, String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return true;
        }
        String termino = busqueda.trim().toLowerCase(Locale.ROOT);
        return contiene(evento.getReferencia(), termino)
                || contiene(evento.getCajeroNombre(), termino)
                || contiene(evento.getDetalle(), termino)
                || contiene(evento.getTipo() == null ? null : evento.getTipo().name(), termino);
    }

    private boolean contiene(String valor, String termino) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(termino);
    }

    private ControlVentasReglasDTO reglasInternas() {
        return toReglas(asegurarConfig());
    }

    private ConfiguracionPos asegurarConfig() {
        ConfiguracionPos config = configuracionRepository.findById(CONFIG_ID).orElseGet(() ->
                configuracionRepository.save(ConfiguracionPos.builder().id(CONFIG_ID).build()));
        boolean cambio = false;
        if (config.getMontoAlertaVenta() == null) {
            config.setMontoAlertaVenta(new BigDecimal("5000.00"));
            cambio = true;
        }
        if (config.getMontoSupervisorRequerido() == null) {
            config.setMontoSupervisorRequerido(new BigDecimal("15000.00"));
            cambio = true;
        }
        if (config.getMaxVentasPorTurno() == null) {
            config.setMaxVentasPorTurno(0);
            cambio = true;
        }
        if (config.getAlertarOverridePrecio() == null) {
            config.setAlertarOverridePrecio(true);
            cambio = true;
        }
        return cambio ? configuracionRepository.save(config) : config;
    }

    private void validarReglas(ControlVentasReglasDTO request) {
        if (request.getMontoAlertaVenta() == null || request.getMontoSupervisorRequerido() == null
                || request.getMaxVentasPorTurno() == null || request.getAlertarOverridePrecio() == null) {
            throw new ReglaNegocioException("REGLAS_INCOMPLETAS", "Debe completar todas las reglas de control");
        }
        if (request.getMontoSupervisorRequerido().compareTo(request.getMontoAlertaVenta()) < 0) {
            throw new ReglaNegocioException(
                    "REGLAS_INCONSISTENTES",
                    "El monto que exige supervisor debe ser mayor o igual al monto de alerta"
            );
        }
    }

    private boolean esAltoMonto(Venta venta, ControlVentasReglasDTO reglas) {
        BigDecimal umbral = reglas.getMontoAlertaVenta();
        return venta.getTotal() != null && umbral != null && venta.getTotal().compareTo(umbral) >= 0;
    }

    private boolean esSupervisorAutorizado(Venta venta, ControlVentasReglasDTO reglas) {
        if (venta.getObservacion() != null
                && venta.getObservacion().toLowerCase(Locale.ROOT).contains("supervisor")) {
            return true;
        }
        BigDecimal umbral = reglas.getMontoSupervisorRequerido();
        return venta.getTotal() != null && umbral != null && venta.getTotal().compareTo(umbral) >= 0;
    }

    private ControlVentaEventoDTO eventoVenta(TipoEventoControlVenta tipo, Venta venta, String detalle, String nivel) {
        return ControlVentaEventoDTO.builder()
                .tipo(tipo)
                .fecha(venta.getFecha())
                .referencia(venta.getNumero())
                .entidadId(venta.getId())
                .cajeroId(venta.getUsuarioId())
                .cajeroNombre(nombreUsuario(venta.getUsuarioId()))
                .monto(venta.getTotal())
                .detalle(detalle)
                .nivel(nivel)
                .build();
    }

    private ControlVentaEventoDTO eventoAnulacion(Auditoria auditoria) {
        return ControlVentaEventoDTO.builder()
                .tipo(TipoEventoControlVenta.ANULACION)
                .fecha(auditoria.getFechaHora())
                .referencia(auditoria.getEntidad() + " #" + auditoria.getEntidadId())
                .entidadId(auditoria.getEntidadId())
                .cajeroId(auditoria.getUsuarioId())
                .cajeroNombre(nombreUsuario(auditoria.getUsuarioId()))
                .monto(parseMonto(auditoria.getValorNuevo()))
                .detalle(auditoria.getDetalle())
                .nivel("ALTO")
                .build();
    }

    private BigDecimal parseMonto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String nombreUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return usuarioRepository.findById(usuarioId).map(Usuario::getNombreCompleto).orElse(null);
    }

    private ControlVentasReglasDTO toReglas(ConfiguracionPos config) {
        return ControlVentasReglasDTO.builder()
                .montoAlertaVenta(config.getMontoAlertaVenta())
                .montoSupervisorRequerido(config.getMontoSupervisorRequerido())
                .maxVentasPorTurno(config.getMaxVentasPorTurno())
                .alertarOverridePrecio(config.getAlertarOverridePrecio())
                .actualizadoEn(config.getActualizadoEn())
                .build();
    }

    private String resumenReglas(ConfiguracionPos config) {
        return "alerta=" + config.getMontoAlertaVenta()
                + ", supervisor=" + config.getMontoSupervisorRequerido()
                + ", maxTurno=" + config.getMaxVentasPorTurno()
                + ", overrideAlert=" + config.getAlertarOverridePrecio();
    }

    private void exigirVerControl() {
        permisoService.exigirPermiso(autorizacionService.operadorActual(), Permiso.CONTROL_VENTAS_VER);
    }

    private void exigirConfigVentas() {
        Usuario operador = autorizacionService.operadorActual();
        if (permisoService.tienePermiso(operador, Permiso.CONFIG_GESTIONAR)
                || permisoService.tienePermiso(operador, Permiso.CONTROL_VENTAS_VER)) {
            return;
        }
        permisoService.exigirPermiso(operador, Permiso.CONTROL_VENTAS_VER);
    }

    private void exigirEditarReglasVentas(Usuario operador) {
        if (permisoService.tienePermiso(operador, Permiso.CONFIG_GESTIONAR)
                || permisoService.tienePermiso(operador, Permiso.CONTROL_VENTAS_CONFIG)) {
            return;
        }
        permisoService.exigirPermiso(operador, Permiso.CONTROL_VENTAS_CONFIG);
    }

    private Rango validarRango(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now(clock) : hasta;
        LocalDate ini = desde == null ? fin.minusDays(6) : desde;
        if (ini.isAfter(fin)) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "La fecha inicial no puede ser posterior a la final");
        }
        if (ChronoUnit.DAYS.between(ini, fin) > MAX_DIAS_RANGO) {
            throw new ReglaNegocioException("RANGO_LARGO", "El rango máximo es de " + MAX_DIAS_RANGO + " días");
        }
        return new Rango(ini, fin, ini.atStartOfDay(), fin.atTime(LocalTime.MAX));
    }

    private static final class CajeroAcumulador {
        private final Long cajeroId;
        private final String cajeroNombre;
        private long totalVentas;
        private BigDecimal montoTotal = BigDecimal.ZERO;
        private long alertas;
        private long overrides;
        private long anulaciones;

        private CajeroAcumulador(Long cajeroId, String cajeroNombre) {
            this.cajeroId = cajeroId;
            this.cajeroNombre = cajeroNombre;
        }
    }

    private static final class DiaAcumulador {
        private long ventas;
        private BigDecimal monto = BigDecimal.ZERO;
        private long alertas;
    }

    private record Rango(LocalDate desde, LocalDate hasta, LocalDateTime inicio, LocalDateTime fin) {
    }
}
