package com.codecore.iam.application.admin;

import com.codecore.iam.application.command.ReplaceAdminMembershipRolesCommand;
import com.codecore.iam.application.dto.AdminMembershipRoleView;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleAdminQueryRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.membership.MembershipRoleAssignment;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipRoleAdministrationUseCaseTest {

    @Mock
    private AuthorizationContextAccessor authorizationContextAccessor;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MembershipRoleRepository membershipRoleRepository;

    @Mock
    private MembershipRoleAdminQueryRepository membershipRoleAdminQueryRepository;

    @Mock
    private OwnershipPolicy ownershipPolicy;

    @Mock
    private TransactionalOperator transactionalOperator;

    private MembershipRoleAdministrationUseCaseImpl useCase;

    private final TenantId tenantId = TenantId.generate();
    private final MembershipId membershipId = MembershipId.generate();
    private final IdentityId identityId = IdentityId.generate();
    private final RoleId userRoleId = RoleId.generate();
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
        useCase = new MembershipRoleAdministrationUseCaseImpl(
                authorizationContextAccessor,
                membershipRepository,
                roleRepository,
                membershipRoleRepository,
                membershipRoleAdminQueryRepository,
                ownershipPolicy,
                transactionalOperator
        );
    }

    @Test
    void shouldListMembershipRoles() {
        IdentityTenantMembership membership = activeMembership();
        when(membershipRepository.findByIdAndTenantId(membershipId, tenantId)).thenReturn(Mono.just(membership));
        when(membershipRoleAdminQueryRepository.findByMembershipId(membershipId, tenantId))
                .thenReturn(Flux.just(new AdminMembershipRoleView(
                        userRoleId,
                        "USER",
                        "User",
                        "ACTIVE",
                        true,
                        Instant.now()
                )));

        StepVerifier.create(useCase.execute(membershipId))
                .assertNext(views -> assertThat(views).hasSize(1))
                .verifyComplete();
    }

    @Test
    void shouldReplaceMembershipRoles() {
        IdentityTenantMembership membership = activeMembership();
        Role userRole = userRole();

        when(membershipRepository.findByIdAndTenantId(membershipId, tenantId)).thenReturn(Mono.just(membership));
        when(roleRepository.findById(userRoleId)).thenReturn(Mono.just(userRole));
        when(membershipRoleRepository.findByMembershipId(membershipId)).thenReturn(Flux.empty());
        when(membershipRoleRepository.replaceAll(eq(membershipId), any())).thenReturn(Mono.empty());
        when(membershipRoleAdminQueryRepository.findByMembershipId(membershipId, tenantId))
                .thenReturn(Flux.just(new AdminMembershipRoleView(
                        userRoleId,
                        "USER",
                        "User",
                        "ACTIVE",
                        true,
                        Instant.now()
                )));

        ReplaceAdminMembershipRolesCommand command = new ReplaceAdminMembershipRolesCommand(
                membershipId,
                List.of(userRoleId.value())
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(views -> assertThat(views).hasSize(1))
                .verifyComplete();

        verify(ownershipPolicy).assertCanModifyUser(context, identityId);
        verify(membershipRoleRepository).replaceAll(eq(membershipId), any());
    }

    @Test
    void shouldRejectReplaceWhenMembershipInactive() {
        IdentityTenantMembership membership = inactiveMembership();
        when(membershipRepository.findByIdAndTenantId(membershipId, tenantId)).thenReturn(Mono.just(membership));

        ReplaceAdminMembershipRolesCommand command = new ReplaceAdminMembershipRolesCommand(
                membershipId,
                List.of(userRoleId.value())
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(InvalidDomainValueException.class)
                .verify();

        verify(ownershipPolicy, never()).assertCanModifyUser(any(), any());
    }

    private IdentityTenantMembership activeMembership() {
        Instant now = Instant.now();
        return IdentityTenantMembership.reconstitute(
                membershipId,
                identityId,
                tenantId,
                MembershipStatus.ACTIVE,
                now,
                now,
                Set.of()
        );
    }

    private IdentityTenantMembership inactiveMembership() {
        Instant now = Instant.now();
        return IdentityTenantMembership.reconstitute(
                membershipId,
                identityId,
                tenantId,
                MembershipStatus.INACTIVE,
                now,
                now,
                Set.of()
        );
    }

    private Role userRole() {
        Instant now = Instant.now();
        return Role.reconstitute(
                userRoleId,
                tenantId,
                RoleCode.of("USER"),
                RoleName.of("User"),
                RoleStatus.ACTIVE,
                true,
                now,
                now,
                Set.of()
        );
    }
}
