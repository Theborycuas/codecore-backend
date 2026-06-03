package com.codecore.iam.infrastructure.security;

import com.codecore.iam.application.dto.AccessTokenClaims;
import com.codecore.iam.application.dto.IssuedAccessToken;
import com.codecore.iam.infrastructure.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "codecore-test-jwt-secret-key-minimum-32-characters-long!!";
    private static final String ISSUER = "codecore-test";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer(ISSUER);
        properties.setExpiration(Duration.ofSeconds(900));
        provider = new JwtTokenProvider(properties);
    }

    @Test
    void shouldIssueBearerTokenWithRequiredClaims() {
        String subject = "33333333-3333-3333-3333-333333333333";
        String email = "jwt.test@codecore.local";

        IssuedAccessToken issued = provider.generateAccessToken(
                new AccessTokenClaims(subject, email, "ACTIVE"));

        assertThat(issued.accessToken()).isNotBlank();
        assertThat(issued.tokenType()).isEqualTo("Bearer");
        assertThat(issued.expiresIn()).isEqualTo(900L);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(issued.accessToken())
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(subject);
        assertThat(claims.get("email", String.class)).isEqualTo(email);
        assertThat(claims.get("status", String.class)).isEqualTo("ACTIVE");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.get("tenantId")).isNull();
        assertThat(claims.get("roles")).isNull();
        assertThat(claims.get("permissions")).isNull();
    }
}
