package com.licoreria.pos.security;

import com.licoreria.pos.config.PosProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(PosProperties posProperties) {
        String secret = posProperties.getJwt().getSecret();
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = posProperties.getJwt().getExpirationMs();
    }

    public String generar(UsuarioPrincipal principal) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getId())
                .claim("rol", principal.getRol().name())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extraerUsername(String token) {
        return claims(token).getSubject();
    }

    public boolean esValido(String token, String username) {
        Claims claims = claims(token);
        return username.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
