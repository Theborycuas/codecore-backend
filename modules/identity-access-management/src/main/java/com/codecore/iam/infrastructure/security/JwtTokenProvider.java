package com.codecore.iam.infrastructure.security;

import com.codecore.iam.application.dto.AccessTokenClaims;
import com.codecore.iam.application.dto.IssuedAccessToken;
import com.codecore.iam.application.port.out.TokenProvider;
import com.codecore.iam.infrastructure.security.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * JWT access token adapter — HS256 signing only (no Spring Security OAuth).
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private static final String TOKEN_TYPE = "Bearer";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = Objects.requireNonNull(jwtProperties, "jwtProperties");
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IssuedAccessToken generateAccessToken(AccessTokenClaims claims) {
        Objects.requireNonNull(claims, "claims");
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.getExpiration());
        long expiresInSeconds = jwtProperties.getExpiration().toSeconds();

        String accessToken = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(claims.subject())
                .claim("email", claims.email())
                .claim("status", claims.status())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return new IssuedAccessToken(accessToken, TOKEN_TYPE, expiresInSeconds);
    }
}
