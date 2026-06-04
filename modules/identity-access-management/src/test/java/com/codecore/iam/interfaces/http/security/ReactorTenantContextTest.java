package com.codecore.iam.interfaces.http.security;

import com.codecore.iam.application.dto.AccessTokenClaims;
import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import com.codecore.iam.application.port.out.TenantContext;
import com.codecore.iam.domain.exception.TenantContextUnavailableException;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.security.JwtTokenProvider;
import com.codecore.iam.infrastructure.security.JwtTokenValidator;
import com.codecore.iam.infrastructure.security.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.assertThat;

class ReactorTenantContextTest {

    private static final String SECRET = "codecore-test-jwt-secret-key-minimum-32-characters-long!!";
    private static final String ISSUER = "codecore-test";
    private static final String SUBJECT = "22222222-2222-2222-2222-222222222222";
    private static final String EMAIL = "tenant.context@codecore.local";
    private static final String TENANT_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    private TenantContext tenantContext;
    private JwtTokenProvider tokenProvider;
    private JwtTokenValidator tokenValidator;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer(ISSUER);
        properties.setExpiration(Duration.ofSeconds(900));
        tokenProvider = new JwtTokenProvider(properties);
        tokenValidator = new JwtTokenValidator(properties);
        tenantContext = new ReactorTenantContext();
    }

    @Test
    void shouldResolveTenantWhenJwtContainsTenantId() {
        AuthenticatedPrincipal principal = principalWithTenant(TENANT_UUID);

        StepVerifier.create(
                        tenantContext.currentTenant()
                                .contextWrite(ctx -> AuthenticationContext.write(ctx, principal))
                )
                .expectNext(new TenantId(TENANT_UUID))
                .verifyComplete();
    }

    @Test
    void shouldFailWhenJwtHasNoTenantIdClaim() {
        AuthenticatedPrincipal legacyPrincipal = new AuthenticatedPrincipal(
                new IdentityId(SUBJECT),
                EMAIL,
                IdentityStatus.ACTIVE,
                Optional.empty()
        );

        StepVerifier.create(
                        tenantContext.currentTenant()
                                .contextWrite(ctx -> AuthenticationContext.write(ctx, legacyPrincipal))
                )
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(TenantContextUnavailableException.class);
                    assertThat(((TenantContextUnavailableException) error).reason())
                            .isEqualTo(TenantContextUnavailableException.Reason.TENANT_CLAIM_ABSENT);
                })
                .verify();
    }

    @Test
    void shouldFailWhenRequestIsNotAuthenticated() {
        StepVerifier.create(tenantContext.currentTenant())
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(TenantContextUnavailableException.class);
                    assertThat(((TenantContextUnavailableException) error).reason())
                            .isEqualTo(TenantContextUnavailableException.Reason.NOT_AUTHENTICATED);
                })
                .verify();
    }

    @Test
    void shouldResolveTenantMatchingJwtClaim() {
        String issuedToken = tokenProvider.generateAccessToken(
                new AccessTokenClaims(SUBJECT, EMAIL, "ACTIVE", TENANT_UUID)
        ).accessToken();

        AuthenticatedPrincipal principal = tokenValidator.validate(issuedToken);

        StepVerifier.create(
                        tenantContext.currentTenant()
                                .contextWrite(ctx -> AuthenticationContext.write(ctx, principal))
                )
                .assertNext(tenantId -> assertThat(tenantId.value()).hasToString(TENANT_UUID))
                .verifyComplete();
    }

    @Test
    void shouldFailForLegacyTokenValidatedWithoutTenantClaim() {
        String legacyToken = Jwts.builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .claim("email", EMAIL)
                .claim("status", "ACTIVE")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        AuthenticatedPrincipal principal = tokenValidator.validate(legacyToken);

        StepVerifier.create(
                        tenantContext.currentTenant()
                                .contextWrite(ctx -> AuthenticationContext.write(ctx, principal))
                )
                .expectError(TenantContextUnavailableException.class)
                .verify();
    }

    private static AuthenticatedPrincipal principalWithTenant(String tenantUuid) {
        return new AuthenticatedPrincipal(
                new IdentityId(SUBJECT),
                EMAIL,
                IdentityStatus.ACTIVE,
                Optional.of(new TenantId(tenantUuid))
        );
    }
}
