package com.codecore.iam.application;

import com.codecore.iam.application.command.RegisterIdentityCommand;
import com.codecore.iam.application.dto.RegisterIdentityResult;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.domain.exception.IdentityAlreadyExistsException;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.valueobject.MembershipStatus;
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
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterIdentityUseCaseTest {

  private static final String EMAIL = "user@codecore.local";
  private static final String PASSWORD = "ValidPass1!";

  @Mock private IdentityRepository identityRepository;

  @Mock private MembershipRepository membershipRepository;

  @Mock private PasswordHasher passwordHasher;

  @Mock private TransactionalOperator transactionalOperator;

  private RegisterIdentityUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    lenient().when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    useCase = new RegisterIdentityUseCaseImpl(
        identityRepository, membershipRepository, passwordHasher, transactionalOperator);
  }

  @Test
  void shouldRegisterIdentitySuccessfully() {
    TenantId tenantId = TenantId.generate();
    when(identityRepository.existsByEmail(any(EmailAddress.class)))
        .thenReturn(Mono.just(false));
    when(passwordHasher.hash(PASSWORD)).thenReturn("$2a$10$hashed");
    when(identityRepository.save(any(Identity.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(membershipRepository.save(any(IdentityTenantMembership.class)))
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

    ArgumentCaptor<IdentityTenantMembership> membershipCaptor =
        ArgumentCaptor.forClass(IdentityTenantMembership.class);
    verify(membershipRepository).save(membershipCaptor.capture());
    assertThat(membershipCaptor.getValue().identityId()).isEqualTo(saved.getValue().id());
    assertThat(membershipCaptor.getValue().tenantId()).isEqualTo(tenantId);
    assertThat(membershipCaptor.getValue().status()).isEqualTo(MembershipStatus.ACTIVE);
  }

  @Test
  void shouldRejectDuplicateEmailInSameTenant() {
    TenantId tenantId = TenantId.generate();
    when(identityRepository.existsByEmail(any(EmailAddress.class)))
        .thenReturn(Mono.just(true));

    RegisterIdentityCommand command =
        new RegisterIdentityCommand(tenantId, EMAIL, PASSWORD);

    StepVerifier.create(useCase.execute(command))
        .expectError(IdentityAlreadyExistsException.class)
        .verify();

    verify(identityRepository, never()).save(any());
    verify(membershipRepository, never()).save(any());
    verify(passwordHasher, never()).hash(any());
  }

  @Test
  void shouldRejectDuplicateEmailInDifferentTenant() {
    TenantId tenantB = TenantId.generate();
    when(identityRepository.existsByEmail(any(EmailAddress.class)))
        .thenReturn(Mono.just(true));

    RegisterIdentityCommand command =
        new RegisterIdentityCommand(tenantB, EMAIL, PASSWORD);

    StepVerifier.create(useCase.execute(command))
        .expectError(IdentityAlreadyExistsException.class)
        .verify();

    verify(identityRepository).existsByEmail(any(EmailAddress.class));
    verify(identityRepository, never()).save(any());
    verify(membershipRepository, never()).save(any());
    verify(passwordHasher, never()).hash(any());
  }

  @Test
  void shouldRejectBlankPassword() {
    TenantId tenantId = TenantId.generate();

    StepVerifier.create(
            useCase.execute(new RegisterIdentityCommand(tenantId, EMAIL, "   ")))
        .expectError(InvalidDomainValueException.class)
        .verify();

    verify(identityRepository, never()).existsByEmail(any());
  }

  @Test
  void shouldRejectBlankEmail() {
    TenantId tenantId = TenantId.generate();

    StepVerifier.create(
            useCase.execute(new RegisterIdentityCommand(tenantId, "  ", PASSWORD)))
        .expectError(InvalidDomainValueException.class)
        .verify();

    verify(identityRepository, never()).existsByEmail(any());
  }
}
