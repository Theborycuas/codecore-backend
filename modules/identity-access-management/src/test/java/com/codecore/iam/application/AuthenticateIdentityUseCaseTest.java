package com.codecore.iam.application;

import com.codecore.iam.application.command.AuthenticationCommand;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateIdentityUseCaseTest {

    private static final String EMAIL = "auth@codecore.local";
    private static final String PASSWORD = "ValidPass1!";
    private static final String HASH = "$2a$10$storedhash";

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private AuthenticateIdentityUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateIdentityUseCaseImpl(identityRepository, passwordHasher);
    }

    @Test
    void shouldAuthenticateActiveIdentityWithCorrectPassword() {
        TenantId tenantId = TenantId.generate();
        Identity identity = identity(tenantId, EMAIL, IdentityStatus.ACTIVE, HASH);
        when(identityRepository.findByTenantAndEmail(eq(tenantId), any(EmailAddress.class)))
                .thenReturn(Mono.just(identity));
        when(passwordHasher.matches(PASSWORD, HASH)).thenReturn(true);

        StepVerifier.create(useCase.execute(new AuthenticationCommand(tenantId, EMAIL, PASSWORD)))
                .assertNext(result -> {
                    assertThat(result.identityId()).isEqualTo(identity.id());
                    assertThat(result.tenantId()).isEqualTo(tenantId);
                    assertThat(result.email().value()).isEqualTo(EMAIL.toLowerCase());
                    assertThat(result.status()).isEqualTo(IdentityStatus.ACTIVE);
                })
                .verifyComplete();

        verify(passwordHasher).matches(PASSWORD, HASH);
    }

    @Test
    void shouldRejectActiveIdentityWithIncorrectPassword() {
        TenantId tenantId = TenantId.generate();
        Identity identity = identity(tenantId, EMAIL, IdentityStatus.ACTIVE, HASH);
        when(identityRepository.findByTenantAndEmail(eq(tenantId), any(EmailAddress.class)))
                .thenReturn(Mono.just(identity));
        when(passwordHasher.matches("WrongPass1!", HASH)).thenReturn(false);

        StepVerifier.create(useCase.execute(new AuthenticationCommand(tenantId, EMAIL, "WrongPass1!")))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void shouldRejectWhenIdentityDoesNotExist() {
        TenantId tenantId = TenantId.generate();
        when(identityRepository.findByTenantAndEmail(eq(tenantId), any(EmailAddress.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(new AuthenticationCommand(tenantId, EMAIL, PASSWORD)))
                .expectError(InvalidCredentialsException.class)
                .verify();

        verify(passwordHasher, never()).matches(any(), any());
    }

    @ParameterizedTest
    @EnumSource(
            value = IdentityStatus.class,
            names = {"PENDING_VERIFICATION", "LOCKED", "DISABLED", "PASSWORD_RESET_REQUIRED"}
    )
    void shouldRejectNonActiveStatuses(IdentityStatus status) {
        TenantId tenantId = TenantId.generate();
        Identity identity = identity(tenantId, EMAIL, status, HASH);
        when(identityRepository.findByTenantAndEmail(eq(tenantId), any(EmailAddress.class)))
                .thenReturn(Mono.just(identity));

        StepVerifier.create(useCase.execute(new AuthenticationCommand(tenantId, EMAIL, PASSWORD)))
                .expectError(IdentityNotAllowedToAuthenticateException.class)
                .verify();

        verify(passwordHasher, never()).matches(any(), any());
    }

    private static Identity identity(
            TenantId tenantId,
            String email,
            IdentityStatus status,
            String hashValue
    ) {
        IdentityId id = IdentityId.generate();
        Instant now = Instant.now();
        Credential credential = new Credential(
                new CredentialId(id.value()),
                PasswordHash.ofHashedValue(hashValue),
                now,
                null,
                status == IdentityStatus.PASSWORD_RESET_REQUIRED,
                0L
        );
        return new Identity(
                id,
                tenantId,
                EmailAddress.of(email),
                status,
                credential,
                null,
                now,
                now,
                0L
        );
    }
}
