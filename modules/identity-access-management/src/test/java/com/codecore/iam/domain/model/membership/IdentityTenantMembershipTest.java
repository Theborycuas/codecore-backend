package com.codecore.iam.domain.model.membership;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

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
}
