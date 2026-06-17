package com.codecore.iam.application.admin;

import com.codecore.iam.application.command.CreateAdminMembershipCommand;
import com.codecore.iam.application.command.UpdateAdminMembershipCommand;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.domain.exception.MembershipAlreadyExistsException;
import com.codecore.iam.domain.exception.MembershipNotFoundException;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.model.identity.Identity;
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
class MembershipAdministrationUseCaseTest {

    @Mock
    private AuthorizationContextAccessor authorizationContextAccessor;

    @Mock
    private com.codecore.iam.application.port.out.MembershipAdminQueryRepository membershipAdminQueryRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private IdentityRegistrationOrchestrator registrationOrchestrator;

    @Mock
    private OwnershipPolicy ownershipPolicy;

    @Mock
    private TransactionalOperator transactionalOperator;

    private MembershipAdministrationUseCaseImpl useCase;

    private final TenantId tenantId = TenantId.generate();
    private final MembershipId membershipId = MembershipId.generate();
    private final IdentityId identityId = IdentityId.generate();
    private final AuthorizationContext context = new AuthorizationContext(
            IdentityId.generate(),
            tenantId,
            MembershipId.generate()
    );

    @BeforeEach
    void setUp() {
        lenient().when(authorizationContextAccessor.current()).thenReturn(Mono.just(context));
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(ownershipPolicy.assertCanModifyUser(any(), any())).thenReturn(Mono.empty());
        useCase = new MembershipAdministrationUseCaseImpl(
                authorizationContextAccessor,
                membershipAdminQueryRepository,
                membershipRepository,
                identityRepository,
                registrationOrchestrator,
                ownershipPolicy,
                transactionalOperator
        );
    }

    @Test
    void shouldDeactivateMembership() {
        IdentityTenantMembership membership = membership(MembershipStatus.ACTIVE);
        when(membershipRepository.findByIdAndTenantId(membershipId, tenantId)).thenReturn(Mono.just(membership));
        when(membershipRepository.save(any(IdentityTenantMembership.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.deactivate(membershipId))
                .verifyComplete();

        assertThat(membership.status()).isEqualTo(MembershipStatus.INACTIVE);
    }

    @Test
    void shouldReturnNotFoundWhenMembershipNotInTenant() {
        when(membershipRepository.findByIdAndTenantId(membershipId, tenantId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.deactivate(membershipId))
                .expectError(MembershipNotFoundException.class)
                .verify();

        verify(ownershipPolicy, never()).assertCanModifyUser(any(), any());
    }

    @Test
    void shouldRejectDuplicateMembershipOnLink() {
        when(membershipRepository.exists(identityId, tenantId)).thenReturn(Mono.just(true));
        when(identityRepository.findById(identityId)).thenReturn(Mono.just(identity()));

        CreateAdminMembershipCommand command = new CreateAdminMembershipCommand(identityId.value(), null, null);

        StepVerifier.create(useCase.execute(command))
                .expectError(MembershipAlreadyExistsException.class)
                .verify();
    }

    @Test
    void shouldActivateMembershipOnUpdate() {
        IdentityTenantMembership membership = membership(MembershipStatus.INACTIVE);
        when(membershipRepository.findByIdAndTenantId(membershipId, tenantId)).thenReturn(Mono.just(membership));
        when(membershipRepository.save(any(IdentityTenantMembership.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(identityRepository.findById(identityId)).thenReturn(Mono.just(identity()));

        UpdateAdminMembershipCommand command = new UpdateAdminMembershipCommand(
                membershipId,
                MembershipStatus.ACTIVE
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> assertThat(view.status()).isEqualTo(MembershipStatus.ACTIVE))
                .verifyComplete();
    }

    private IdentityTenantMembership membership(MembershipStatus status) {
        Instant now = Instant.now();
        return IdentityTenantMembership.reconstitute(
                membershipId,
                identityId,
                tenantId,
                status,
                now,
                now,
                java.util.Set.of()
        );
    }

    private Identity identity() {
        Instant now = Instant.now();
        return new Identity(
                identityId,
                tenantId,
                EmailAddress.of("member@codecore.local"),
                IdentityStatus.ACTIVE,
                null,
                null,
                now,
                now,
                0L
        );
    }
}
