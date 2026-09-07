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
    private Factura factura = new Factura();
    private Jwt jwt = new Jwt();
    private Respaldo respaldo = new Respaldo();
    private Stripe stripe = new Stripe();

    @Getter
    @Setter
    public static class Respaldo {
        private String directorio = "./backups";
        private String mysqldumpPath = "mysqldump";
        private String mysqlPath = "mysql";
        private int maxRespaldos = 30;
        private boolean permitirRestaurar = true;
    }

    @Getter
    @Setter
    public static class Normativa {
        private int edadMinimaAlcohol = 18;
        private boolean horarioHabilitado = true;
    }

    @Getter
    @Setter
    public static class Impuesto {
        /** IVA general de Nicaragua: 15%. */
        private BigDecimal tasaIva = new BigDecimal("0.15");
    }

    @Getter
    @Setter
    public static class Mayorista {
        private int volumenMinimoUmm = 6;
    }

    @Getter
    @Setter
    public static class Factura {
        /** RN-FAC-06: solo se anula con el turno de caja aún abierto. */
        private boolean requiereTurnoAbiertoParaAnular = true;
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

    @Getter
    @Setter
    public static class Stripe {
        /** Si false o sin secret-key, el cobro Stripe no se ofrece y el POS no cambia. */
        private boolean enabled = false;
        private String secretKey = "";
        private String publishableKey = "";
        private String webhookSecret = "";
        /** Moneda ISO para PaymentIntent (en test suele usarse usd). */
        private String currency = "usd";
    }
}
