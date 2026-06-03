package com.codecore.iam.application;

import com.codecore.iam.application.command.AuthenticationCommand;
import com.codecore.iam.application.command.RegisterIdentityCommand;
import com.codecore.iam.application.port.in.AuthenticateIdentityUseCase;
import com.codecore.iam.application.port.in.RegisterIdentityUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.domain.exception.IdentityNotMemberOfTenantException;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.configuration.IamAuthenticationConfiguration;
import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.domain.exception.IdentityNotAllowedToAuthenticateException;
import com.codecore.iam.domain.exception.InvalidCredentialsException;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.infrastructure.security.JwtTokenProvider;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamR2dbcTestConfiguration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
        IamModuleConfiguration.class,
        IamR2dbcTestConfiguration.class,
        IamAuthenticationConfiguration.class,
        R2dbcIdentityRepository.class,
        R2dbcMembershipRepository.class,
        R2dbcTenantRepository.class,
        BCryptPasswordHasher.class,
        JwtTokenProvider.class
})
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class AuthenticateIdentityUseCaseIT extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";
    private static final String JWT_SECRET = "codecore-test-jwt-secret-key-minimum-32-characters-long!!";

    @Autowired
    private AuthenticateIdentityUseCase authenticateIdentityUseCase;

    @Autowired
    private RegisterIdentityUseCase registerIdentityUseCase;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private MembershipRepository membershipRepository;

    @Test
    void shouldAuthenticateActiveIdentityAgainstPostgreSQL() {
        TenantId tenantId = TenantId.generate();
        String email = "auth.active.%s@codecore.local".formatted(tenantId.value());

        StepVerifier.create(persistActiveIdentity(tenantId, email, PASSWORD))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(authenticateIdentityUseCase.execute(
                        new AuthenticationCommand(tenantId, email, PASSWORD)))
                .assertNext(result -> {
                    assertThat(result.accessToken()).isNotBlank();
                    assertThat(result.tokenType()).isEqualTo("Bearer");
                    assertThat(result.expiresIn()).isEqualTo(900L);

                    Claims claims = Jwts.parser()
                            .verifyWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                            .requireIssuer("codecore-test")
                            .build()
                            .parseSignedClaims(result.accessToken())
                            .getPayload();
                    assertThat(claims.get("email", String.class)).isEqualTo(email.toLowerCase());
                    assertThat(claims.get("status", String.class)).isEqualTo("ACTIVE");
                    assertThat(claims.get("tenantId", String.class)).isEqualTo(tenantId.value().toString());
                    assertThat(claims.getSubject()).isNotBlank();
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectIncorrectPasswordAgainstPostgreSQL() {
        TenantId tenantId = TenantId.generate();
        String email = "auth.wrong.%s@codecore.local".formatted(tenantId.value());

        StepVerifier.create(persistActiveIdentity(tenantId, email, PASSWORD))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(authenticateIdentityUseCase.execute(
                        new AuthenticationCommand(tenantId, email, "WrongPass1!")))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void shouldRejectUnknownIdentityAgainstPostgreSQL() {
        TenantId tenantId = TenantId.generate();
        String email = "auth.missing.%s@codecore.local".formatted(tenantId.value());

        StepVerifier.create(authenticateIdentityUseCase.execute(
                        new AuthenticationCommand(tenantId, email, PASSWORD)))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void shouldRejectPendingVerificationAfterRegistration() {
        TenantId tenantId = TenantId.generate();
        String email = "auth.pending.%s@codecore.local".formatted(tenantId.value());

        StepVerifier.create(registerIdentityUseCase.execute(
                        new RegisterIdentityCommand(tenantId, email, PASSWORD)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(authenticateIdentityUseCase.execute(
                        new AuthenticationCommand(tenantId, email, PASSWORD)))
                .expectError(IdentityNotAllowedToAuthenticateException.class)
                .verify();
    }

    @Test
    void shouldRejectLoginWhenMembershipMissing() {
        TenantId tenantId = TenantId.generate();
        String email = "auth.no.membership.%s@codecore.local".formatted(tenantId.value());

        StepVerifier.create(persistIdentityOnly(tenantId, email, IdentityStatus.ACTIVE, PASSWORD))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(authenticateIdentityUseCase.execute(
                        new AuthenticationCommand(tenantId, email, PASSWORD)))
                .expectError(IdentityNotMemberOfTenantException.class)
                .verify();
    }

    @Test
    void shouldRejectLoginWhenMembershipInactive() {
        TenantId tenantId = TenantId.generate();
        String email = "auth.inactive.membership.%s@codecore.local".formatted(tenantId.value());

        StepVerifier.create(persistIdentityOnly(tenantId, email, IdentityStatus.ACTIVE, PASSWORD)
                        .flatMap(saved -> {
                            IdentityTenantMembership membership = IdentityTenantMembership.create(
                                    saved.id(),
                                    saved.tenantId(),
                                    Instant.now()
                            );
                            membership.deactivate();
                            return membershipRepository.save(membership);
                        }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(authenticateIdentityUseCase.execute(
                        new AuthenticationCommand(tenantId, email, PASSWORD)))
                .expectError(IdentityNotMemberOfTenantException.class)
                .verify();
    }

    @Test
    void shouldCreateActiveMembershipOnRegistration() {
        TenantId tenantId = TenantId.generate();
        String email = "register.membership.%s@codecore.local".formatted(tenantId.value());

        StepVerifier.create(registerIdentityUseCase.execute(
                        new RegisterIdentityCommand(tenantId, email, PASSWORD)))
                .assertNext(result -> StepVerifier.create(
                                membershipRepository.exists(result.identityId(), tenantId))
                        .expectNext(true)
                        .verifyComplete())
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(
            value = IdentityStatus.class,
            names = {"LOCKED", "DISABLED", "PASSWORD_RESET_REQUIRED"}
    )
    void shouldRejectNonActivePersistedStatuses(IdentityStatus status) {
        TenantId tenantId = TenantId.generate();
        String email = "auth.%s.%s@codecore.local".formatted(status.name().toLowerCase(), tenantId.value());

        StepVerifier.create(persistIdentityWithMembership(tenantId, email, status, PASSWORD))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(authenticateIdentityUseCase.execute(
                        new AuthenticationCommand(tenantId, email, PASSWORD)))
                .expectError(IdentityNotAllowedToAuthenticateException.class)
                .verify();
    }

    private reactor.core.publisher.Mono<Identity> persistActiveIdentity(
            TenantId tenantId,
            String email,
            String rawPassword
    ) {
        return persistIdentityWithMembership(tenantId, email, IdentityStatus.ACTIVE, rawPassword);
    }

    private reactor.core.publisher.Mono<Identity> persistIdentityWithMembership(
            TenantId tenantId,
            String email,
            IdentityStatus status,
            String rawPassword
    ) {
        return persistIdentityOnly(tenantId, email, status, rawPassword)
                .flatMap(saved -> {
                    IdentityTenantMembership membership = IdentityTenantMembership.create(
                            saved.id(),
                            saved.tenantId(),
                            Instant.now()
                    );
                    return membershipRepository.save(membership).thenReturn(saved);
                });
    }

    private reactor.core.publisher.Mono<Identity> persistIdentityOnly(
            TenantId tenantId,
            String email,
            IdentityStatus status,
            String rawPassword
    ) {
        IdentityId identityId = IdentityId.generate();
        Instant now = Instant.now();
        String hashed = passwordHasher.hash(rawPassword);
        Credential credential = new Credential(
                new CredentialId(identityId.value()),
                PasswordHash.ofHashedValue(hashed),
                now,
                null,
                status == IdentityStatus.PASSWORD_RESET_REQUIRED,
                0L
        );
        Identity identity = new Identity(
                identityId,
                tenantId,
                EmailAddress.of(email),
                status,
                credential,
                null,
                now,
                now,
                0L
        );
        return identityRepository.save(identity);
    }
}
