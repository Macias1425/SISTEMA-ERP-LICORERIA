package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.dto.ConfiguracionDTO;
import com.licoreria.pos.dto.HorarioVentaDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.ConfiguracionPos;
import com.licoreria.pos.model.HorarioVentaLicor;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.ConfiguracionPosRepository;
import com.licoreria.pos.repository.HorarioVentaLicorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceTest {

    @Mock
    private ConfiguracionPosRepository configuracionRepository;
    @Mock
    private HorarioVentaLicorRepository horarioRepository;
    @Mock
    private AccesoService accesoService;
    @Mock
    private NumeracionFiscalService numeracionFiscalService;
    @Mock
    private AuditoriaService auditoriaService;

    private PosProperties posProperties;
    private ConfiguracionService configuracionService;

    @BeforeEach
    void setUp() {
        posProperties = new PosProperties();
        Clock clock = Clock.fixed(
                java.time.Instant.parse("2026-08-24T02:00:00Z"),
                ZoneOffset.UTC
        );
        configuracionService = new ConfiguracionService(
                configuracionRepository, horarioRepository, posProperties,
                accesoService, numeracionFiscalService, auditoriaService, clock
        );
    }

    @Test
    void guardaYAplicaReglasAlBean() {
        when(accesoService.exigirPermiso(Permiso.CONFIG_GESTIONAR)).thenReturn(
                Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build());
        when(configuracionRepository.findById(1L)).thenReturn(Optional.of(configBase()));
        when(horarioRepository.findAll()).thenReturn(List.of());
        when(horarioRepository.save(any(HorarioVentaLicor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(horarioRepository.findByDiaSemana(any())).thenReturn(Optional.empty());
        when(configuracionRepository.save(any(ConfiguracionPos.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfiguracionDTO request = ConfiguracionDTO.builder()
                .nombreNegocio("Licorería Central")
                .edadMinimaAlcohol(21)
                .horarioHabilitado(false)
                .tasaIva(new BigDecimal("0.18"))
                .volumenMinimoUmm(12)
                .requiereTurnoAbiertoParaAnular(false)
                .maxIntentosLogin(8)
                .bloqueoMinutos(20)
                .horarios(horariosValidos())
                .build();

        configuracionService.guardar(request);

        assertEquals(21, posProperties.getNormativa().getEdadMinimaAlcohol());
        assertEquals(false, posProperties.getNormativa().isHorarioHabilitado());
        assertEquals(new BigDecimal("0.1800"), posProperties.getImpuesto().getTasaIva());
        assertEquals(12, posProperties.getMayorista().getVolumenMinimoUmm());
        assertEquals(false, posProperties.getFactura().isRequiereTurnoAbiertoParaAnular());
        assertEquals(8, posProperties.getJwt().getMaxIntentosLogin());
        assertEquals(20, posProperties.getJwt().getBloqueoMinutos());
    }

    @Test
    void rechazaHoraInicioPosterior() {
        when(accesoService.exigirPermiso(Permiso.CONFIG_GESTIONAR)).thenReturn(
                Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build());

        List<HorarioVentaDTO> horarios = horariosValidos();
        horarios.get(0).setHoraInicio(LocalTime.of(22, 0));
        horarios.get(0).setHoraFin(LocalTime.of(8, 0));

        assertThrows(ReglaNegocioException.class, () -> configuracionService.guardar(requestBase(horarios)));
    }

    @Test
    void rechazaHorarioSinDiasActivosCuandoEstaHabilitado() {
        when(accesoService.exigirPermiso(Permiso.CONFIG_GESTIONAR)).thenReturn(
                Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build());

        List<HorarioVentaDTO> horarios = horariosValidos();
        horarios.forEach(item -> item.setActivo(false));

        ConfiguracionDTO request = requestBase(horarios);
        request.setHorarioHabilitado(true);

        assertThrows(ReglaNegocioException.class, () -> configuracionService.guardar(request));
    }

    @Test
    void rechazaHorarioMenorAUnaHora() {
        when(accesoService.exigirPermiso(Permiso.CONFIG_GESTIONAR)).thenReturn(
                Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build());

        List<HorarioVentaDTO> horarios = horariosValidos();
        horarios.get(0).setHoraInicio(LocalTime.of(8, 0));
        horarios.get(0).setHoraFin(LocalTime.of(8, 30));

        assertThrows(ReglaNegocioException.class, () -> configuracionService.guardar(requestBase(horarios)));
    }

    private ConfiguracionPos configBase() {
        return ConfiguracionPos.builder()
                .id(1L)
                .nombreNegocio("Licorería POS")
                .edadMinimaAlcohol(18)
                .horarioHabilitado(true)
                .tasaIva(new BigDecimal("0.15"))
                .volumenMinimoUmm(6)
                .requiereTurnoAbiertoParaAnular(true)
                .maxIntentosLogin(5)
                .bloqueoMinutos(15)
                .build();
    }

    private ConfiguracionDTO requestBase(List<HorarioVentaDTO> horarios) {
        return ConfiguracionDTO.builder()
                .nombreNegocio("Licorería POS")
                .edadMinimaAlcohol(18)
                .horarioHabilitado(true)
                .tasaIva(new BigDecimal("0.15"))
                .volumenMinimoUmm(6)
                .requiereTurnoAbiertoParaAnular(true)
                .maxIntentosLogin(5)
                .bloqueoMinutos(15)
                .horarios(horarios)
                .build();
    }

    private List<HorarioVentaDTO> horariosValidos() {
        return new ArrayList<>(Arrays.stream(DayOfWeek.values())
                .map(dia -> HorarioVentaDTO.builder()
                        .diaSemana(dia)
                        .horaInicio(LocalTime.of(8, 0))
                        .horaFin(LocalTime.of(22, 0))
                        .activo(true)
                        .build())
                .toList());
    }
}
