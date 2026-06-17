package com.codecore.iam.application.admin;

import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.exception.OwnershipDeniedException;
import com.codecore.iam.domain.model.membership.MembershipRoleAssignment;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnershipPolicyTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private MembershipRoleRepository membershipRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    private OwnershipPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new OwnershipPolicy(membershipRepository, membershipRoleRepository, roleRepository);
    }

    @Test
    void shouldAllowOwnerToModifyAdmin() {
        TenantId tenantId = TenantId.generate();
        MembershipId actorMembership = MembershipId.generate();
        IdentityId targetId = IdentityId.generate();
        MembershipId targetMembership = MembershipId.generate();

        stubActorRole(actorMembership, tenantId, "OWNER");
        stubTargetRole(targetId, targetMembership, tenantId, "ADMIN");

        AuthorizationContext actor = new AuthorizationContext(IdentityId.generate(), tenantId, actorMembership);

        StepVerifier.create(policy.assertCanModifyUser(actor, targetId))
                .verifyComplete();
    }

    @Test
    void shouldDenyAdminModifyingOwner() {
        TenantId tenantId = TenantId.generate();
        MembershipId actorMembership = MembershipId.generate();
        IdentityId targetId = IdentityId.generate();
        MembershipId targetMembership = MembershipId.generate();

        stubActorRole(actorMembership, tenantId, "ADMIN");
        stubTargetRole(targetId, targetMembership, tenantId, "OWNER");

        AuthorizationContext actor = new AuthorizationContext(IdentityId.generate(), tenantId, actorMembership);

        StepVerifier.create(policy.assertCanModifyUser(actor, targetId))
                .expectError(OwnershipDeniedException.class)
                .verify();
    }

    @Test
    void shouldDenyManagerModifyingAdmin() {
        TenantId tenantId = TenantId.generate();
        MembershipId actorMembership = MembershipId.generate();
        IdentityId targetId = IdentityId.generate();
        MembershipId targetMembership = MembershipId.generate();

        stubActorRole(actorMembership, tenantId, "MANAGER");
        stubTargetRole(targetId, targetMembership, tenantId, "ADMIN");

        AuthorizationContext actor = new AuthorizationContext(IdentityId.generate(), tenantId, actorMembership);

        StepVerifier.create(policy.assertCanModifyUser(actor, targetId))
                .expectError(OwnershipDeniedException.class)
                .verify();
    }

    @Test
    void shouldAllowManagerToModifyUserWithoutRoles() {
        TenantId tenantId = TenantId.generate();
        MembershipId actorMembership = MembershipId.generate();
        IdentityId targetId = IdentityId.generate();
        MembershipId targetMembership = MembershipId.generate();

        stubActorRole(actorMembership, tenantId, "MANAGER");
        when(membershipRepository.findByIdentityIdAndTenantId(targetId, tenantId))
                .thenReturn(Mono.just(membership(targetMembership, targetId, tenantId)));
        when(membershipRoleRepository.findByMembershipId(targetMembership)).thenReturn(Flux.empty());

        AuthorizationContext actor = new AuthorizationContext(IdentityId.generate(), tenantId, actorMembership);

        StepVerifier.create(policy.assertCanModifyUser(actor, targetId))
                .verifyComplete();
    }

    private void stubActorRole(MembershipId membershipId, TenantId tenantId, String code) {
        RoleId roleId = RoleId.generate();
        when(membershipRoleRepository.findByMembershipId(membershipId))
                .thenReturn(Flux.just(MembershipRoleAssignment.assign(roleId, Instant.now())));
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(role(roleId, tenantId, code)));
    }

    private void stubTargetRole(
            IdentityId targetId,
            MembershipId targetMembership,
            TenantId tenantId,
            String code
    ) {
        when(membershipRepository.findByIdentityIdAndTenantId(targetId, tenantId))
                .thenReturn(Mono.just(membership(targetMembership, targetId, tenantId)));
        RoleId roleId = RoleId.generate();
        when(membershipRoleRepository.findByMembershipId(targetMembership))
                .thenReturn(Flux.just(MembershipRoleAssignment.assign(roleId, Instant.now())));
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(role(roleId, tenantId, code)));
    }

    private static IdentityTenantMembership membership(
            MembershipId membershipId,
            IdentityId identityId,
            TenantId tenantId
    ) {
        Instant now = Instant.now();
        return IdentityTenantMembership.reconstitute(
                membershipId,
                identityId,
                tenantId,
                com.codecore.iam.domain.valueobject.MembershipStatus.ACTIVE,
                now,
                now,
                Set.of()
        );
    }

    private static Role role(RoleId roleId, TenantId tenantId, String code) {
        Instant now = Instant.now();
        return Role.reconstitute(
                roleId,
                tenantId,
                RoleCode.of(code),
                RoleName.of(code),
                RoleStatus.ACTIVE,
                true,
                now,
                now,
                Set.of()
        );
    }
}
