package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.HorarioVentaLicor;
import com.licoreria.pos.repository.HorarioVentaLicorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorarioVentaServiceTest {

    @Mock
    private HorarioVentaLicorRepository horarioRepository;

    private HorarioVentaService service;
    private final ZoneId zona = ZoneId.of("America/Managua");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 8, 19, 23, 30).atZone(zona).toInstant(),
                zona
        );
        service = new HorarioVentaService(new PosProperties(), horarioRepository, clock);
    }

    @Test
    void bloqueaLicorFueraDeHorario() {
        when(horarioRepository.findByDiaSemanaAndActivoTrue(DayOfWeek.WEDNESDAY))
                .thenReturn(Optional.of(HorarioVentaLicor.builder()
                        .diaSemana(DayOfWeek.WEDNESDAY)
                        .horaInicio(LocalTime.of(8, 0))
                        .horaFin(LocalTime.of(22, 0))
                        .activo(true)
                        .build()));

        assertThrows(ReglaNegocioException.class, () -> service.validarVentaLicor(true));
    }

    @Test
    void permiteProductosNoAlcoholicosFueraDeHorario() {
        assertDoesNotThrow(() -> service.validarVentaLicor(false));
    }
}
