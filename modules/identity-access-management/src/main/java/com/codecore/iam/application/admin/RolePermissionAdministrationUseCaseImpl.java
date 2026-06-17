package com.codecore.iam.application.admin;

import com.codecore.iam.application.command.ReplaceAdminRolePermissionsCommand;
import com.codecore.iam.application.dto.AdminRolePermissionView;
import com.codecore.iam.application.port.in.GetAdminRolePermissionsUseCase;
import com.codecore.iam.application.port.in.ReplaceAdminRolePermissionsUseCase;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.port.out.RolePermissionAdminQueryRepository;
import com.codecore.iam.application.port.out.RolePermissionRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.exception.PermissionNotFoundException;
import com.codecore.iam.domain.exception.RoleNotFoundException;
import com.codecore.iam.domain.exception.SystemRoleImmutableException;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.model.role.RolePermissionAssignment;
import com.codecore.iam.domain.valueobject.PermissionId;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.TenantId;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Role ↔ Permission administration (FASE 15.5).
 */
public final class RolePermissionAdministrationUseCaseImpl
        implements GetAdminRolePermissionsUseCase,
        ReplaceAdminRolePermissionsUseCase {

    private final AuthorizationContextAccessor authorizationContextAccessor;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionAdminQueryRepository rolePermissionAdminQueryRepository;
    private final TransactionalOperator transactionalOperator;

    public RolePermissionAdministrationUseCaseImpl(
            AuthorizationContextAccessor authorizationContextAccessor,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            RolePermissionAdminQueryRepository rolePermissionAdminQueryRepository,
            TransactionalOperator transactionalOperator
    ) {
        this.authorizationContextAccessor = Objects.requireNonNull(
                authorizationContextAccessor,
                "authorizationContextAccessor"
        );
        this.roleRepository = Objects.requireNonNull(roleRepository, "roleRepository");
        this.permissionRepository = Objects.requireNonNull(permissionRepository, "permissionRepository");
        this.rolePermissionRepository = Objects.requireNonNull(
                rolePermissionRepository,
                "rolePermissionRepository"
        );
        this.rolePermissionAdminQueryRepository = Objects.requireNonNull(
                rolePermissionAdminQueryRepository,
                "rolePermissionAdminQueryRepository"
        );
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<List<AdminRolePermissionView>> execute(RoleId roleId) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadRoleInTenant(ctx.tenantId(), roleId)
                        .flatMap(role -> rolePermissionAdminQueryRepository
                                .findByRoleId(role.id())
                                .collectList()));
    }

    @Override
    public Mono<List<AdminRolePermissionView>> execute(ReplaceAdminRolePermissionsCommand command) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadRoleInTenant(ctx.tenantId(), command.roleId())
                        .flatMap(role -> assertCustomRole(role)
                                .then(Mono.defer(() -> replacePermissions(role, command.permissionIds())))))
                .as(transactionalOperator::transactional);
    }

    private Mono<List<AdminRolePermissionView>> replacePermissions(Role role, List<java.util.UUID> permissionIdValues) {
        List<PermissionId> desiredIds = normalizePermissionIds(permissionIdValues);
        Instant now = Instant.now();

        return validatePermissionsExist(desiredIds)
                .then(rolePermissionRepository.findByRoleId(role.id()).collectList())
                .flatMap(currentAssignments -> {
                    Role mutableRole = reconstituteWithAssignments(role, currentAssignments);
                    Set<PermissionId> currentIds = mutableRole.assignedPermissionIds();
                    Set<PermissionId> desiredSet = new LinkedHashSet<>(desiredIds);

                    for (PermissionId permissionId : desiredSet) {
                        if (!currentIds.contains(permissionId)) {
                            mutableRole.assignPermission(permissionId, now);
                        }
                    }
                    for (PermissionId permissionId : currentIds) {
                        if (!desiredSet.contains(permissionId)) {
                            mutableRole.revokePermission(permissionId);
                        }
                    }

                    return rolePermissionRepository
                            .replaceAll(role.id(), mutableRole.permissionAssignments())
                            .then(rolePermissionAdminQueryRepository.findByRoleId(role.id()).collectList());
                });
    }

    private Mono<Void> validatePermissionsExist(List<PermissionId> permissionIds) {
        return Mono.defer(() -> {
            Mono<Void> chain = Mono.empty();
            for (PermissionId permissionId : permissionIds) {
                chain = chain.then(
                        Mono.defer(() -> permissionRepository.findById(permissionId)
                                .switchIfEmpty(Mono.error(new PermissionNotFoundException(
                                        "Permission not found: " + permissionId.value())))
                                .then())
                );
            }
            return chain;
        });
    }

    private static List<PermissionId> normalizePermissionIds(List<java.util.UUID> permissionIdValues) {
        if (permissionIdValues == null) {
            throw new InvalidDomainValueException("permissionIds is required");
        }
        Set<java.util.UUID> unique = new HashSet<>();
        List<PermissionId> normalized = new ArrayList<>();
        for (java.util.UUID value : permissionIdValues) {
            if (value == null) {
                throw new InvalidDomainValueException("permissionIds must not contain null");
            }
            if (!unique.add(value)) {
                throw new InvalidDomainValueException("permissionIds must be unique");
            }
            normalized.add(new PermissionId(value));
        }
        return normalized;
    }

    private static Role reconstituteWithAssignments(Role role, List<RolePermissionAssignment> assignments) {
        return Role.reconstitute(
                role.id(),
                role.tenantId(),
                role.code(),
                role.name(),
                role.status(),
                role.systemRole(),
                role.createdAt(),
                role.updatedAt(),
                new LinkedHashSet<>(assignments)
        );
    }

    private Mono<Role> loadRoleInTenant(TenantId tenantId, RoleId roleId) {
        return roleRepository.findById(roleId)
                .filter(role -> role.tenantId().equals(tenantId))
                .switchIfEmpty(Mono.error(new RoleNotFoundException("Role not found in tenant context")));
    }

    private Mono<Void> assertCustomRole(Role role) {
        if (role.systemRole()) {
            return Mono.error(new SystemRoleImmutableException("System roles cannot be modified"));
        }
        return Mono.empty();
    }
}
