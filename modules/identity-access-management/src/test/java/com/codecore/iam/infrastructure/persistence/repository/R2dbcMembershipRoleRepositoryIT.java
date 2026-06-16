package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.membership.MembershipRoleAssignment;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamMembershipRolePersistenceTestConfiguration;
import com.codecore.iam.testsupport.IamR2dbcTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({IamMembershipRolePersistenceTestConfiguration.class, IamR2dbcTestConfiguration.class})
class R2dbcMembershipRoleRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MembershipRoleRepository membershipRoleRepository;

    @Test
    void shouldAssignFindAndRevokeMembershipRole() {
        Instant now = Instant.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantId tenantId = TenantId.generate();
        IdentityTenantMembership membership = persistMembership(tenantId, suffix, now);
        Role role = persistRole(tenantId, "ADMIN_" + suffix, now);
        MembershipRoleAssignment assignment = MembershipRoleAssignment.assign(role.id(), now);

        StepVerifier.create(membershipRoleRepository.assign(membership.id(), assignment))
                .verifyComplete();

        StepVerifier.create(membershipRoleRepository.existsByMembershipIdAndRoleId(membership.id(), role.id()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(membershipRoleRepository.findByMembershipId(membership.id()))
                .assertNext(found -> {
                    assertThat(found.roleId()).isEqualTo(role.id());
                    assertThat(found.assignedAt()).isNotNull();
                })
                .verifyComplete();

        StepVerifier.create(membershipRoleRepository.revoke(membership.id(), role.id()))
                .verifyComplete();

        StepVerifier.create(membershipRoleRepository.existsByMembershipIdAndRoleId(membership.id(), role.id()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldAllowMultipleRolesPerMembership() {
        Instant now = Instant.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantId tenantId = TenantId.generate();
        IdentityTenantMembership membership = persistMembership(tenantId, suffix, now);
        Role admin = persistRole(tenantId, "ADMIN_" + suffix, now);
        Role billing = persistRole(tenantId, "BILLING_" + suffix, now);

        StepVerifier.create(membershipRoleRepository.assign(
                membership.id(), MembershipRoleAssignment.assign(admin.id(), now)))
                .verifyComplete();
        StepVerifier.create(membershipRoleRepository.assign(
                membership.id(), MembershipRoleAssignment.assign(billing.id(), now)))
                .verifyComplete();

        StepVerifier.create(membershipRoleRepository.findByMembershipId(membership.id()).collectList())
                .assertNext(assignments -> {
                    assertThat(assignments).hasSize(2);
                    assertThat(assignments.stream().map(MembershipRoleAssignment::roleId))
                            .containsExactlyInAnyOrder(admin.id(), billing.id());
                })
                .verifyComplete();
    }

    @Test
    void shouldEnforceUniqueMembershipRolePair() {
        Instant now = Instant.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantId tenantId = TenantId.generate();
        IdentityTenantMembership membership = persistMembership(tenantId, suffix, now);
        Role role = persistRole(tenantId, "VET_" + suffix, now);
        MembershipRoleAssignment assignment = MembershipRoleAssignment.assign(role.id(), now);

        StepVerifier.create(membershipRoleRepository.assign(membership.id(), assignment))
                .verifyComplete();

        StepVerifier.create(membershipRoleRepository.assign(membership.id(), assignment))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    private IdentityTenantMembership persistMembership(TenantId tenantId, String suffix, Instant now) {
        IdentityTenantMembership membership = IdentityTenantMembership.create(
                IdentityId.generate(),
                tenantId,
                now
        );
        StepVerifier.create(membershipRepository.save(membership))
                .expectNextCount(1)
                .verifyComplete();
        return membership;
    }

    private Role persistRole(TenantId tenantId, String code, Instant now) {
        Role role = Role.create(tenantId, RoleCode.of(code), RoleName.of(code), now);
        StepVerifier.create(roleRepository.save(role))
                .expectNextCount(1)
                .verifyComplete();
        return role;
    }
}
