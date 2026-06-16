package com.codecore.iam.application.authorization;

import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.port.out.RolePermissionRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.application.port.out.TenantSystemRolesProvisioner;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.model.role.RolePermissionAssignment;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.TenantId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Creates tenant-scoped system roles and links grants via persistence (system roles are immutable in domain).
 */
public class TenantSystemRolesProvisionerImpl implements TenantSystemRolesProvisioner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public TenantSystemRolesProvisionerImpl(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.roleRepository = Objects.requireNonNull(roleRepository, "roleRepository");
        this.permissionRepository = Objects.requireNonNull(permissionRepository, "permissionRepository");
        this.rolePermissionRepository = Objects.requireNonNull(
                rolePermissionRepository,
                "rolePermissionRepository"
        );
    }

    @Override
    public Mono<Void> provisionForTenant(TenantId tenantId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Instant now = Instant.now();
        return Flux.fromArray(SystemRoleTemplate.values())
                .concatMap(template -> provisionRoleIfAbsent(tenantId, template, now))
                .then();
    }

    private Mono<Void> provisionRoleIfAbsent(TenantId tenantId, SystemRoleTemplate template, Instant now) {
        return roleRepository.existsByTenantIdAndCode(tenantId, template.code())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.empty();
                    }
                    Role role = Role.createSystemRole(tenantId, template.code(), template.roleName(), now);
                    return roleRepository.save(role)
                            .flatMap(saved -> assignPermissions(saved, template.permissions(), now));
                });
    }

    private Mono<Void> assignPermissions(Role role, Iterable<PermissionCode> permissionCodes, Instant now) {
        return Flux.fromIterable(permissionCodes)
                .concatMap(permissionRepository::findByCode)
                .concatMap(permission -> rolePermissionRepository.assign(
                        role.id(),
                        RolePermissionAssignment.assign(permission.id(), now)
                ))
                .then();
    }
}
