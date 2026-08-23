package com.licoreria.pos.security;

import com.licoreria.pos.config.PosProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final int maxIntentos;
    private final long bloqueoMs;
    private final Clock clock;
    private final Map<String, Intento> intentos = new ConcurrentHashMap<>();

    public LoginAttemptService(PosProperties posProperties, Clock clock) {
        this.maxIntentos = posProperties.getJwt().getMaxIntentosLogin();
        this.bloqueoMs = posProperties.getJwt().getBloqueoMinutos() * 60_000L;
        this.clock = clock;
    }

    public boolean estaBloqueado(String username) {
        Intento intento = intentos.get(normalizar(username));
        if (intento == null || intento.bloqueadoHasta == null) {
            return false;
        }
        if (clock.instant().isAfter(intento.bloqueadoHasta)) {
            intentos.remove(normalizar(username));
            return false;
        }
        return true;
    }

    public void registrarFallo(String username) {
        String clave = normalizar(username);
        Intento intento = intentos.computeIfAbsent(clave, ignored -> new Intento());
        intento.fallos++;
        if (intento.fallos >= maxIntentos) {
            intento.bloqueadoHasta = clock.instant().plusMillis(bloqueoMs);
        }
    }

    public void registrarExito(String username) {
        intentos.remove(normalizar(username));
    }

    private String normalizar(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private static class Intento {
        private int fallos;
        private Instant bloqueadoHasta;
    }
}
