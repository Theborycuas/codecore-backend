package com.codecore.iam.application.admin;

import com.codecore.iam.application.command.UpdateAdminUserCommand;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.IdentityAdminQueryRepository;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.domain.exception.IdentityNotFoundException;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdministrationUseCaseTest {

    @Mock
    private AuthorizationContextAccessor authorizationContextAccessor;

    @Mock
    private IdentityAdminQueryRepository identityAdminQueryRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private IdentityRegistrationOrchestrator registrationOrchestrator;

    @Mock
    private OwnershipPolicy ownershipPolicy;

    @Mock
    private TransactionalOperator transactionalOperator;

    private UserAdministrationUseCaseImpl useCase;

    private final TenantId tenantId = TenantId.generate();
    private final IdentityId identityId = IdentityId.generate();
    private final AuthorizationContext context = new AuthorizationContext(
            IdentityId.generate(),
            tenantId,
            com.codecore.iam.domain.valueobject.MembershipId.generate()
    );

    @BeforeEach
    void setUp() {
        lenient().when(authorizationContextAccessor.current()).thenReturn(Mono.just(context));
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(ownershipPolicy.assertCanModifyUser(any(), any())).thenReturn(Mono.empty());
        useCase = new UserAdministrationUseCaseImpl(
                authorizationContextAccessor,
                identityAdminQueryRepository,
                identityRepository,
                membershipRepository,
                registrationOrchestrator,
                ownershipPolicy,
                transactionalOperator
        );
    }

    @Test
    void shouldDeactivateMembershipForUserInTenant() {
        Identity identity = activeIdentity();
        IdentityTenantMembership membership = IdentityTenantMembership.create(
                identityId,
                tenantId,
                Instant.now()
        );
        when(membershipRepository.exists(identityId, tenantId)).thenReturn(Mono.just(true));
        when(identityRepository.findById(identityId)).thenReturn(Mono.just(identity));
        when(membershipRepository.findByIdentityIdAndTenantId(identityId, tenantId))
                .thenReturn(Mono.just(membership));
        when(membershipRepository.save(any(IdentityTenantMembership.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.deactivate(identityId))
                .verifyComplete();

        verify(ownershipPolicy).assertCanModifyUser(context, identityId);
        assertThat(identity.status()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(membership.status()).isEqualTo(MembershipStatus.INACTIVE);
        verify(identityRepository, never()).save(any());
    }

    @Test
    void shouldReturnNotFoundWhenUserNotInTenant() {
        when(membershipRepository.exists(identityId, tenantId)).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.deactivate(identityId))
                .expectError(IdentityNotFoundException.class)
                .verify();

        verify(ownershipPolicy, never()).assertCanModifyUser(any(), any());
    }

    @Test
    void shouldUpdateStatusWhenPermitted() {
        Identity identity = activeIdentity();
        when(membershipRepository.exists(identityId, tenantId)).thenReturn(Mono.just(true));
        when(identityRepository.findById(identityId)).thenReturn(Mono.just(identity));
        when(identityRepository.save(any(Identity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        UpdateAdminUserCommand command = new UpdateAdminUserCommand(
                identityId,
                IdentityStatus.LOCKED,
                null
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> assertThat(view.status()).isEqualTo(IdentityStatus.LOCKED))
                .verifyComplete();
    }

    private Identity activeIdentity() {
        Instant now = Instant.now();
        return new Identity(
                identityId,
                tenantId,
                EmailAddress.of("user@codecore.local"),
                IdentityStatus.ACTIVE,
                null,
                null,
                now,
                now,
                0L
        );
    }
}
