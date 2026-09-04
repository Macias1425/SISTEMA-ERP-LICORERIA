package com.licoreria.pos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /** Zona horaria de Nicaragua (UTC-6, sin horario de verano). */
    public static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Managua");

    @Bean
    public Clock clock() {
        return Clock.system(ZONA_NEGOCIO);
    }
}
