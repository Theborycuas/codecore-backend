package com.codecore.iam.infrastructure.security;

import com.codecore.iam.application.dto.AccessTokenClaims;
import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import com.codecore.iam.domain.exception.ExpiredTokenException;
import com.codecore.iam.domain.exception.InvalidTokenException;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.infrastructure.security.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenValidatorTest {

    private static final String SECRET = "codecore-test-jwt-secret-key-minimum-32-characters-long!!";
    private static final String WRONG_SECRET = "codecore-wrong-jwt-secret-key-minimum-32-characters-long!";
    private static final String ISSUER = "codecore-test";
    private static final String SUBJECT = "33333333-3333-3333-3333-333333333333";
    private static final String EMAIL = "jwt.validate@codecore.local";

    private JwtTokenProvider tokenProvider;
    private JwtTokenValidator tokenValidator;

    @BeforeEach
    void setUp() {
        JwtProperties properties = properties(SECRET, ISSUER, Duration.ofSeconds(900));
        tokenProvider = new JwtTokenProvider(properties);
        tokenValidator = new JwtTokenValidator(properties);
    }

    @Test
    void shouldValidateValidToken() {
        String tenantId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        String token = tokenProvider.generateAccessToken(
                new AccessTokenClaims(SUBJECT, EMAIL, "ACTIVE", tenantId)).accessToken();

        AuthenticatedPrincipal principal = tokenValidator.validate(token);

        assertThat(principal.identityId().value()).hasToString(SUBJECT);
        assertThat(principal.email()).isEqualTo(EMAIL);
        assertThat(principal.status()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(principal.tenantId()).contains(new com.codecore.iam.domain.valueobject.TenantId(tenantId));
    }

    @Test
    void shouldValidateTokenWithBearerPrefix() {
        String tenantId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
        String token = tokenProvider.generateAccessToken(
                new AccessTokenClaims(SUBJECT, EMAIL, "ACTIVE", tenantId)).accessToken();

        AuthenticatedPrincipal principal = tokenValidator.validate("Bearer " + token);

        assertThat(principal.email()).isEqualTo(EMAIL);
        assertThat(principal.tenantId()).contains(new com.codecore.iam.domain.valueobject.TenantId(tenantId));
    }

    @Test
    void shouldValidateLegacyTokenWithoutTenantId() {
        String token = signedToken(
                properties(SECRET, ISSUER, Duration.ofSeconds(900)),
                SUBJECT,
                EMAIL,
                "ACTIVE",
                null,
                Instant.now(),
                Instant.now().plusSeconds(900)
        );

        AuthenticatedPrincipal principal = tokenValidator.validate(token);

        assertThat(principal.email()).isEqualTo(EMAIL);
        assertThat(principal.tenantId()).isEmpty();
    }

    @Test
    void shouldRejectExpiredToken() {
        String expiredToken = signedToken(
                properties(SECRET, ISSUER, Duration.ofSeconds(900)),
                SUBJECT,
                EMAIL,
                "ACTIVE",
                null,
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(60)
        );

        assertThatThrownBy(() -> tokenValidator.validate(expiredToken))
                .isInstanceOf(ExpiredTokenException.class);
    }

    @Test
    void shouldRejectInvalidSignature() {
        String token = signedToken(
                properties(WRONG_SECRET, ISSUER, Duration.ofSeconds(900)),
                SUBJECT,
                EMAIL,
                "ACTIVE",
                null,
                Instant.now(),
                Instant.now().plusSeconds(900)
        );

        assertThatThrownBy(() -> tokenValidator.validate(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shouldRejectInvalidIssuer() {
        String token = signedToken(
                properties(SECRET, "wrong-issuer", Duration.ofSeconds(900)),
                SUBJECT,
                EMAIL,
                "ACTIVE",
                null,
                Instant.now(),
                Instant.now().plusSeconds(900)
        );

        assertThatThrownBy(() -> tokenValidator.validate(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThatThrownBy(() -> tokenValidator.validate("not.a.valid.jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shouldRejectBlankToken() {
        assertThatThrownBy(() -> tokenValidator.validate("   "))
                .isInstanceOf(InvalidTokenException.class);
    }

    private static JwtProperties properties(String secret, String issuer, Duration expiration) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setIssuer(issuer);
        properties.setExpiration(expiration);
        return properties;
    }

    private static String signedToken(
            JwtProperties properties,
            String subject,
            String email,
            String status,
            String tenantId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        SecretKey key = new SecretKeySpec(
                properties.getSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        var builder = Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(subject)
                .claim("email", email)
                .claim("status", status)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt));
        if (tenantId != null) {
            builder.claim("tenantId", tenantId);
        }
        return builder.signWith(key).compact();
    }
}
