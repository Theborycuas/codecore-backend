package com.codecore.iam.application;

import com.codecore.iam.application.admin.IdentityRegistrationOrchestrator;
import com.codecore.iam.application.command.RegisterIdentityCommand;
import com.codecore.iam.application.dto.RegisterIdentityResult;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.domain.exception.IdentityAlreadyExistsException;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class RegisterIdentityUseCaseTest {

    private static final String EMAIL = "user@codecore.local";
    private static final String PASSWORD = "ValidPass1!";

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private IdentityRegistrationOrchestrator registrationOrchestrator;

    private RegisterIdentityUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterIdentityUseCaseImpl(identityRepository, registrationOrchestrator);
    }

    @Test
    void shouldRegisterIdentitySuccessfully() {
        TenantId tenantId = TenantId.generate();
        IdentityId identityId = IdentityId.generate();
        Instant now = Instant.now();
        Identity saved = new Identity(
                identityId,
                tenantId,
                EmailAddress.of(EMAIL),
                IdentityStatus.PENDING_VERIFICATION,
                null,
                null,
                now,
                now,
                0L
        );

        when(identityRepository.existsByEmail(any(EmailAddress.class))).thenReturn(Mono.just(false));
        when(registrationOrchestrator.registerNewIdentity(
                eq(tenantId),
                any(EmailAddress.class),
                any(),
                eq(IdentityStatus.PENDING_VERIFICATION)
        )).thenReturn(Mono.just(saved));

        RegisterIdentityCommand command = new RegisterIdentityCommand(tenantId, EMAIL, PASSWORD);

        StepVerifier.create(useCase.execute(command))
                .assertNext(result -> {
                    assertThat(result.tenantId()).isEqualTo(tenantId);
                    assertThat(result.email().value()).isEqualTo(EMAIL.toLowerCase());
                    assertThat(result.status()).isEqualTo(IdentityStatus.PENDING_VERIFICATION);
                    assertThat(result.identityId()).isEqualTo(identityId);
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateEmailInSameTenant() {
        TenantId tenantId = TenantId.generate();
        when(identityRepository.existsByEmail(any(EmailAddress.class))).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.execute(new RegisterIdentityCommand(tenantId, EMAIL, PASSWORD)))
                .expectError(IdentityAlreadyExistsException.class)
                .verify();

        verify(registrationOrchestrator, never()).registerNewIdentity(any(), any(), any(), any());
    }

    @Test
    void shouldRejectDuplicateEmailInDifferentTenant() {
        TenantId tenantB = TenantId.generate();
        when(identityRepository.existsByEmail(any(EmailAddress.class))).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.execute(new RegisterIdentityCommand(tenantB, EMAIL, PASSWORD)))
                .expectError(IdentityAlreadyExistsException.class)
                .verify();

        verify(registrationOrchestrator, never()).registerNewIdentity(any(), any(), any(), any());
    }

    @Test
    void shouldRejectBlankPassword() {
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(useCase.execute(new RegisterIdentityCommand(tenantId, EMAIL, "   ")))
                .expectError(InvalidDomainValueException.class)
                .verify();

        verify(identityRepository, never()).existsByEmail(any());
    }

    @Test
    void shouldRejectBlankEmail() {
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(useCase.execute(new RegisterIdentityCommand(tenantId, "  ", PASSWORD)))
                .expectError(InvalidDomainValueException.class)
                .verify();

        verify(identityRepository, never()).existsByEmail(any());
    }
}
