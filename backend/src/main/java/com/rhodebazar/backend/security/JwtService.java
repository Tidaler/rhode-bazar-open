package com.rhodebazar.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${rhodebazar.jwt.secret}") String secret,
            @Value("${rhodebazar.jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String genererToken(String email, String role) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    public String extraireEmail(String token) {
        return extraireClaims(token).getSubject();
    }

    public boolean estValide(String token, String email) {
        try {
            Claims claims = extraireClaims(token);
            return claims.getSubject().equalsIgnoreCase(email) && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims extraireClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
