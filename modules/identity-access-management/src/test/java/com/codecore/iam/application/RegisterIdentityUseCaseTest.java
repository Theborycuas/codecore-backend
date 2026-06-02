package com.codecore.iam.application;

import com.codecore.iam.application.command.RegisterIdentityCommand;
import com.codecore.iam.application.dto.RegisterIdentityResult;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

  @Mock private IdentityRepository identityRepository;

  @Mock private PasswordHasher passwordHasher;

  private RegisterIdentityUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new RegisterIdentityUseCaseImpl(identityRepository, passwordHasher);
  }

  @Test
  void shouldRegisterIdentitySuccessfully() {
    TenantId tenantId = TenantId.generate();
    when(identityRepository.existsByTenantAndEmail(eq(tenantId), any(EmailAddress.class)))
        .thenReturn(Mono.just(false));
    when(passwordHasher.hash(PASSWORD)).thenReturn("$2a$10$hashed");
    when(identityRepository.save(any(Identity.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    RegisterIdentityCommand command =
        new RegisterIdentityCommand(tenantId, EMAIL, PASSWORD);

    StepVerifier.create(useCase.execute(command))
        .assertNext(
            result -> {
              assertThat(result.tenantId()).isEqualTo(tenantId);
              assertThat(result.email().value()).isEqualTo(EMAIL.toLowerCase());
              assertThat(result.status()).isEqualTo(IdentityStatus.PENDING_VERIFICATION);
              assertThat(result.identityId()).isNotNull();
            })
        .verifyComplete();

    ArgumentCaptor<Identity> saved = ArgumentCaptor.forClass(Identity.class);
    verify(identityRepository).save(saved.capture());
    assertThat(saved.getValue().status()).isEqualTo(IdentityStatus.PENDING_VERIFICATION);
    assertThat(saved.getValue().credential()).isPresent();
    verify(passwordHasher).hash(PASSWORD);
  }

  @Test
  void shouldRejectDuplicateEmailInSameTenant() {
    TenantId tenantId = TenantId.generate();
    when(identityRepository.existsByTenantAndEmail(eq(tenantId), any(EmailAddress.class)))
        .thenReturn(Mono.just(true));

    RegisterIdentityCommand command =
        new RegisterIdentityCommand(tenantId, EMAIL, PASSWORD);

    StepVerifier.create(useCase.execute(command))
        .expectError(IdentityAlreadyExistsException.class)
        .verify();

    verify(identityRepository, never()).save(any());
    verify(passwordHasher, never()).hash(any());
  }

  @Test
  void shouldAllowSameEmailInDifferentTenant() {
    TenantId tenantA = TenantId.generate();
    TenantId tenantB = TenantId.generate();
    when(identityRepository.existsByTenantAndEmail(eq(tenantB), any(EmailAddress.class)))
        .thenReturn(Mono.just(false));
    when(passwordHasher.hash(PASSWORD)).thenReturn("$2a$10$hashed");
    when(identityRepository.save(any(Identity.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    RegisterIdentityCommand command =
        new RegisterIdentityCommand(tenantB, EMAIL, PASSWORD);

    StepVerifier.create(useCase.execute(command))
        .assertNext(result -> assertThat(result.tenantId()).isEqualTo(tenantB))
        .verifyComplete();

    verify(identityRepository).existsByTenantAndEmail(eq(tenantB), any(EmailAddress.class));
    verify(identityRepository, never()).existsByTenantAndEmail(eq(tenantA), any(EmailAddress.class));
  }

  @Test
  void shouldRejectBlankPassword() {
    TenantId tenantId = TenantId.generate();

    StepVerifier.create(
            useCase.execute(new RegisterIdentityCommand(tenantId, EMAIL, "   ")))
        .expectError(InvalidDomainValueException.class)
        .verify();

    verify(identityRepository, never()).existsByTenantAndEmail(any(), any());
  }

  @Test
  void shouldRejectBlankEmail() {
    TenantId tenantId = TenantId.generate();

    StepVerifier.create(
            useCase.execute(new RegisterIdentityCommand(tenantId, "  ", PASSWORD)))
        .expectError(InvalidDomainValueException.class)
        .verify();

    verify(identityRepository, never()).existsByTenantAndEmail(any(), any());
  }
}
