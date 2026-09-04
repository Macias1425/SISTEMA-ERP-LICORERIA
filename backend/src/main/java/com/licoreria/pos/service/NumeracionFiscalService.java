package com.licoreria.pos.service;

import com.licoreria.pos.dto.EstadoFiscalDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.ConfiguracionPos;
import com.licoreria.pos.repository.ConfiguracionPosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Asigna correlativos dentro del rango autorizado por la DGI y vigila su agotamiento.
 * Si la facturación fiscal está desactivada el POS sigue operando con su numeración interna.
 */
@Service
@RequiredArgsConstructor
public class NumeracionFiscalService {

    private static final Long CONFIG_ID = 1L;
    /** A partir de este consumo del rango se avisa al administrador. */
    private static final int UMBRAL_AVISO_PORCENTAJE = 85;
    private static final int UMBRAL_AVISO_DIAS = 15;

    private final ConfiguracionPosRepository configuracionRepository;
    private final Clock clock;

    /** Documento fiscal asignado a una factura. */
    public record DocumentoFiscal(String numeroFiscal, String autorizacionDgi, String rangoAutorizado,
                                  LocalDate fechaLimite) {
    }

    @Transactional(readOnly = true)
    public boolean habilitada() {
        return Boolean.TRUE.equals(config().getFacturacionFiscalHabilitada());
    }

    /**
     * Reserva el siguiente correlativo. Devuelve {@code null} si el régimen fiscal está
     * desactivado, para no bloquear la operación diaria del negocio.
     */
    @Transactional
    public DocumentoFiscal asignarSiguiente() {
        ConfiguracionPos config = config();
        if (!Boolean.TRUE.equals(config.getFacturacionFiscalHabilitada())) {
            return null;
        }
        validarVigencia(config);

        long siguiente = correlativoActual(config) + 1;
        if (siguiente > rangoFinal(config)) {
            throw new ReglaNegocioException("RANGO_FISCAL_AGOTADO",
                    "Se agotó el rango autorizado por la DGI. Solicite un nuevo rango antes de seguir facturando");
        }
        config.setCorrelativoActual(siguiente);
        configuracionRepository.save(config);

        return new DocumentoFiscal(
                formatearNumero(config, siguiente),
                config.getAutorizacionDgi(),
                rangoTexto(config),
                config.getFechaLimiteEmision()
        );
    }

    @Transactional(readOnly = true)
    public EstadoFiscalDTO estado() {
        ConfiguracionPos config = config();
        boolean habilitada = Boolean.TRUE.equals(config.getFacturacionFiscalHabilitada());
        long inicial = rangoInicial(config);
        long finalRango = rangoFinal(config);
        long actual = correlativoActual(config);
        long total = Math.max(finalRango - inicial + 1, 1);
        long emitidos = Math.max(actual - inicial + 1, 0);
        long disponibles = Math.max(finalRango - actual, 0);
        int consumido = (int) Math.min(100, Math.round(emitidos * 100.0 / total));
        Integer dias = config.getFechaLimiteEmision() == null
                ? null
                : (int) ChronoUnit.DAYS.between(LocalDate.now(clock), config.getFechaLimiteEmision());

        String estado = clasificar(config, habilitada, disponibles, consumido, dias);
        return EstadoFiscalDTO.builder()
                .habilitada(habilitada)
                .autorizacionDgi(config.getAutorizacionDgi())
                .rucEmisor(config.getRucEmisor())
                .rangoAutorizado(rangoTexto(config))
                .rangoInicial(inicial)
                .rangoFinal(finalRango)
                .correlativoActual(actual)
                .proximoNumero(actual + 1 > finalRango ? null : formatearNumero(config, actual + 1))
                .documentosDisponibles(disponibles)
                .porcentajeConsumido(consumido)
                .fechaLimiteEmision(config.getFechaLimiteEmision())
                .diasParaVencer(dias)
                .estado(estado)
                .mensaje(mensaje(estado, disponibles, dias))
                .build();
    }

    /** Validaciones de los datos fiscales antes de guardarlos en configuración. */
    public void validarConfiguracion(boolean habilitada, String autorizacion, String ruc, Long inicial, Long finalRango,
                                     Long correlativo, LocalDate fechaLimite) {
        if (!habilitada) {
            return;
        }
        if (autorizacion == null || autorizacion.isBlank()) {
            throw new ReglaNegocioException("AUTORIZACION_FISCAL_REQUERIDA",
                    "Debe registrar el número de autorización de la DGI");
        }
        if (ruc == null || ruc.isBlank()) {
            throw new ReglaNegocioException("RUC_REQUERIDO", "Debe registrar el RUC del emisor");
        }
        if (inicial == null || finalRango == null || inicial < 1 || finalRango < inicial) {
            throw new ReglaNegocioException("RANGO_FISCAL_INVALIDO",
                    "El rango autorizado debe ir de menor a mayor y empezar en 1 o más");
        }
        long actual = correlativo == null ? 0L : correlativo;
        if (actual != 0 && (actual < inicial - 1 || actual > finalRango)) {
            throw new ReglaNegocioException("CORRELATIVO_FISCAL_INVALIDO",
                    "El correlativo actual debe estar dentro del rango autorizado");
        }
        if (fechaLimite == null) {
            throw new ReglaNegocioException("FECHA_LIMITE_REQUERIDA",
                    "Debe registrar la fecha límite de emisión de la autorización");
        }
    }

    private void validarVigencia(ConfiguracionPos config) {
        if (config.getAutorizacionDgi() == null || config.getAutorizacionDgi().isBlank()) {
            throw new ReglaNegocioException("AUTORIZACION_FISCAL_NO_CONFIGURADA",
                    "La facturación fiscal está activa pero no hay autorización de la DGI registrada");
        }
        if (config.getFechaLimiteEmision() != null
                && config.getFechaLimiteEmision().isBefore(LocalDate.now(clock))) {
            throw new ReglaNegocioException("AUTORIZACION_FISCAL_VENCIDA",
                    "La autorización venció el " + config.getFechaLimiteEmision()
                            + ". No se puede emitir con este rango");
        }
    }

    private String clasificar(ConfiguracionPos config, boolean habilitada, long disponibles, int consumido,
                              Integer dias) {
        if (!habilitada) {
            return "DESACTIVADA";
        }
        if (config.getAutorizacionDgi() == null || config.getAutorizacionDgi().isBlank()) {
            return "NO_CONFIGURADO";
        }
        if (dias != null && dias < 0) {
            return "VENCIDO";
        }
        if (disponibles <= 0) {
            return "AGOTADO";
        }
        if (consumido >= UMBRAL_AVISO_PORCENTAJE || (dias != null && dias <= UMBRAL_AVISO_DIAS)) {
            return "POR_AGOTARSE";
        }
        return "OK";
    }

    private String mensaje(String estado, long disponibles, Integer dias) {
        return switch (estado) {
            case "DESACTIVADA" -> "Facturación fiscal desactivada: se usa numeración interna del POS";
            case "NO_CONFIGURADO" -> "Falta registrar la autorización de la DGI y el rango autorizado";
            case "VENCIDO" -> "La autorización está vencida: solicite un nuevo rango";
            case "AGOTADO" -> "Rango agotado: no se pueden emitir más documentos";
            case "POR_AGOTARSE" -> "Quedan " + disponibles + " documentos"
                    + (dias == null ? "" : " y " + dias + " días de vigencia");
            default -> "Rango vigente con " + disponibles + " documentos disponibles";
        };
    }

    private String formatearNumero(ConfiguracionPos config, long correlativo) {
        return String.join("-",
                normalizar(config.getEstablecimiento(), 3, "000"),
                normalizar(config.getPuntoEmision(), 3, "001"),
                normalizar(config.getTipoDocumentoFiscal(), 2, "01"),
                String.format("%08d", correlativo));
    }

    private String rangoTexto(ConfiguracionPos config) {
        return formatearNumero(config, rangoInicial(config)) + " a " + formatearNumero(config, rangoFinal(config));
    }

    private String normalizar(String valor, int longitud, String porDefecto) {
        String base = valor == null || valor.isBlank() ? porDefecto : valor.trim();
        if (base.length() >= longitud) {
            return base.substring(0, longitud);
        }
        return "0".repeat(longitud - base.length()) + base;
    }

    private long rangoInicial(ConfiguracionPos config) {
        return config.getRangoInicial() == null ? 1L : config.getRangoInicial();
    }

    private long rangoFinal(ConfiguracionPos config) {
        return config.getRangoFinal() == null ? 0L : config.getRangoFinal();
    }

    private long correlativoActual(ConfiguracionPos config) {
        long actual = config.getCorrelativoActual() == null ? 0L : config.getCorrelativoActual();
        return actual == 0 ? rangoInicial(config) - 1 : actual;
    }

    private ConfiguracionPos config() {
        return configuracionRepository.findById(CONFIG_ID)
                .orElseGet(() -> configuracionRepository.save(ConfiguracionPos.builder().id(CONFIG_ID).build()));
    }
}
