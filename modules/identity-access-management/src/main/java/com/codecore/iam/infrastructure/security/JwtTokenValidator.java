package com.codecore.iam.infrastructure.security;

import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import com.codecore.iam.application.port.out.TokenValidator;
import com.codecore.iam.domain.exception.ExpiredTokenException;
import com.codecore.iam.domain.exception.InvalidTokenException;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates CodeCore HS256 JWTs and maps claims to {@link AuthenticatedPrincipal}.
 */
@Component
public class JwtTokenValidator implements TokenValidator {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INVALID_TOKEN_MESSAGE = "Invalid token";
    private static final String EXPIRED_TOKEN_MESSAGE = "Token has expired";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenValidator(JwtProperties jwtProperties) {
        this.jwtProperties = Objects.requireNonNull(jwtProperties, "jwtProperties");
        this.signingKey = hmacSha256Key(jwtProperties.getSecret());
    }

    @Override
    public AuthenticatedPrincipal validate(String accessToken) {
        String rawToken = normalizeToken(accessToken);
        if (rawToken.isBlank()) {
            throw new InvalidTokenException(INVALID_TOKEN_MESSAGE);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseSignedClaims(rawToken)
                    .getPayload();
            return toPrincipal(claims);
        } catch (ExpiredJwtException ex) {
            throw new ExpiredTokenException(EXPIRED_TOKEN_MESSAGE);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException(INVALID_TOKEN_MESSAGE);
        }
    }

    private AuthenticatedPrincipal toPrincipal(Claims claims) {
        String subject = claims.getSubject();
        String email = claims.get("email", String.class);
        String statusValue = claims.get("status", String.class);

        if (subject == null || subject.isBlank() || email == null || email.isBlank()
                || statusValue == null || statusValue.isBlank()) {
            throw new InvalidTokenException(INVALID_TOKEN_MESSAGE);
        }

        try {
            return new AuthenticatedPrincipal(
                    new IdentityId(subject),
                    email,
                    IdentityStatus.valueOf(statusValue),
                    parseTenantId(claims.get("tenantId", String.class))
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException(INVALID_TOKEN_MESSAGE);
        }
    }

    private static Optional<TenantId> parseTenantId(String tenantIdValue) {
        if (tenantIdValue == null || tenantIdValue.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new TenantId(tenantIdValue));
    }

    private static String normalizeToken(String accessToken) {
        if (accessToken == null) {
            return "";
        }
        String trimmed = accessToken.trim();
        if (trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return trimmed.substring(BEARER_PREFIX.length()).trim();
        }
        return trimmed;
    }

    private static SecretKey hmacSha256Key(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}
