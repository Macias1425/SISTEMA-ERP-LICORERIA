package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.dto.ConfiguracionDTO;
import com.licoreria.pos.dto.DatosNegocioDTO;
import com.licoreria.pos.dto.HorarioVentaDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.ConfiguracionPos;
import com.licoreria.pos.model.HorarioVentaLicor;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.ConfiguracionPosRepository;
import com.licoreria.pos.repository.HorarioVentaLicorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private static final Long CONFIG_ID = 1L;
    private static final int MINUTOS_MINIMOS_HORARIO = 60;

    private final ConfiguracionPosRepository configuracionRepository;
    private final HorarioVentaLicorRepository horarioRepository;
    private final PosProperties posProperties;
    private final AccesoService accesoService;
    private final NumeracionFiscalService numeracionFiscalService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ConfiguracionDTO obtener() {
        accesoService.exigirPermiso(Permiso.CONFIG_GESTIONAR);
        return toDto(asegurarParametros(), asegurarHorarios());
    }

    /**
     * Encabezado del negocio para tickets y facturas. No exige CONFIG_GESTIONAR: el cajero
     * necesita estos datos para imprimir, y sin ellos el comprobante sale sin identificación.
     */
    @Transactional(readOnly = true)
    public DatosNegocioDTO datosNegocio() {
        ConfiguracionPos config = asegurarParametros();
        return DatosNegocioDTO.builder()
                .nombreNegocio(config.getNombreNegocio())
                .direccionNegocio(config.getDireccionNegocio())
                .telefonoNegocio(config.getTelefonoNegocio())
                .rucEmisor(config.getRucEmisor())
                .autorizacionDgi(config.getAutorizacionDgi())
                .tasaIva(config.getTasaIva())
                .edadMinimaAlcohol(config.getEdadMinimaAlcohol())
                .facturacionFiscalHabilitada(config.getFacturacionFiscalHabilitada())
                .build();
    }

    @Transactional
    public ConfiguracionDTO guardar(ConfiguracionDTO request) {
        Usuario admin = accesoService.exigirPermiso(Permiso.CONFIG_GESTIONAR);
        validarParametros(request);
        validarHorarios(request.getHorarios(), Boolean.TRUE.equals(request.getHorarioHabilitado()));

        ConfiguracionPos actual = asegurarParametros();
        String antes = resumen(actual, asegurarHorarios());

        actual.setNombreNegocio(request.getNombreNegocio().trim());
        actual.setDireccionNegocio(texto(request.getDireccionNegocio()));
        actual.setTelefonoNegocio(texto(request.getTelefonoNegocio()));
        actual.setEdadMinimaAlcohol(request.getEdadMinimaAlcohol());
        actual.setHorarioHabilitado(Boolean.TRUE.equals(request.getHorarioHabilitado()));
        actual.setTasaIva(request.getTasaIva().setScale(4, RoundingMode.HALF_UP));
        actual.setVolumenMinimoUmm(request.getVolumenMinimoUmm());
        actual.setRequiereTurnoAbiertoParaAnular(Boolean.TRUE.equals(request.getRequiereTurnoAbiertoParaAnular()));
        actual.setMaxIntentosLogin(request.getMaxIntentosLogin());
        actual.setBloqueoMinutos(request.getBloqueoMinutos());
        aplicarDatosFiscales(actual, request);
        actual.setActualizadoEn(clock.instant().atZone(clock.getZone()).toLocalDateTime());
        configuracionRepository.save(actual);
        aplicar(actual);

        List<HorarioVentaLicor> guardados = new ArrayList<>();
        for (HorarioVentaDTO linea : request.getHorarios()) {
            HorarioVentaLicor horario = horarioRepository.findByDiaSemana(linea.getDiaSemana())
                    .orElseGet(() -> HorarioVentaLicor.builder().diaSemana(linea.getDiaSemana()).build());
            horario.setHoraInicio(linea.getHoraInicio());
            horario.setHoraFin(linea.getHoraFin());
            horario.setActivo(Boolean.TRUE.equals(linea.getActivo()));
            guardados.add(horarioRepository.save(horario));
        }

        ConfiguracionDTO dto = toDto(actual, guardados);
        auditoriaService.registrar(admin, AccionAuditoria.CONFIGURACION, "Configuracion", CONFIG_ID,
                antes, resumen(actual, guardados), "Actualización de parámetros globales");
        return dto;
    }

    public void sincronizarAlArranque() {
        aplicar(asegurarParametros());
        asegurarHorarios();
    }

    private void validarParametros(ConfiguracionDTO request) {
        if (request.getNombreNegocio() == null || request.getNombreNegocio().trim().length() < 3) {
            throw new ReglaNegocioException("NOMBRE_NEGOCIO_INVALIDO", "El nombre del negocio debe tener al menos 3 caracteres");
        }
        if (request.getEdadMinimaAlcohol() == null || request.getEdadMinimaAlcohol() < 18) {
            throw new ReglaNegocioException("EDAD_MINIMA_INVALIDA", "La edad mínima para alcohol no puede ser menor a 18 años");
        }
        if (request.getTasaIva() == null
                || request.getTasaIva().compareTo(BigDecimal.ZERO) < 0
                || request.getTasaIva().compareTo(BigDecimal.ONE) > 0) {
            throw new ReglaNegocioException("IVA_INVALIDO", "El IVA debe estar entre 0% y 100%");
        }
        if (request.getVolumenMinimoUmm() == null || request.getVolumenMinimoUmm() < 1) {
            throw new ReglaNegocioException("VOLUMEN_MAYORISTA_INVALIDO", "El volumen mayorista debe ser al menos 1 botella");
        }
    }

    /** El régimen fiscal solo se toca si el request lo trae; así el módulo es opcional. */
    private void aplicarDatosFiscales(ConfiguracionPos actual, ConfiguracionDTO request) {
        if (request.getFacturacionFiscalHabilitada() == null) {
            return;
        }
        boolean habilitada = Boolean.TRUE.equals(request.getFacturacionFiscalHabilitada());
        numeracionFiscalService.validarConfiguracion(
                habilitada,
                request.getAutorizacionDgi(),
                request.getRucEmisor(),
                request.getRangoInicial(),
                request.getRangoFinal(),
                request.getCorrelativoActual(),
                request.getFechaLimiteEmision()
        );
        actual.setFacturacionFiscalHabilitada(habilitada);
        actual.setAutorizacionDgi(texto(request.getAutorizacionDgi()));
        actual.setRucEmisor(texto(request.getRucEmisor()));
        actual.setEstablecimiento(texto(request.getEstablecimiento()));
        actual.setPuntoEmision(texto(request.getPuntoEmision()));
        actual.setTipoDocumentoFiscal(texto(request.getTipoDocumentoFiscal()));
        actual.setRangoInicial(request.getRangoInicial());
        actual.setRangoFinal(request.getRangoFinal());
        actual.setCorrelativoActual(request.getCorrelativoActual() == null ? 0L : request.getCorrelativoActual());
        actual.setFechaLimiteEmision(request.getFechaLimiteEmision());
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void validarHorarios(List<HorarioVentaDTO> horarios, boolean horarioHabilitado) {
        if (horarios == null || horarios.isEmpty()) {
            throw new ReglaNegocioException("HORARIO_INCOMPLETO", "Debe configurar los 7 días de la semana");
        }
        Set<DayOfWeek> vistos = EnumSet.noneOf(DayOfWeek.class);
        int activos = 0;
        for (HorarioVentaDTO horario : horarios) {
            if (horario.getDiaSemana() == null) {
                throw new ReglaNegocioException("HORARIO_INVALIDO", "Cada horario debe indicar el día de la semana");
            }
            if (!vistos.add(horario.getDiaSemana())) {
                throw new ReglaNegocioException("HORARIO_DUPLICADO", "Hay más de un horario para " + horario.getDiaSemana());
            }
            if (horario.getHoraInicio() == null || horario.getHoraFin() == null) {
                throw new ReglaNegocioException("HORARIO_INVALIDO", "Debe indicar hora de inicio y fin para " + horario.getDiaSemana());
            }
            if (!horario.getHoraInicio().isBefore(horario.getHoraFin())) {
                throw new ReglaNegocioException("HORARIO_INVALIDO",
                        "La hora de inicio debe ser anterior a la de fin (" + horario.getDiaSemana() + ")");
            }
            if (Boolean.TRUE.equals(horario.getActivo())) {
                activos += 1;
                long minutos = Duration.between(horario.getHoraInicio(), horario.getHoraFin()).toMinutes();
                if (minutos < MINUTOS_MINIMOS_HORARIO) {
                    throw new ReglaNegocioException(
                            "HORARIO_CORTO",
                            "El horario de " + horario.getDiaSemana() + " debe cubrir al menos " + MINUTOS_MINIMOS_HORARIO + " minutos"
                    );
                }
            }
        }
        if (vistos.size() != DayOfWeek.values().length) {
            throw new ReglaNegocioException("HORARIO_INCOMPLETO", "Debe configurar los 7 días de la semana");
        }
        if (horarioHabilitado && activos == 0) {
            throw new ReglaNegocioException(
                    "HORARIO_SIN_DIAS",
                    "Con el horario de licor activo debe haber al menos un día habilitado para vender alcohol"
            );
        }
    }

    private ConfiguracionPos asegurarParametros() {
        ConfiguracionPos config = configuracionRepository.findById(CONFIG_ID).orElseGet(() -> {
            ConfiguracionPos inicial = ConfiguracionPos.builder()
                    .id(CONFIG_ID)
                    .nombreNegocio("Licorería POS")
                    .direccionNegocio("Managua, Nicaragua")
                    .telefonoNegocio("2222-0101")
                    .edadMinimaAlcohol(posProperties.getNormativa().getEdadMinimaAlcohol())
                    .horarioHabilitado(posProperties.getNormativa().isHorarioHabilitado())
                    .tasaIva(posProperties.getImpuesto().getTasaIva())
                    .volumenMinimoUmm(posProperties.getMayorista().getVolumenMinimoUmm())
                    .requiereTurnoAbiertoParaAnular(posProperties.getFactura().isRequiereTurnoAbiertoParaAnular())
                    .maxIntentosLogin(posProperties.getJwt().getMaxIntentosLogin())
                    .bloqueoMinutos((int) posProperties.getJwt().getBloqueoMinutos())
                    .build();
            return configuracionRepository.save(inicial);
        });
        boolean cambio = false;
        if (config.getNombreNegocio() == null || config.getNombreNegocio().isBlank()) {
            config.setNombreNegocio("Licorería POS");
            cambio = true;
        }
        if (config.getDireccionNegocio() == null || config.getDireccionNegocio().isBlank()) {
            config.setDireccionNegocio("Managua, Nicaragua");
            cambio = true;
        }
        if (config.getTelefonoNegocio() == null || config.getTelefonoNegocio().isBlank()) {
            config.setTelefonoNegocio("2222-0101");
            cambio = true;
        }
        if (config.getRequiereTurnoAbiertoParaAnular() == null) {
            config.setRequiereTurnoAbiertoParaAnular(posProperties.getFactura().isRequiereTurnoAbiertoParaAnular());
            cambio = true;
        }
        if (config.getMaxIntentosLogin() == null) {
            config.setMaxIntentosLogin(posProperties.getJwt().getMaxIntentosLogin());
            cambio = true;
        }
        if (config.getBloqueoMinutos() == null) {
            config.setBloqueoMinutos((int) posProperties.getJwt().getBloqueoMinutos());
            cambio = true;
        }
        return cambio ? configuracionRepository.save(config) : config;
    }

    private List<HorarioVentaLicor> asegurarHorarios() {
        List<HorarioVentaLicor> existentes = new ArrayList<>(horarioRepository.findAll());
        for (DayOfWeek dia : DayOfWeek.values()) {
            boolean hay = existentes.stream().anyMatch(item -> item.getDiaSemana() == dia);
            if (!hay) {
                existentes.add(horarioRepository.save(HorarioVentaLicor.builder()
                        .diaSemana(dia)
                        .horaInicio(LocalTime.of(8, 0))
                        .horaFin(LocalTime.of(22, 0))
                        .activo(true)
                        .build()));
            }
        }
        existentes.sort(Comparator.comparing(HorarioVentaLicor::getDiaSemana));
        return existentes;
    }

    private void aplicar(ConfiguracionPos configuracion) {
        posProperties.getNormativa().setEdadMinimaAlcohol(configuracion.getEdadMinimaAlcohol());
        posProperties.getNormativa().setHorarioHabilitado(Boolean.TRUE.equals(configuracion.getHorarioHabilitado()));
        posProperties.getImpuesto().setTasaIva(configuracion.getTasaIva());
        posProperties.getMayorista().setVolumenMinimoUmm(configuracion.getVolumenMinimoUmm());
        posProperties.getFactura().setRequiereTurnoAbiertoParaAnular(
                Boolean.TRUE.equals(configuracion.getRequiereTurnoAbiertoParaAnular()));
        posProperties.getJwt().setMaxIntentosLogin(configuracion.getMaxIntentosLogin());
        posProperties.getJwt().setBloqueoMinutos(configuracion.getBloqueoMinutos());
    }

    private ConfiguracionDTO toDto(ConfiguracionPos configuracion, List<HorarioVentaLicor> horarios) {
        List<HorarioVentaDTO> lineas = horarios.stream()
                .sorted(Comparator.comparing(HorarioVentaLicor::getDiaSemana))
                .map(this::toHorario)
                .toList();
        int diasActivos = (int) lineas.stream().filter(item -> Boolean.TRUE.equals(item.getActivo())).count();
        return ConfiguracionDTO.builder()
                .nombreNegocio(configuracion.getNombreNegocio())
                .direccionNegocio(configuracion.getDireccionNegocio())
                .telefonoNegocio(configuracion.getTelefonoNegocio())
                .edadMinimaAlcohol(configuracion.getEdadMinimaAlcohol())
                .horarioHabilitado(configuracion.getHorarioHabilitado())
                .tasaIva(configuracion.getTasaIva())
                .volumenMinimoUmm(configuracion.getVolumenMinimoUmm())
                .requiereTurnoAbiertoParaAnular(configuracion.getRequiereTurnoAbiertoParaAnular())
                .maxIntentosLogin(configuracion.getMaxIntentosLogin())
                .bloqueoMinutos(configuracion.getBloqueoMinutos())
                .horarios(lineas)
                .facturacionFiscalHabilitada(configuracion.getFacturacionFiscalHabilitada())
                .rucEmisor(configuracion.getRucEmisor())
                .autorizacionDgi(configuracion.getAutorizacionDgi())
                .establecimiento(configuracion.getEstablecimiento())
                .puntoEmision(configuracion.getPuntoEmision())
                .tipoDocumentoFiscal(configuracion.getTipoDocumentoFiscal())
                .rangoInicial(configuracion.getRangoInicial())
                .rangoFinal(configuracion.getRangoFinal())
                .correlativoActual(configuracion.getCorrelativoActual())
                .fechaLimiteEmision(configuracion.getFechaLimiteEmision())
                .estadoFiscal(numeracionFiscalService.estado())
                .actualizadoEn(configuracion.getActualizadoEn())
                .diasHorarioActivos(diasActivos)
                .build();
    }

    private HorarioVentaDTO toHorario(HorarioVentaLicor horario) {
        return HorarioVentaDTO.builder()
                .id(horario.getId())
                .diaSemana(horario.getDiaSemana())
                .horaInicio(horario.getHoraInicio())
                .horaFin(horario.getHoraFin())
                .activo(horario.getActivo())
                .build();
    }

    private String resumen(ConfiguracionPos configuracion, List<HorarioVentaLicor> horarios) {
        BigDecimal tasa = configuracion.getTasaIva() == null ? BigDecimal.ZERO : configuracion.getTasaIva();
        return "negocio=" + configuracion.getNombreNegocio()
                + ", edad=" + configuracion.getEdadMinimaAlcohol()
                + ", iva=" + tasa
                + ", mayorista=" + configuracion.getVolumenMinimoUmm()
                + ", horarioOn=" + configuracion.getHorarioHabilitado()
                + ", anularTurno=" + configuracion.getRequiereTurnoAbiertoParaAnular()
                + ", loginMax=" + configuracion.getMaxIntentosLogin()
                + ", bloqueoMin=" + configuracion.getBloqueoMinutos()
                + ", fiscalOn=" + configuracion.getFacturacionFiscalHabilitada()
                + ", autorizacion=" + configuracion.getAutorizacionDgi()
                + ", rango=" + configuracion.getRangoInicial() + "-" + configuracion.getRangoFinal()
                + ", correlativo=" + configuracion.getCorrelativoActual()
                + ", dias=" + horarios.size();
    }
}
