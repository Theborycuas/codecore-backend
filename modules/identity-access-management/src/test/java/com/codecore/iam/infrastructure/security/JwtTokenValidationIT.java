package com.codecore.iam.infrastructure.security;

import com.codecore.iam.application.dto.AccessTokenClaims;
import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import com.codecore.iam.application.dto.IssuedAccessToken;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.infrastructure.security.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip: {@link JwtTokenProvider} issues a token, {@link JwtTokenValidator} validates it.
 */
class JwtTokenValidationIT {

    private static final String SECRET = "codecore-test-jwt-secret-key-minimum-32-characters-long!!";
    private static final String ISSUER = "codecore-test";

    @Test
    void shouldRoundTripIssuedTokenToAuthenticatedPrincipal() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer(ISSUER);
        properties.setExpiration(Duration.ofSeconds(900));

        JwtTokenProvider provider = new JwtTokenProvider(properties);
        JwtTokenValidator validator = new JwtTokenValidator(properties);

        String identityId = UUID.randomUUID().toString();
        String tenantId = UUID.randomUUID().toString();
        String email = "roundtrip.%s@codecore.local".formatted(identityId);

        IssuedAccessToken issued = provider.generateAccessToken(
                new AccessTokenClaims(identityId, email, "ACTIVE", tenantId));

        AuthenticatedPrincipal principal = validator.validate(issued.accessToken());

        assertThat(principal.identityId().value()).hasToString(identityId);
        assertThat(principal.email()).isEqualTo(email);
        assertThat(principal.status()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(principal.tenantId()).contains(new com.codecore.iam.domain.valueobject.TenantId(tenantId));
        assertThat(issued.tokenType()).isEqualTo("Bearer");
        assertThat(issued.expiresIn()).isEqualTo(900L);
    }
}
