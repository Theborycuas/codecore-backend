package com.codecore.iam.domain.model.membership;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityTenantMembershipTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    void shouldCreateActiveMembership() {
        IdentityId identityId = IdentityId.generate();
        TenantId tenantId = TenantId.generate();

        IdentityTenantMembership membership = IdentityTenantMembership.create(identityId, tenantId, NOW);

        assertThat(membership.id()).isNotNull();
        assertThat(membership.identityId()).isEqualTo(identityId);
        assertThat(membership.tenantId()).isEqualTo(tenantId);
        assertThat(membership.status()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.createdAt()).isEqualTo(NOW);
        assertThat(membership.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldDeactivateAndActivate() {
        IdentityTenantMembership membership = IdentityTenantMembership.create(
                IdentityId.generate(),
                TenantId.generate(),
                NOW
        );

        membership.deactivate();
        assertThat(membership.status()).isEqualTo(MembershipStatus.INACTIVE);
        assertThat(membership.updatedAt()).isAfter(NOW);

        membership.activate();
        assertThat(membership.status()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void shouldAssignAndRevokeRolesForSameTenant() {
        TenantId tenantId = TenantId.generate();
        IdentityTenantMembership membership = IdentityTenantMembership.create(
                IdentityId.generate(),
                tenantId,
                NOW
        );
        RoleId adminRole = RoleId.generate();
        RoleId billingRole = RoleId.generate();

        membership.assignRole(adminRole, tenantId, NOW);
        membership.assignRole(billingRole, tenantId, NOW);

        assertThat(membership.hasRole(adminRole)).isTrue();
        assertThat(membership.hasRole(billingRole)).isTrue();
        assertThat(membership.assignedRoleIds()).containsExactlyInAnyOrder(adminRole, billingRole);

        membership.revokeRole(adminRole);
        assertThat(membership.hasRole(adminRole)).isFalse();
        assertThat(membership.hasRole(billingRole)).isTrue();
    }

    @Test
    void shouldRejectCrossTenantRoleAssignment() {
        IdentityTenantMembership membership = IdentityTenantMembership.create(
                IdentityId.generate(),
                TenantId.generate(),
                NOW
        );

        assertThatThrownBy(() -> membership.assignRole(RoleId.generate(), TenantId.generate(), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant");
    }

    @Test
    void shouldRejectDuplicateRoleAssignment() {
        TenantId tenantId = TenantId.generate();
        IdentityTenantMembership membership = IdentityTenantMembership.create(
                IdentityId.generate(),
                tenantId,
                NOW
        );
        RoleId roleId = RoleId.generate();
        membership.assignRole(roleId, tenantId, NOW);

        assertThatThrownBy(() -> membership.assignRole(roleId, tenantId, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
