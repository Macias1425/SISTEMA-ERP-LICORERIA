package com.licoreria.pos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "pos")
public class PosProperties {

    private Normativa normativa = new Normativa();
    private Impuesto impuesto = new Impuesto();
    private Mayorista mayorista = new Mayorista();
    private Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Normativa {
        private int edadMinimaAlcohol = 18;
        private boolean horarioHabilitado = true;
    }

    @Getter
    @Setter
    public static class Impuesto {
        private BigDecimal tasaIsv = new BigDecimal("0.15");
    }

    @Getter
    @Setter
    public static class Mayorista {
        private int volumenMinimoUmm = 6;
    }

    @Getter
    @Setter
    public static class Jwt {
        /** Mínimo 32 caracteres (256 bits) para HS256. En producción usar JWT_SECRET. */
        private String secret = "cambiar-esta-clave-en-produccion-pos-licoreria-2026";
        private long expirationMs = 28_800_000L;
        private int maxIntentosLogin = 5;
        private long bloqueoMinutos = 15;
    }
}
