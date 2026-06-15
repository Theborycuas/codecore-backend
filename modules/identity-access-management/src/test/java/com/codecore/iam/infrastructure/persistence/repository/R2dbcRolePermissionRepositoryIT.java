package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.port.out.RolePermissionRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.model.permission.Permission;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.model.role.RolePermissionAssignment;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamR2dbcTestConfiguration;
import com.codecore.iam.testsupport.IamRolePermissionPersistenceTestConfiguration;
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
@Import({IamRolePermissionPersistenceTestConfiguration.class, IamR2dbcTestConfiguration.class})
class R2dbcRolePermissionRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Test
    void shouldAssignFindAndRevokeRolePermission() {
        Instant now = Instant.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Role role = persistRole("ADMIN_" + suffix, now);
        Permission permission = persistPermission("perm_" + suffix + ":create", now);
        RolePermissionAssignment assignment = RolePermissionAssignment.assign(permission.id(), now);

        StepVerifier.create(rolePermissionRepository.assign(role.id(), assignment))
                .verifyComplete();

        StepVerifier.create(rolePermissionRepository.existsByRoleIdAndPermissionId(role.id(), permission.id()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(rolePermissionRepository.findByRoleId(role.id()))
                .assertNext(found -> {
                    assertThat(found.permissionId()).isEqualTo(permission.id());
                    assertThat(found.assignedAt()).isNotNull();
                })
                .verifyComplete();

        StepVerifier.create(rolePermissionRepository.revoke(role.id(), permission.id()))
                .verifyComplete();

        StepVerifier.create(rolePermissionRepository.existsByRoleIdAndPermissionId(role.id(), permission.id()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldEnforceUniqueRolePermissionPair() {
        Instant now = Instant.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Role role = persistRole("VET_" + suffix, now);
        Permission permission = persistPermission("patient_" + suffix + ":view", now);
        RolePermissionAssignment assignment = RolePermissionAssignment.assign(permission.id(), now);

        StepVerifier.create(rolePermissionRepository.assign(role.id(), assignment))
                .verifyComplete();

        StepVerifier.create(rolePermissionRepository.assign(role.id(), assignment))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    void shouldReplaceAllAssignmentsForRole() {
        Instant now = Instant.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Role role = persistRole("MANAGER_" + suffix, now);
        Permission create = persistPermission("user_" + suffix + ":create", now);
        Permission update = persistPermission("user_" + suffix + ":update", now);
        Permission delete = persistPermission("user_" + suffix + ":delete", now);

        StepVerifier.create(rolePermissionRepository.assign(
                role.id(), RolePermissionAssignment.assign(create.id(), now)))
                .verifyComplete();

        StepVerifier.create(rolePermissionRepository.replaceAll(
                role.id(),
                java.util.List.of(
                        RolePermissionAssignment.assign(update.id(), now),
                        RolePermissionAssignment.assign(delete.id(), now)
                )))
                .verifyComplete();

        StepVerifier.create(rolePermissionRepository.findByRoleId(role.id()).collectList())
                .assertNext(assignments -> {
                    assertThat(assignments).hasSize(2);
                    assertThat(assignments.stream().map(RolePermissionAssignment::permissionId))
                            .containsExactlyInAnyOrder(update.id(), delete.id());
                })
                .verifyComplete();
    }

    private Role persistRole(String code, Instant now) {
        Role role = Role.create(TenantId.generate(), RoleCode.of(code), RoleName.of(code), now);
        StepVerifier.create(roleRepository.save(role))
                .expectNextCount(1)
                .verifyComplete();
        return role;
    }

    private Permission persistPermission(String code, Instant now) {
        Permission permission = Permission.create(PermissionCode.of(code), code, now);
        StepVerifier.create(permissionRepository.save(permission))
                .expectNextCount(1)
                .verifyComplete();
        return permission;
    }
}
