package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.in.AuthorizationService;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.port.out.RolePermissionRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.configuration.IamAuthorizationConfiguration;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.membership.MembershipRoleAssignment;
import com.codecore.iam.domain.model.permission.Permission;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.model.role.RolePermissionAssignment;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamAuthorizationPersistenceTestConfiguration;
import com.codecore.iam.testsupport.IamR2dbcTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({IamAuthorizationPersistenceTestConfiguration.class, IamR2dbcTestConfiguration.class, IamAuthorizationConfiguration.class})
class R2dbcAuthorizationQueryRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private MembershipRoleRepository membershipRoleRepository;

    @Autowired
    private AuthorizationService authorizationService;

    @Test
    void shouldGrantPermissionThroughMembershipRoleChain() {
        Instant now = Instant.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantId tenantId = TenantId.generate();
        IdentityTenantMembership membership = persistMembership(tenantId, now);
        Role role = persistRole(tenantId, "ADMIN_" + suffix, now);
        Permission permission = persistPermission("user:read_" + suffix, now);

        assignPermission(role, permission, now);
        assignRole(membership, role, now);

        var context = new com.codecore.iam.application.dto.AuthorizationContext(
                membership.identityId(),
                tenantId,
                membership.id()
        );

        StepVerifier.create(authorizationService.hasPermission(context, permission.code()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(authorizationService.hasRole(context, role.code()))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldDenyWhenPermissionNotAssigned() {
        Instant now = Instant.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantId tenantId = TenantId.generate();
        IdentityTenantMembership membership = persistMembership(tenantId, now);
        Role role = persistRole(tenantId, "STAFF_" + suffix, now);
        Permission permission = persistPermission("patient:create_" + suffix, now);
        assignRole(membership, role, now);

        var context = new com.codecore.iam.application.dto.AuthorizationContext(
                membership.identityId(),
                tenantId,
                membership.id()
        );

        StepVerifier.create(authorizationService.hasPermission(context, permission.code()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldGrantWhenAnyPermissionMatches() {
        Instant now = Instant.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantId tenantId = TenantId.generate();
        IdentityTenantMembership membership = persistMembership(tenantId, now);
        Role role = persistRole(tenantId, "EDITOR_" + suffix, now);
        Permission read = persistPermission("doc:read_" + suffix, now);
        Permission write = persistPermission("doc:write_" + suffix, now);

        assignPermission(role, read, now);
        assignRole(membership, role, now);

        var context = new com.codecore.iam.application.dto.AuthorizationContext(
                membership.identityId(),
                tenantId,
                membership.id()
        );

        StepVerifier.create(authorizationService.hasAnyPermission(context, write.code(), read.code()))
                .expectNext(true)
                .verifyComplete();
    }

    private IdentityTenantMembership persistMembership(TenantId tenantId, Instant now) {
        IdentityTenantMembership membership = IdentityTenantMembership.create(
                IdentityId.generate(),
                tenantId,
                now
        );
        StepVerifier.create(membershipRepository.save(membership))
                .assertNext(saved -> assertThat(saved.id()).isEqualTo(membership.id()))
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

    private Permission persistPermission(String code, Instant now) {
        Permission permission = Permission.create(PermissionCode.of(code), "test permission", now);
        StepVerifier.create(permissionRepository.save(permission))
                .expectNextCount(1)
                .verifyComplete();
        return permission;
    }

    private void assignPermission(Role role, Permission permission, Instant now) {
        StepVerifier.create(rolePermissionRepository.assign(
                role.id(),
                RolePermissionAssignment.assign(permission.id(), now)
        )).verifyComplete();
    }

    private void assignRole(IdentityTenantMembership membership, Role role, Instant now) {
        StepVerifier.create(membershipRoleRepository.assign(
                membership.id(),
                MembershipRoleAssignment.assign(role.id(), now)
        )).verifyComplete();
    }
}
