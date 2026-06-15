package com.codecore.iam.domain.model.role;

import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    void shouldCreateActiveTenantRole() {
        TenantId tenantId = TenantId.generate();
        RoleCode code = RoleCode.of("admin");
        RoleName name = RoleName.of("Administrator");

        Role role = Role.create(tenantId, code, name, NOW);

        assertThat(role.tenantId()).isEqualTo(tenantId);
        assertThat(role.code()).isEqualTo(RoleCode.of("ADMIN"));
        assertThat(role.name()).isEqualTo(name);
        assertThat(role.status()).isEqualTo(RoleStatus.ACTIVE);
        assertThat(role.systemRole()).isFalse();
        assertThat(role.createdAt()).isEqualTo(NOW);
        assertThat(role.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRenameDeactivateAndActivateCustomRole() {
        Role role = Role.create(
                TenantId.generate(),
                RoleCode.of("MANAGER"),
                RoleName.of("Manager"),
                NOW
        );

        RoleName updatedName = RoleName.of("Operations Manager");
        role.rename(updatedName);
        assertThat(role.name()).isEqualTo(updatedName);

        role.deactivate();
        assertThat(role.status()).isEqualTo(RoleStatus.INACTIVE);

        role.activate();
        assertThat(role.status()).isEqualTo(RoleStatus.ACTIVE);
        assertThat(role.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldRejectMutationOfSystemRole() {
        Role role = Role.createSystemRole(
                TenantId.generate(),
                RoleCode.of("ADMIN"),
                RoleName.of("Platform Admin"),
                NOW
        );

        assertThat(role.systemRole()).isTrue();

        assertThatThrownBy(() -> role.rename(RoleName.of("Other")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(role::deactivate)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectInvalidRoleCode() {
        assertThatThrownBy(() -> RoleCode.of("bad-code"))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void shouldRejectBlankRoleName() {
        assertThatThrownBy(() -> RoleName.of("   "))
                .isInstanceOf(InvalidDomainValueException.class);
    }
}
