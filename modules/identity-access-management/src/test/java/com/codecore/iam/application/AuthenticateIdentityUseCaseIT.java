package com.codecore.iam.application;

import com.codecore.iam.application.command.AuthenticationCommand;
import com.codecore.iam.application.command.RegisterIdentityCommand;
import com.codecore.iam.application.port.in.AuthenticateIdentityUseCase;
import com.codecore.iam.application.port.in.RegisterIdentityUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
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
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({IamModuleConfiguration.class, R2dbcIdentityRepository.class, BCryptPasswordHasher.class})
class AuthenticateIdentityUseCaseIT extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

    @Autowired
    private AuthenticateIdentityUseCase authenticateIdentityUseCase;

    @Autowired
    private RegisterIdentityUseCase registerIdentityUseCase;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private PasswordHasher passwordHasher;

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
                    assertThat(result.tenantId()).isEqualTo(tenantId);
                    assertThat(result.email().value()).isEqualTo(email.toLowerCase());
                    assertThat(result.status()).isEqualTo(IdentityStatus.ACTIVE);
                    assertThat(result.identityId()).isNotNull();
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

    @ParameterizedTest
    @EnumSource(
            value = IdentityStatus.class,
            names = {"LOCKED", "DISABLED", "PASSWORD_RESET_REQUIRED"}
    )
    void shouldRejectNonActivePersistedStatuses(IdentityStatus status) {
        TenantId tenantId = TenantId.generate();
        String email = "auth.%s.%s@codecore.local".formatted(status.name().toLowerCase(), tenantId.value());

        StepVerifier.create(persistIdentity(tenantId, email, status, PASSWORD))
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
        return persistIdentity(tenantId, email, IdentityStatus.ACTIVE, rawPassword);
    }

    private reactor.core.publisher.Mono<Identity> persistIdentity(
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
