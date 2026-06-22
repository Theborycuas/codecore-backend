package com.codecore.iam.domain.model.permission;

import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.valueobject.PermissionCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    void shouldCreateGlobalPermission() {
        PermissionCode code = PermissionCode.of("User:Create");
        Permission permission = Permission.create(code, "Create users", NOW);

        assertThat(permission.code()).isEqualTo(PermissionCode.of("user:create"));
        assertThat(permission.code().resource()).isEqualTo("user");
        assertThat(permission.code().action()).isEqualTo("create");
        assertThat(permission.description()).isEqualTo("Create users");
        assertThat(permission.systemPermission()).isFalse();
        assertThat(permission.createdAt()).isEqualTo(NOW);
    }

    @Test
    void shouldUpdateDescriptionForCustomPermission() {
        Permission permission = Permission.create(
                PermissionCode.of("patient:view"),
                "View patients",
                NOW
        );

        permission.updateDescription("View patient records");
        assertThat(permission.description()).isEqualTo("View patient records");
        assertThat(permission.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldRejectMutationOfSystemPermission() {
        Permission permission = Permission.createSystemPermission(
                PermissionCode.of("user:delete"),
                "Delete users",
                NOW
        );

        assertThat(permission.systemPermission()).isTrue();
        assertThatThrownBy(() -> permission.updateDescription("Other"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAcceptHyphenatedResourceCodes() {
        assertThat(PermissionCode.of("staff-assignment:read").value())
                .isEqualTo("staff-assignment:read");
    }

    @Test
    void shouldRejectInvalidPermissionCode() {
        assertThatThrownBy(() -> PermissionCode.of("USER_CREATE"))
                .isInstanceOf(InvalidDomainValueException.class);
        assertThatThrownBy(() -> PermissionCode.of("user:"))
                .isInstanceOf(InvalidDomainValueException.class);
        assertThatThrownBy(() -> PermissionCode.of(":create"))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void shouldAllowNullOrBlankDescription() {
        Permission permission = Permission.create(PermissionCode.of("appointment:update"), null, NOW);
        assertThat(permission.description()).isNull();

        permission.updateDescription("   ");
        assertThat(permission.description()).isNull();
    }
}
