package com.quickcart.catalog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtVerifier {

    private final SecretKey key;
    private final String issuer;

    public JwtVerifier(@Value("${quickcart.jwt.secret}") String secret,
                       @Value("${quickcart.jwt.issuer}") String issuer) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.issuer = issuer;
    }

    /** Empty for any invalid, expired, wrong-issuer, wrong-algorithm or unknown-role token. Never throws. */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            if (!"HS256".equals(jws.getHeader().getAlgorithm())) {
                return Optional.empty();
            }
            Claims c = jws.getPayload();
            String role = c.get("role", String.class);
            if (!"CUSTOMER".equals(role) && !"ADMIN".equals(role)) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(
                    Long.parseLong(c.getSubject()), c.get("email", String.class), role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}